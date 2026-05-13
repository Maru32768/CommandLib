extra["platformDependencies"] = "io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT"
extra["javaVersion"] = "21"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.20.6"
extra["serverJarDownloads"] =
    "https://api.papermc.io/v2/projects/paper/versions/1.20.6/builds/151/downloads/paper-1.20.6-151.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
