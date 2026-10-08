extra["platformDependencies"] = "io.papermc.paper:paper-api:1.19.1-R0.1-SNAPSHOT"
extra["javaVersion"] = "17"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.19.1"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/5afe23a1fade92c547124fa874bc7d908fa676f49f09879fa876224b62e9d51b/paper-1.19.1-111.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
