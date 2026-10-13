val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "mohist"
extra["commandlib.integration.minecraftVersion"] = "1.16.5"
extra["commandlib.integration.javaVersion"] = "17"
extra["commandlib.integration.serverJarName"] = "mohist.jar"
extra["commandlib.integration.requiresMohistBootstrap"] = "true"
extra["commandlib.integration.bootstrapServerPort"] = "25566"
// A start with the library check puts Guava 20 before the server's Guava 21 and fails in GameRules (NoSuchMethodError).
extra["commandlib.integration.mohistInstallArgs"] = "-noserver"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.16.5-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
