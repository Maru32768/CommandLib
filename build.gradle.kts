import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("com.diffplug.spotless") version "8.10.3"
    id("net.ltgt.errorprone") version "5.1.1" apply false
    id("me.champeau.gradle.japicmp") version "0.4.6" apply false
    kotlin("jvm") version "2.3.20" apply false
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
    exclude("**/build/**", "**/.gradle*/**", ".claude/**")
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
    kotlin {
        target(javaSources("*-kotlin/src/**/*.kt", "examples/src/**/*.kt"))
        ktlint()
    }
}

val publishedArtifactIds = mapOf(
    ":modded:forge-1.16.5" to "forge-1.16.5",
    ":modded:forge-1.18.2" to "forge-1.18.2",
    ":modded:forge-1.19.2" to "forge-1.19.2",
    ":modded:forge-1.20.1" to "forge-1.20.1",
    ":modded:neoforge-1.21.1" to "neoforge-1.21.1",
    ":modded:neoforge-1.21.4" to "neoforge-1.21.4",
    ":modded:fabric-1.20.1" to "fabric-1.20.1",
    ":modded:fabric-1.21.1" to "fabric-1.21.1",
    ":modded:fabric-1.21.4" to "fabric-1.21.4",
    ":modded-testing:forge-1.16.5" to "forge-testing-1.16.5",
    ":modded-testing:forge-1.18.2" to "forge-testing-1.18.2",
    ":modded-testing:forge-1.19.2" to "forge-testing-1.19.2",
    ":modded-testing:forge-1.20.1" to "forge-testing-1.20.1",
    ":modded-testing:neoforge-1.21.1" to "neoforge-testing-1.21.1",
    ":modded-testing:neoforge-1.21.4" to "neoforge-testing-1.21.4",
    ":modded-testing:fabric-1.20.1" to "fabric-testing-1.20.1",
    ":modded-testing:fabric-1.21.1" to "fabric-testing-1.21.1",
    ":modded-testing:fabric-1.21.4" to "fabric-testing-1.21.4",
)

val publishedProjectPaths = setOf(
    ":common",
    ":common-testing",
    ":spigot",
    ":spigot-testing",
    ":paper",
    ":paper-testing",
    ":velocity",
    ":velocity-testing",
    ":common-kotlin",
    ":spigot-kotlin",
    ":paper-kotlin",
    ":velocity-kotlin",
    ":modded:forge-1.16.5",
    ":modded:forge-1.18.2",
    ":modded:forge-1.19.2",
    ":modded:forge-1.20.1",
    ":modded:neoforge-1.21.1",
    ":modded:neoforge-1.21.4",
    ":modded:fabric-1.20.1",
    ":modded:fabric-1.21.1",
    ":modded:fabric-1.21.4",
    ":modded-testing:forge-1.16.5",
    ":modded-testing:forge-1.18.2",
    ":modded-testing:forge-1.19.2",
    ":modded-testing:forge-1.20.1",
    ":modded-testing:neoforge-1.21.1",
    ":modded-testing:neoforge-1.21.4",
    ":modded-testing:fabric-1.20.1",
    ":modded-testing:fabric-1.21.1",
    ":modded-testing:fabric-1.21.4",
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

    // A console sender's locale is the JVM's, so the tests pin it to get CommandLib's English texts on any machine.
    tasks.withType<Test>().configureEach {
        systemProperty("user.language", "en")
        systemProperty("user.country", "US")
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

    // Legal documents are inputs of every archive, including sources, javadoc and remapped jars.
    tasks.withType<Jar>().configureEach {
        from(rootProject.file("LICENSE"))
        from(rootProject.file("THIRD_PARTY_NOTICES.md"))
        // Older builds copied LICENSE into resources/main; avoid duplicate entries on incremental builds.
        filesMatching(listOf("LICENSE", "THIRD_PARTY_NOTICES.md")) {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }

    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
    }

    tasks.named<Jar>("sourcesJar") {
        from(rootProject.file("README.md"))
    }
}

// The Kotlin DSL: common-kotlin and the <platform>-kotlin modules.
configure(subprojects.filter { it.name.endsWith("-kotlin") }) {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        // Plugins built with Kotlin 2.0 and later can read the DSL (a compiler reads metadata one version ahead), and the POM asks for no newer standard library.
        coreLibrariesVersion = "2.1.21"
        compilerOptions {
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        }
    }

    // Maven Central requires a javadoc jar. These modules have no Java sources, so it is empty; the KDoc is in the
    // sources jar.
    the<JavaPluginExtension>().withJavadocJar()

    dependencies {
        // Only executeSuspend and the other coroutine functions need it, so a plugin that does not call them does not
        // need it either.
        "compileOnly"("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
        "testImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.10.2")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        "testImplementation"("org.assertj:assertj-core:3.25.3")
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
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

// The published modules ship a javadoc jar. Building it runs doclint, which fails on broken Javadoc. Undocumented
// members are allowed.
configure(subprojects.filter { it.path in publishedProjectPaths }) {
    the<JavaPluginExtension>().withJavadocJar()
    tasks.named<Javadoc>("javadoc") {
        options.encoding = "UTF-8"
        (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:all,-missing", true)
    }
}

// Error Prone and NullAway cover the published modules and the typed NMS modules bundled into :spigot.
configure(subprojects.filter { it.path in publishedProjectPaths || it.path.startsWith(":nms:") }) {
    apply(plugin = "net.ltgt.errorprone")

    dependencies {
        "errorprone"("com.google.errorprone:error_prone_core:2.50.0")
        "errorprone"("com.uber.nullaway:nullaway:0.14.2")
    }

    val typedNms = path.startsWith(":nms:")
    val toolchainVersion = the<JavaPluginExtension>().toolchain.languageVersion
    // Error Prone runs only on JDK 21+. The typed NMS module of 26.x compiles against a server jar of Java 25.
    val errorProneCompiler = the<JavaToolchainService>().compilerFor {
        languageVersion.set(toolchainVersion.map { JavaLanguageVersion.of(maxOf(it.asInt(), 21)) })
    }
    tasks.withType<JavaCompile>().configureEach {
        javaCompiler.set(errorProneCompiler)
        // --release keeps the bytecode and the JDK API at the module's toolchain version, which tests and javadoc still
        // use. The typed NMS modules set their own release (typedNmsRelease).
        if (!typedNms) {
            options.release.set(toolchainVersion.map { it.asInt() })
        }
        options.errorprone.disableWarningsInGeneratedCode.set(true)
        // Brigadier nodes, dispatchers and commands are compared by identity on purpose.
        options.errorprone.disable("ReferenceEquality")
        // common, spigot and the Forge 1.16.5 node of the shared modded sources stay on Java 11, and the rest keeps
        // their style.
        options.errorprone.disable("PatternMatchingInstanceof", "StatementSwitchToExpressionSwitch")
        // Bukkit, Forge and NeoForge call event handlers through reflection.
        options.errorprone.option(
            "UnusedMethod:ExemptingMethodAnnotations",
            "org.bukkit.event.EventHandler,net.minecraftforge.eventbus.api.SubscribeEvent,net.neoforged.bus.api.SubscribeEvent",
        )
        // Tests pass null on purpose, so NullAway checks only the main sources.
        options.errorprone.check("NullAway", if (name == "compileJava") CheckSeverity.ERROR else CheckSeverity.OFF)
        options.errorprone.option("NullAway:AnnotatedPackages", "dev.marulabs.commandlib")
        // Error Prone warnings fail the build too, so they do not pile up again.
        options.compilerArgs.add("-Werror")
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
