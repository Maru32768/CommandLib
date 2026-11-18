val mcProtocol by configurations.creating

extra["cadence.integration.platform"] = "mohist"
extra["cadence.integration.minecraftVersion"] = "1.16.5"
extra["cadence.integration.javaVersion"] = "17"
extra["cadence.integration.serverJarName"] = "mohist.jar"
extra["cadence.integration.requiresMohistBootstrap"] = "true"
extra["cadence.integration.bootstrapServerPort"] = "25566"
// A start with the library check puts Guava 20 before the server's Guava 21 and fails in GameRules (NoSuchMethodError).
extra["cadence.integration.mohistInstallArgs"] = "-noserver"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.16.5-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
