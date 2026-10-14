extra["platformDependencies"] = "io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT"
extra["javaVersion"] = "25"
extra["platform"] = "paper"
extra["commandlibModule"] = "paper"
extra["minecraftServerVersion"] = "26.1.2"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/1d70b1dab9cf4a6de615209a536f3a45a2186240253c428213ce2188ab95e5f7/paper-26.1.2-74.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
