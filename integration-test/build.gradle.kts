repositories {
    mavenCentral()
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("org.awaitility:awaitility:4.2.1")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("org.testcontainers:testcontainers:1.21.4")
    testRuntimeOnly("org.slf4j:slf4j-simple:1.7.36")
    testImplementation("net.kyori:adventure-text-serializer-gson:4.15.0")
    testImplementation("net.kyori:adventure-text-serializer-json-legacy-impl:4.15.0")
}

tasks.test {
    useJUnitPlatform()
    filter {
        excludeTestsMatching("net.kunmc.lab.commandlib.integration.IntegrationTest")
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
    dependsOn(
        ":integration-test:targets:paper-1.16.5:minecraftIntegrationTest",
        ":integration-test:targets:mohist-1.16.5:minecraftIntegrationTest",
        ":integration-test:targets:paper-1.17.1:minecraftIntegrationTest",
        ":integration-test:targets:paper-1.18.2:minecraftIntegrationTest",
        ":integration-test:targets:paper-1.19.2:minecraftIntegrationTest",
        ":integration-test:targets:paper-1.19.4:minecraftIntegrationTest",
        ":integration-test:targets:paper-1.20.1:minecraftIntegrationTest",
        ":integration-test:targets:mohist-1.20.1:minecraftIntegrationTest",
        ":integration-test:targets:paper-1.20.4:minecraftIntegrationTest",
        ":integration-test:targets:paper-1.20.5:minecraftIntegrationTest",
        ":integration-test:targets:paper-1.20.6:minecraftIntegrationTest",
        ":integration-test:targets:paper-1.21.0:minecraftIntegrationTest",
        ":integration-test:targets:forge-1.16.5:minecraftIntegrationTest",
        ":integration-test:targets:forge-1.20.1:minecraftIntegrationTest",
        ":integration-test:targets:neoforge-1.21.1:minecraftIntegrationTest",
        ":integration-test:targets:spigot-1.20.4:minecraftIntegrationTest",
        ":integration-test:targets:spigot-1.20.6:minecraftIntegrationTest",
        ":integration-test:targets:paper-1.18:minecraftIntegrationTest",
        ":integration-test:targets:paper-1.18.1:minecraftIntegrationTest",
        ":integration-test:targets:paper-1.19:minecraftIntegrationTest",
        ":integration-test:targets:paper-1.19.1:minecraftIntegrationTest",
        ":integration-test:targets:paper-1.19.3:minecraftIntegrationTest",
        ":integration-test:targets:paper-1.20:minecraftIntegrationTest",
        ":integration-test:targets:paper-1.20.2:minecraftIntegrationTest",
    )
}

// IntelliJ asks every Gradle project for this task while importing Kotlin DSL projects. Defining the no-op
// task keeps the integration-test module importable even though it is not a normal application module.
tasks.register("prepareKotlinBuildScriptModel")
