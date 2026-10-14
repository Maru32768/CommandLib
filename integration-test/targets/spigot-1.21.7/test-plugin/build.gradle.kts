extra["platformDependencies"] = "org.spigotmc:spigot-api:1.21.7-R0.1-SNAPSHOT"
extra["javaVersion"] = "21"
extra["platform"] = "spigot"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.21.7"

apply(from = "../../../shared/integration-test.gradle.kts")
