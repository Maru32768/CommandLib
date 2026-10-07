extra["platformDependencies"] = "io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT"
extra["javaVersion"] = "17"
extra["platform"] = "mohist"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.20.1"
extra["serverJarDownloads"] =
    "https://mohistmc.com/builds-raw/Mohist-1.20.1/Mohist-1.20.1-923.jar=>server/mohist.jar"
extra["includeProtocolLib"] = "true"

apply(from = "../../../shared/integration-test.gradle.kts")
