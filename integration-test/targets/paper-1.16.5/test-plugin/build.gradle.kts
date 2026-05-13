extra["platformDependencies"] = "com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT"
extra["javaVersion"] = "11"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.16.5"
extra["serverJarDownloads"] =
    "https://api.papermc.io/v2/projects/paper/versions/1.16.5/builds/794/downloads/paper-1.16.5-794.jar=>server/server.jar"
extra["includeProtocolLib"] = "true"

apply(from = "../../../shared/integration-test.gradle.kts")
