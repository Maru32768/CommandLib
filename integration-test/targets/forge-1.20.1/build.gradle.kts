val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "forge"
extra["commandlib.integration.minecraftVersion"] = "1.20.1"
extra["commandlib.integration.javaVersion"] = "17"
extra["commandlib.integration.forgeVersion"] = "1.20.1-47.4.10"
extra["commandlib.integration.workDirectory"] = "work"
extra["commandlib.integration.testPluginSubdirectory"] = "mods"
extra["commandlib.integration.testPluginJarPattern"] = "TestMod.*\\.jar"
extra["commandlib.integration.serverLaunchArgs"] = "@libraries/net/minecraftforge/forge/1.20.1-47.4.10/unix_args.txt"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.20-1")
}

apply(from = "../../gradle/forge-integration-target.gradle.kts")
apply(from = "../../gradle/integration-test-target.gradle.kts")
