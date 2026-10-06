extra["platformDependencies"] = "io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT"
extra["javaVersion"] = "17"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.20.1"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/234a9b32098100c6fc116664d64e36ccdb58b5b649af0f80bcccb08b0255eaea/paper-1.20.1-196.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
