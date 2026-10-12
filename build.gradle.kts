import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("com.diffplug.spotless") version "8.10.3"
    id("net.ltgt.errorprone") version "5.1.1" apply false
    id("me.champeau.gradle.japicmp") version "0.4.6" apply false
}

allprojects {
    group = "dev.marulabs"
    version = "0.17.2"
}

// Spotless resolves the formatter from here.
repositories {
    mavenCentral()
}

// One Java target for the whole tree: Stonecutter nodes compile generated copies of modded/src, and the samples are
// separate builds, so per-project source sets would miss or duplicate files.
fun javaSources(vararg includes: String) = fileTree(rootDir) {
    include(*includes)
    exclude("**/build/**", ".gradle/**", ".claude/**")
    // Written by tools/nms-codegen.
    exclude("nms/**", "spigot/src/main/java/dev/marulabs/commandlib/util/nms/TypedNmsModules.java")
}

// Stonecutter trees keep version-specific imports in conditional comments, which sorting the imports would break.
val stonecutterSources = arrayOf(
    "modded/src/**/*.java",
    "modded-testing/src/**/*.java",
    "integration-test/shared/modded/src/**/*.java",
)

fun com.diffplug.gradle.spotless.JavaExtension.formatJava() {
    eclipse().configFile("config/eclipse-formatter.xml")
    // Eclipse puts the initializer of a double brace initialization (new Command("x") {{ ... }}) on lines of its own.
    // Join the braces again and dedent the body, as IntelliJ formats it.
    custom("doubleBraceInitialization", DoubleBraceInitialization())
    trimTrailingWhitespace()
    endWithNewline()
}

spotless {
    java {
        target(javaSources("**/src/**/*.java").minus(javaSources(*stonecutterSources)))
        formatJava()
        importOrder("", "javax|java", "\\#")
    }
    format("stonecutterJava", com.diffplug.gradle.spotless.JavaExtension::class.java) {
        target(javaSources(*stonecutterSources))
        formatJava()
    }
}

val publishedArtifactIds = mapOf(
    ":modded:forge-1.16.5" to "forge-1.16.5",
    ":modded:forge-1.20.1" to "forge-1.20.1",
    ":modded:neoforge-1.21.1" to "neoforge-1.21.1",
    ":modded-testing:forge-1.16.5" to "forge-testing-1.16.5",
    ":modded-testing:forge-1.20.1" to "forge-testing-1.20.1",
    ":modded-testing:neoforge-1.21.1" to "neoforge-testing-1.21.1",
)

val publishedProjectPaths = setOf(
    ":common",
    ":common-testing",
    ":spigot",
    ":spigot-testing",
    ":paper",
    ":paper-testing",
    ":modded:forge-1.16.5",
    ":modded:forge-1.20.1",
    ":modded:neoforge-1.21.1",
    ":modded-testing:forge-1.16.5",
    ":modded-testing:forge-1.20.1",
    ":modded-testing:neoforge-1.21.1",
)

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "idea")

    extensions.configure<org.gradle.plugins.ide.idea.model.IdeaModel> {
        module {
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }

    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(11))
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    if (project.path in publishedProjectPaths) {
        apply(plugin = "maven-publish")

        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    groupId = rootProject.group.toString()
                    artifactId = publishedArtifactIds[project.path] ?: project.name
                    version = project.version.toString()

                    from(components["java"])

                    pom {
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://opensource.org/licenses/MIT")
                            }
                        }
                    }
                }
            }
        }
    }

    tasks.named<Jar>("jar") {
        doFirst {
            copy {
                from(project.rootDir.toPath().toAbsolutePath())
                into(layout.buildDirectory.dir("resources/main"))
                include("LICENSE*")
            }
        }
    }

    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
    }

    tasks.named<Jar>("sourcesJar") {
        from(rootProject.file("README.md"))
    }
}

// Serializable, so that Spotless can tell whether its steps changed.
class DoubleBraceInitialization : com.diffplug.spotless.FormatterFunc, java.io.Serializable {
    override fun apply(text: String): String {
        fun indentOf(line: String) = line.length - line.trimStart().length
        val lines = text.split("\n").toMutableList()
        var i = 0
        while (i + 1 < lines.size) {
            val indent = indentOf(lines[i])
            val bodyIndent = indentOf(lines[i + 1])
            if (lines[i].endsWith("{") && lines[i + 1].trim() == "{" && bodyIndent > indent) {
                val close = (i + 2 until lines.size).first {
                    lines[it].isNotBlank() && indentOf(lines[it]) <= bodyIndent
                }
                if (lines[close].trim() == "}" && indentOf(lines[close]) == bodyIndent &&
                    close + 1 < lines.size && lines[close + 1].trim().startsWith("}") &&
                    indentOf(lines[close + 1]) == indent) {
                    val shift = bodyIndent - indent
                    for (j in i + 2 until close) {
                        lines[j] = if (lines[j].isBlank()) lines[j] else lines[j].substring(shift)
                    }
                    lines[i] += "{"
                    // The closing line gets one more brace, so calls chained on it and aligned on its dots move too.
                    val dots = lines[close + 1].indices.filter { lines[close + 1][it] == '.' }.toSet()
                    var chained = close + 2
                    while (chained < lines.size && lines[chained].trimStart().startsWith(".") &&
                        indentOf(lines[chained]) in dots) {
                        lines[chained] = " " + lines[chained++]
                    }
                    lines[close + 1] = " ".repeat(indent) + "}" + lines[close + 1].trimStart()
                    lines.removeAt(close)
                    lines.removeAt(i + 1)
                }
            }
            i++
        }
        return lines.joinToString("\n")
    }
}

