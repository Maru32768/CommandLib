extra["platformDependencies"] = "io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT"
extra["javaVersion"] = "21"
extra["platform"] = "paper"
extra["commandlibModule"] = "paper"
extra["minecraftServerVersion"] = "1.21.6"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/35e2dfa66b3491b9d2f0bb033679fa5aca1e1fdf097e7a06a80ce8afeda5c214/paper-1.21.6-48.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
