val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "paper"
extra["commandlib.integration.minecraftVersion"] = "1.21.11"
extra["commandlib.integration.javaVersion"] = "21"

dependencies {
    mcProtocol("org.geysermc.mcprotocollib:protocol:1.21.11-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
