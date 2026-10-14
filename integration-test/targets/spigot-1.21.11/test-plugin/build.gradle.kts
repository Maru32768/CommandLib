extra["platformDependencies"] = "org.spigotmc:spigot-api:1.21.11-R0.2-SNAPSHOT"
extra["javaVersion"] = "21"
extra["platform"] = "spigot"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.21.11"

apply(from = "../../../shared/integration-test.gradle.kts")