// Error Prone and the javadoc jar cover the Bukkit-side modules. The typed NMS modules and the modded nodes build with
// their own toolchains and plugins, so they are left out.
val bukkitProjectPaths = setOf(
    ":common",
    ":common-testing",
    ":spigot",
    ":spigot-testing",
    ":paper",
    ":paper-testing",
)

configure(subprojects.filter { it.path in bukkitProjectPaths }) {
    apply(plugin = "net.ltgt.errorprone")

    // Building the javadoc jar runs doclint, which fails on broken Javadoc. Undocumented members are allowed.
    the<JavaPluginExtension>().withJavadocJar()
    tasks.named<Javadoc>("javadoc") {
        options.encoding = "UTF-8"
        (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:all,-missing", true)
    }

    dependencies {
        "errorprone"("com.google.errorprone:error_prone_core:2.50.0")
        "errorprone"("com.uber.nullaway:nullaway:0.14.2")
    }

    val toolchainVersion = the<JavaPluginExtension>().toolchain.languageVersion
    val errorProneCompiler = the<JavaToolchainService>().compilerFor { languageVersion.set(JavaLanguageVersion.of(21)) }
    tasks.withType<JavaCompile>().configureEach {
        // Error Prone runs only on JDK 21+. --release keeps the bytecode and the JDK API at the module's toolchain
        // version, which tests and javadoc still use.
        javaCompiler.set(errorProneCompiler)
        options.release.set(toolchainVersion.map { it.asInt() })
        options.errorprone.disableWarningsInGeneratedCode.set(true)
        // Tests pass null on purpose, so NullAway checks only the main sources. It warns until the missing @Nullable
        // annotations it reports are added.
        options.errorprone.check("NullAway", if (name == "compileJava") CheckSeverity.WARN else CheckSeverity.OFF)
        options.errorprone.option("NullAway:AnnotatedPackages", "net.kunmc.lab.commandlib")
        // javac stops listing warnings at 100 by default.
        options.compilerArgs.addAll(listOf("-Xmaxwarns", "10000"))
    }
}

// The release whose JitPack artifact each module's public API is compared with by the japicmp task. A module joins once
// it has a release, and the entries move to each new release.
val apiBaselines = mapOf(
    ":common" to "common:0.17.2",
    // 0.17.2 published this module as bukkit.
    ":spigot" to "bukkit:0.17.2",
)

configure(subprojects.filter { it.path in apiBaselines }) {
    repositories {
        maven("https://jitpack.io") {
            content { includeGroup("com.github.Maru32768.CommandLib") }
        }
    }

    val baseline = apiBaselines.getValue(path)
    val apiBaseline = configurations.create("apiBaseline") { isTransitive = false }
    // The baseline's own CommandLib dependencies, such as common of the same release.
    val apiBaselineClasspath = configurations.create("apiBaselineClasspath")
    dependencies {
        apiBaseline("com.github.Maru32768.CommandLib:$baseline")
        apiBaselineClasspath("com.github.Maru32768.CommandLib:$baseline")
    }

    val compileClasspath = configurations.named("compileClasspath")
    // The compile-only platform APIs, which the published POMs leave out, without the current CommandLib modules.
    val externalCompileClasspath = compileClasspath.map { classpath ->
        classpath.incoming.artifactView {
            componentFilter { it !is org.gradle.api.artifacts.component.ProjectComponentIdentifier }
        }.files
    }
    tasks.register<me.champeau.gradle.japicmp.JapicmpTask>("japicmp") {
        group = "verification"
        description = "Reports public API changes against the $baseline release."
        oldArchives.from(apiBaseline)
        oldClasspath.from(apiBaselineClasspath, externalCompileClasspath)
        newArchives.from(tasks.named("jar"))
        newClasspath.from(tasks.named("jar"), compileClasspath)
        onlyBinaryIncompatibleModified.set(true)
        // Under 0.x semantic versioning only a minor release may break the API.
        failOnModification.set(baseline.substringAfter(":").substringBeforeLast(".") ==
                                   version.toString().substringBeforeLast("."))
        txtOutputFile.set(layout.buildDirectory.file("reports/japicmp/japicmp.txt"))
        htmlOutputFile.set(layout.buildDirectory.file("reports/japicmp/japicmp.html"))
    }
}
