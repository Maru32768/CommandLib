extra["platformDependencies"] = "io.papermc.paper:paper-api:1.19-R0.1-SNAPSHOT"
extra["javaVersion"] = "17"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.19"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/0d39cacc51a77b2b071e1ce862fcbf0b4a4bd668cc7e8b313598d84fa09fabac/paper-1.19-81.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
