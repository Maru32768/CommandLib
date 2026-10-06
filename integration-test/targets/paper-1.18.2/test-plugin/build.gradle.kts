extra["platformDependencies"] = "io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT"
extra["javaVersion"] = "17"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.18.2"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/0578f18f4d632b494b468ec56b3b414b5b56fea087ee7d39cf6dcdf4c9d01f05/paper-1.18.2-388.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
