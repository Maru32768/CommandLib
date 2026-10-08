extra["platformDependencies"] = "io.papermc.paper:paper-api:1.20-R0.1-SNAPSHOT"
extra["javaVersion"] = "17"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.20"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/1e4ccfc0599f491ee6fee4455d3722332ac5d78584fccd55cbb3b51e11504505/paper-1.20-17.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
