val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "neoforge"
extra["commandlib.integration.minecraftVersion"] = "1.21.1"
extra["commandlib.integration.javaVersion"] = "21"
extra["commandlib.integration.forgeVersion"] = "21.1.256"
extra["commandlib.integration.workDirectory"] = "work"
extra["commandlib.integration.testPluginSubdirectory"] = "mods"
extra["commandlib.integration.testPluginJarPattern"] = "TestMod.*\\.jar"
extra["commandlib.integration.serverLaunchArgs"] = "@libraries/net/neoforged/neoforge/21.1.256/unix_args.txt"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.21-1")
}

apply(from = "../../gradle/forge-integration-target.gradle.kts")
apply(from = "../../gradle/integration-test-target.gradle.kts")
