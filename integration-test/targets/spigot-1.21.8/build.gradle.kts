val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "spigot"
extra["commandlib.integration.minecraftVersion"] = "1.21.8"
extra["commandlib.integration.javaVersion"] = "21"

dependencies {
    mcProtocol("org.geysermc.mcprotocollib:protocol:1.21.7-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
