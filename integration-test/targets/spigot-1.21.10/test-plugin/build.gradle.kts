extra["platformDependencies"] = "org.spigotmc:spigot-api:1.21.10-R0.1-SNAPSHOT"
extra["javaVersion"] = "21"
extra["platform"] = "spigot"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.21.10"

apply(from = "../../../shared/integration-test.gradle.kts")
