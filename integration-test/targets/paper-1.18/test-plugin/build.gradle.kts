extra["platformDependencies"] = "io.papermc.paper:paper-api:1.18-R0.1-SNAPSHOT"
extra["javaVersion"] = "17"
extra["platform"] = "paper"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.18"
extra["serverJarDownloads"] =
    "https://fill-data.papermc.io/v1/objects/3c995f20dae4e4e21d5554fac957a0a8a5c85bd5bf34915fac4b4f16e0ef101b/paper-1.18-66.jar=>server/server.jar"

apply(from = "../../../shared/integration-test.gradle.kts")
