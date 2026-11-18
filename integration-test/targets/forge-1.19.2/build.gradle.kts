val mcProtocol by configurations.creating

extra["cadence.integration.platform"] = "forge"
extra["cadence.integration.minecraftVersion"] = "1.19.2"
extra["cadence.integration.javaVersion"] = "17"
extra["cadence.integration.workDirectory"] = "work"
extra["cadence.integration.testPluginSubdirectory"] = "mods"
extra["cadence.integration.testPluginJarPattern"] = "TestMod.*\\.jar"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.19.2-1")
}

apply(from = "../../gradle/forge-integration-target.gradle.kts")
apply(from = "../../gradle/integration-test-target.gradle.kts")
