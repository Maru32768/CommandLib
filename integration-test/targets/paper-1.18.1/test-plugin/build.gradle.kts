extra["platformDependencies"] = "io.papermc.paper:paper-api:1.18.1-R0.1-SNAPSHOT"
extra["javaVersion"] = "17"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.18.1"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/a94917a4472c2cbc9907a15c666bbb784f95ecd7b53c77bc08fe71103e5487f5/paper-1.18.1-216.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
