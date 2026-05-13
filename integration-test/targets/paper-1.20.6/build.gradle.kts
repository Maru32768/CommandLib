val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "paper"
extra["commandlib.integration.minecraftVersion"] = "1.20.6"
extra["commandlib.integration.javaVersion"] = "21"

dependencies {
    mcProtocol("org.geysermc.mcprotocollib:protocol:1.20.6-2-SNAPSHOT")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
