extra["platformDependencies"] = "io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT"
extra["javaVersion"] = "21"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.20.6"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/4b011f5adb5f6c72007686a223174fce82f31aeb4b34faf4652abc840b47e640/paper-1.20.6-151.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
