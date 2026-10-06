extra["platformDependencies"] = "com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT"
extra["javaVersion"] = "11"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.16.5"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/e67da4851d08cde378ab2b89be58849238c303351ed2482181a99c2c2b489276/paper-1.16.5-794.jar=>server/server.jar"
extra["includeProtocolLib"] = "true"

apply(from = "../../../shared/integration-test.gradle.kts")
