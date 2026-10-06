extra["platformDependencies"] = "io.papermc.paper:paper-api:1.19.2-R0.1-SNAPSHOT"
extra["javaVersion"] = "17"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.19.2"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/2eb5c7459ec94bcdc597ed711d549a3ab4b0fda13e412a0792a1a069b5903864/paper-1.19.2-307.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
