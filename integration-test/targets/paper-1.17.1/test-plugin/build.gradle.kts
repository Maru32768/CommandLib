extra["platformDependencies"] = "io.papermc.paper:paper-api:1.17.1-R0.1-SNAPSHOT"
extra["javaVersion"] = "17"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.17.1"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/6cc1ee2f94253ce10b5374ed85fffc735a97d8f1b64db293683dfa24dd3cc05f/paper-1.17.1-411.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
