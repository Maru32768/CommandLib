val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "neoforge"
extra["commandlib.integration.minecraftVersion"] = "1.21.4"
extra["commandlib.integration.javaVersion"] = "21"
extra["commandlib.integration.workDirectory"] = "work"
extra["commandlib.integration.testPluginSubdirectory"] = "mods"
extra["commandlib.integration.testPluginJarPattern"] = "TestMod.*\\.jar"

dependencies {
    mcProtocol("org.geysermc.mcprotocollib:protocol:1.21.4-SNAPSHOT")
}

apply(from = "../../gradle/forge-integration-target.gradle.kts")
apply(from = "../../gradle/integration-test-target.gradle.kts")
