extra["platformDependencies"] = "org.spigotmc:spigot-api:1.21.1-R0.1-SNAPSHOT"
extra["javaVersion"] = "21"
extra["platform"] = "spigot"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.21.1"

apply(from = "../../../shared/integration-test.gradle.kts")
