extra["platformDependencies"] = "io.papermc.paper:paper-api:1.20.2-R0.1-SNAPSHOT"
extra["javaVersion"] = "17"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.20.2"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/ba340a835ac40b8563aa7eda1cd6479a11a7623409c89a2c35cd9d7490ed17a7/paper-1.20.2-318.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
