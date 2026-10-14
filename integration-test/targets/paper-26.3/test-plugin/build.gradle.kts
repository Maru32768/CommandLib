extra["platformDependencies"] = "io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT"
extra["javaVersion"] = "25"
extra["platform"] = "paper"
extra["commandlibModule"] = "paper"
extra["minecraftServerVersion"] = "26.3"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/d6a5589a720fbe74b15fd9be6679bff3c55f737801e149fab8337cd0b0a1a989/paper-26.3-180.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
