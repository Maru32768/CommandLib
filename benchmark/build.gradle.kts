// JMH benchmarks of common through the common-testing CommandTester. Not published and not run by check; see
// docs/benchmarks.md.
repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net")
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    implementation(project(":common-testing"))
    implementation("com.mojang:brigadier:1.0.18")
    implementation("org.openjdk.jmh:jmh-core:1.37")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

val jmhResults = layout.buildDirectory.file("results/jmh/results.json")

tasks.register<JavaExec>("jmh") {
    group = "benchmark"
    description = "Runs the JMH benchmarks. --args replaces the default JMH options."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.openjdk.jmh.Main")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
    val resultsFile = jmhResults.get().asFile
    args("-f", "1", "-wi", "3", "-w", "1s", "-i", "5", "-r", "1s", "-prof", "gc", "-rf", "json", "-rff", resultsFile)
    doFirst { resultsFile.parentFile.mkdirs() }
    outputs.upToDateWhen { false }
}

tasks.register("jmhBaseline") {
    group = "benchmark"
    description = "Writes the metrics jmhCompare reads from build/results/jmh/results.json to baseline.json."
    val resultsFile = jmhResults.get().asFile
    val baselineFile = file("baseline.json")
    doLast {
        @Suppress("UNCHECKED_CAST")
        val results = groovy.json.JsonSlurper().parse(resultsFile) as List<Map<String, Any?>>
        // Only what jmhCompare reads, and the JDK it was recorded with.
        val baseline = results.map { result ->
            val primary = result["primaryMetric"] as Map<*, *>
            val percentiles = primary["scorePercentiles"] as Map<*, *>?
            val alloc = (result["secondaryMetrics"] as Map<*, *>)["gc.alloc.rate.norm"] as Map<*, *>?
            mapOf(
                "benchmark" to result["benchmark"],
                "mode" to result["mode"],
                "params" to result["params"],
                "jdkVersion" to result["jdkVersion"],
                "vmName" to result["vmName"],
                "primaryMetric" to mapOf(
                    "score" to primary["score"],
                    "scoreError" to primary["scoreError"],
                    "scorePercentiles" to percentiles?.filterKeys { it == "50.0" || it == "95.0" },
                    "scoreUnit" to primary["scoreUnit"],
                ).filterValues { it != null },
                "secondaryMetrics" to mapOf("gc.alloc.rate.norm" to alloc?.filterKeys { it == "score" || it == "scoreUnit" }),
            ).filterValues { it != null }
        }
        baselineFile.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(baseline)) + "\n")
    }
}

// The regression tolerances of docs/benchmarks.md: the allowed relative change in the worse direction. Only the
// allocation fails the task: the baseline's time errors are as large as the scores, so the time metrics are reported
// until it is recorded again on an idle machine.
val tolerances = mapOf(
    "p50" to 0.10,
    "p95" to 0.20,
    "thrpt" to 0.10,
    "avgt" to 0.15,
    "alloc" to 0.05,
)

tasks.register("jmhCompare") {
    group = "benchmark"
    description = "Fails when build/results/jmh/results.json regressed beyond the tolerances against baseline.json."
    val baselineFile = file(providers.gradleProperty("benchmark.baseline").getOrElse("baseline.json"))
    val resultsFile = jmhResults.get().asFile
    doLast {
        @Suppress("UNCHECKED_CAST")
        fun read(file: File) = (groovy.json.JsonSlurper().parse(file) as List<Map<String, Any?>>).associateBy {
            "${it["benchmark"]} ${it["mode"]} ${it["params"] ?: ""}"
        }

        @Suppress("UNCHECKED_CAST")
        fun primary(m: Map<String, Any?>) = m["primaryMetric"] as Map<String, Any?>
        fun score(m: Map<String, Any?>) = (primary(m)["score"] as Number?)?.toDouble()

        @Suppress("UNCHECKED_CAST")
        fun percentile(m: Map<String, Any?>, p: String) =
            ((primary(m)["scorePercentiles"] as Map<String, Any?>?)?.get(p) as Number?)?.toDouble()

        @Suppress("UNCHECKED_CAST")
        fun alloc(m: Map<String, Any?>) = ((m["secondaryMetrics"] as Map<String, Map<String, Any?>>?)
            ?.get("gc.alloc.rate.norm")?.get("score") as Number?)?.toDouble()

        val baseline = read(baselineFile)
        val regressions = mutableListOf<String>()
        fun check(key: String, metric: String, base: Double?, now: Double?, higherIsWorse: Boolean) {
            if (base == null || now == null || base == 0.0) return
            val change = (now - base) / base * (if (higherIsWorse) 1 else -1)
            val line = "%-100s %-5s %14.3f -> %14.3f (%+.1f%% worse)".format(key, metric, base, now, change * 100)
            if (change <= tolerances.getValue(metric)) {
                println(line)
            } else if (metric == "alloc") {
                println(line)
                regressions += line
            } else {
                println("$line (time, not gated)")
            }
        }
        for ((key, now) in read(resultsFile)) {
            val base = baseline[key]
            if (base == null) {
                println("%-100s no baseline".format(key))
                continue
            }
            when (now["mode"]) {
                "sample" -> {
                    check(key, "p50", percentile(base, "50.0"), percentile(now, "50.0"), true)
                    check(key, "p95", percentile(base, "95.0"), percentile(now, "95.0"), true)
                }
                "thrpt" -> check(key, "thrpt", score(base), score(now), false)
                else -> check(key, "avgt", score(base), score(now), true)
            }
            check(key, "alloc", alloc(base), alloc(now), true)
        }
        if (regressions.isNotEmpty()) {
            throw GradleException("Benchmarks regressed beyond the tolerances:\n" + regressions.joinToString("\n"))
        }
    }
}
