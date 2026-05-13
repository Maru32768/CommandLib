extra["platformDependencies"] = "io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT"
extra["javaVersion"] = "17"
extra["platform"] = "mohist"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.20.1"
extra["serverJarDownloads"] =
    "https://mohistmc.com/builds-raw/Mohist-1.20.1/Mohist-1.20.1-923.jar=>server/mohist.jar"
extra["includeProtocolLib"] = "true"
extra["nmsGenerationServerJar"] = "mohist.jar"
extra["nmsJarPaths"] =
    "libraries/net/minecraftforge/forge/1.20.1-47.3.22/forge-1.20.1-47.3.22-server.jar|" +
            "libraries/net/minecraftforge/forge/1.20.1-47.3.22/forge-1.20.1-47.3.22-universal.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
