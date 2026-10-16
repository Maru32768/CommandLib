// The fixture checks the thread with Bukkit#isGlobalTickThread, which paper-api has from 1.21.1.
extra["platformDependencies"] = "io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT"
extra["javaVersion"] = "25"
extra["platform"] = "folia"
extra["commandlibModule"] = "paper"
extra["testPluginSuite"] = "folia"
extra["minecraftServerVersion"] = "26.3"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/0c4cc7dd82c40bd1effa8a9c4cacfad55d41b7dc38377900957c961c2952c022/folia-26.3-3.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
