extra["platformDependencies"] = "io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT"
extra["javaVersion"] = "25"
extra["platform"] = "paper"
extra["commandlibModule"] = "paper"
extra["minecraftServerVersion"] = "26.1.1"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/43d08ed52c7af4e9f72769122501a5c15f99c7ace0c428a3eef040fd3f6fdbc5/paper-26.1.1-29.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
