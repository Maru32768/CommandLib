val mcProtocol by configurations.creating

extra["cadence.integration.platform"] = "mohist"
extra["cadence.integration.minecraftVersion"] = "1.20.1"
extra["cadence.integration.javaVersion"] = "17"
extra["cadence.integration.serverJarName"] = "mohist.jar"
extra["cadence.integration.requiresMohistBootstrap"] = "true"
extra["cadence.integration.bootstrapServerPort"] = "25567"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.20-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
