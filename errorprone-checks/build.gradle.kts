// The repository's own Error Prone checks. The root build puts them on the Error Prone path of every module that runs
// Error Prone, next to the stock checks and NullAway.
repositories {
    mavenCentral()
}

configure<JavaPluginExtension> {
    // Error Prone runs on JDK 21+, so its plugins may use that bytecode.
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// The checks use javac's internal API, which jdk.compiler does not export.
val javacPackages = listOf("api", "code", "comp", "file", "main", "model", "parser", "processing", "tree", "util")
val javacExports = javacPackages.map { "--add-exports=jdk.compiler/com.sun.tools.javac.$it=ALL-UNNAMED" }

dependencies {
    compileOnly(libs.errorprone.check.api)

    testImplementation(libs.errorprone.test.helpers)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(javacExports)
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(javacExports + javacPackages.map { "--add-opens=jdk.compiler/com.sun.tools.javac.$it=ALL-UNNAMED" })
}
