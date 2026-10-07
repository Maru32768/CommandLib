extra["platformDependencies"] = "com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT"
extra["javaVersion"] = "11"
extra["platform"] = "mohist"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.16.5"
extra["serverJarDownloads"] =
    "https://mohistmc.com/builds-raw/1.16.5/1.16.5-044418daab9f95ca53469a0950e3b30197625b75.jar=>server/mohist.jar"
extra["includeProtocolLib"] = "true"

apply(from = "../../../shared/integration-test.gradle.kts")
