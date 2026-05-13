val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "mohist"
extra["commandlib.integration.minecraftVersion"] = "1.16.5"
extra["commandlib.integration.javaVersion"] = "17"
extra["commandlib.integration.serverJarName"] = "mohist.jar"
extra["commandlib.integration.requiresMohistBootstrap"] = "true"
extra["commandlib.integration.bootstrapServerPort"] = "25566"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.16.5-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
