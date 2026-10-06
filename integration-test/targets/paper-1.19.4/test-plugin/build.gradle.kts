extra["platformDependencies"] = "io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT"
extra["javaVersion"] = "17"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.19.4"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/4e9da8b307d36a6b0d595ff8d93c5f63ef602b27a84f7851bb37a7a532f88262/paper-1.19.4-538.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
