val mcProtocol by configurations.creating

extra["cadence.integration.platform"] = "neoforge"
extra["cadence.integration.minecraftVersion"] = "1.21.4"
extra["cadence.integration.javaVersion"] = "21"
extra["cadence.integration.workDirectory"] = "work"
extra["cadence.integration.testPluginSubdirectory"] = "mods"
extra["cadence.integration.testPluginJarPattern"] = "TestMod.*\\.jar"

dependencies {
    mcProtocol("org.geysermc.mcprotocollib:protocol:1.21.4-SNAPSHOT")
}

apply(from = "../../gradle/forge-integration-target.gradle.kts")
apply(from = "../../gradle/integration-test-target.gradle.kts")
