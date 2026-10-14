extra["platformDependencies"] = "org.spigotmc:spigot-api:26.2-R0.1-SNAPSHOT"
extra["javaVersion"] = "25"
extra["platform"] = "spigot"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "26.2"

apply(from = "../../../shared/integration-test.gradle.kts")
