val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "paper"
extra["commandlib.integration.minecraftVersion"] = "26.1.1"
extra["commandlib.integration.javaVersion"] = "25"

dependencies {
    mcProtocol("org.geysermc.mcprotocollib:protocol:26.1-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
