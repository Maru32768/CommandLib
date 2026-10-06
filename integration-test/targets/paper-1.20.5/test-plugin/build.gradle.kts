extra["platformDependencies"] = "io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT"
extra["javaVersion"] = "21"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.20.5"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/3cd7da2f8df92e082a501a39c674aab3c0343edd179b86f5baccaebfc9974132/paper-1.20.5-22.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
