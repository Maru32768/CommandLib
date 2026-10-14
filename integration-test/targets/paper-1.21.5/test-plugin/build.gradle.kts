extra["platformDependencies"] = "io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT"
extra["javaVersion"] = "21"
extra["platform"] = "paper"
extra["commandlibModule"] = "paper"
extra["minecraftServerVersion"] = "1.21.5"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/2ae6ae22adf417699746e0f89fc2ef6cb6ee050a5f6608cee58f0535d60b509e/paper-1.21.5-114.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
