extra["platformDependencies"] = "io.papermc.paper:paper-api:1.19.3-R0.1-SNAPSHOT"
extra["javaVersion"] = "17"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.19.3"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/3007f2c638d5f04ed32b6adaa33053fe3634ccfa74345c83d3ea4982d38db5dc/paper-1.19.3-448.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
