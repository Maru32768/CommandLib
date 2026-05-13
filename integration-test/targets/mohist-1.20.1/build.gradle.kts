val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "mohist"
extra["commandlib.integration.minecraftVersion"] = "1.20.1"
extra["commandlib.integration.javaVersion"] = "17"
extra["commandlib.integration.serverJarName"] = "mohist.jar"
extra["commandlib.integration.requiresMohistBootstrap"] = "true"
extra["commandlib.integration.bootstrapServerPort"] = "25567"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.20-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
