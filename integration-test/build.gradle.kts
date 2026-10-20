repositories {
    mavenCentral()
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj)
    testImplementation(libs.awaitility)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers)
    testRuntimeOnly(libs.slf4j.simple)
    testImplementation(libs.adventure.text.serializer.gson)
    testImplementation(libs.adventure.text.serializer.json.legacy.impl)
}

tasks.test {
    useJUnitPlatform()
    inputs.dir("expected-cases")
    inputs.files(fileTree("targets") { include("*/build.gradle.kts") })
    filter {
        excludeTestsMatching("dev.marulabs.commandlib.integration.IntegrationTest")
    }
    // Real-server tests live in integration-test:targets:* and are intentionally opt-in there.
    // Keeping this property false by default preserves the lightweight coverage check on :integration-test:test.
    systemProperty("commandlib.rootDir", rootProject.projectDir.absolutePath)
    systemProperty("commandlib.integrationTestDir", project.projectDir.absolutePath)
    systemProperty("commandlib.runMinecraftIntegration", "false")
}

tasks.register("minecraftIntegrationTest") {
    group = "verification"
    description = "Runs all Docker-based Minecraft integration tests."
    dependsOn(provider {
        project(":integration-test:targets").childProjects.values.map { "${it.path}:minecraftIntegrationTest" }
    })
}

// IntelliJ asks every Gradle project for this task while importing Kotlin DSL projects. Defining the no-op
// task keeps the integration-test module importable even though it is not a normal application module.
tasks.register("prepareKotlinBuildScriptModel")
