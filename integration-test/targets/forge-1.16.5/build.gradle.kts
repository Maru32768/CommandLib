val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "forge"
extra["commandlib.integration.minecraftVersion"] = "1.16.5"
extra["commandlib.integration.javaVersion"] = "11"
extra["commandlib.integration.workDirectory"] = "work"
extra["commandlib.integration.testPluginSubdirectory"] = "mods"
extra["commandlib.integration.testPluginJarPattern"] = "TestMod.*\\.jar"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.16.5-1")
}

apply(from = "../../gradle/forge-integration-target.gradle.kts")
apply(from = "../../gradle/integration-test-target.gradle.kts")
