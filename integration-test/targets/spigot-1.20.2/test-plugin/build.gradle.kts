extra["platformDependencies"] = "org.spigotmc:spigot-api:1.20.2-R0.1-SNAPSHOT"
extra["javaVersion"] = "17"
extra["platform"] = "spigot"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.20.2"

apply(from = "../../../shared/integration-test.gradle.kts")
