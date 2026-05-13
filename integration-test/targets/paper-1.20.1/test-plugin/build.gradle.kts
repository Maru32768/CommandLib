extra["platformDependencies"] = "io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT"
extra["javaVersion"] = "17"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.20.1"
extra["serverJarDownloads"] =
    "https://api.papermc.io/v2/projects/paper/versions/1.20.1/builds/196/downloads/paper-1.20.1-196.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
