extra["platformDependencies"] = "io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT"
extra["javaVersion"] = "21"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.21.1"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/39bd8c00b9e18de91dcabd3cc3dcfa5328685a53b7187a2f63280c22e2d287b9/paper-1.21.1-133.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
