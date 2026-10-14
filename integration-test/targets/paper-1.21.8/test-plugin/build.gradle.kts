extra["platformDependencies"] = "io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT"
extra["javaVersion"] = "21"
extra["platform"] = "paper"
extra["commandlibModule"] = "paper"
extra["minecraftServerVersion"] = "1.21.8"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/8de7c52c3b02403503d16fac58003f1efef7dd7a0256786843927fa92ee57f1e/paper-1.21.8-60.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
