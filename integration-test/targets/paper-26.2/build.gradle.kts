val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "paper"
extra["commandlib.integration.minecraftVersion"] = "26.2"
extra["commandlib.integration.javaVersion"] = "25"
extra["commandlib.integration.botJavaVersion"] = "21"

dependencies {
    mcProtocol("org.geysermc.mcprotocollib:protocol:26.2")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
