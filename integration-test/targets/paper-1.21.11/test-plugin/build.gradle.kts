extra["platformDependencies"] = "io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT"
extra["javaVersion"] = "21"
extra["platform"] = "paper"
extra["commandlibModule"] = "paper"
extra["minecraftServerVersion"] = "1.21.11"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba/paper-1.21.11-132.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
