extra["platformDependencies"] = "io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT"
extra["javaVersion"] = "25"
extra["platform"] = "paper"
extra["commandlibModule"] = "paper"
extra["minecraftServerVersion"] = "26.2"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/bf1dcf627364f8a631ab15250d240eb5a8e89faaac523b391e1c61367fce9ff1/paper-26.2-133.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
