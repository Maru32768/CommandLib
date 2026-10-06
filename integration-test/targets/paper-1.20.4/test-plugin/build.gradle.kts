extra["platformDependencies"] = "io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT"
extra["javaVersion"] = "21"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.20.4"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/9c6419c3504db84d437d4bbeb5b9d27d548a009a3ed2f1c617b62e7341cfeee3/paper-1.20.4-496.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
