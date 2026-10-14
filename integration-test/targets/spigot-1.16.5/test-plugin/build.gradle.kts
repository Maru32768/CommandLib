extra["platformDependencies"] = "org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT"
extra["javaVersion"] = "11"
extra["platform"] = "spigot"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.16.5"

apply(from = "../../../shared/integration-test.gradle.kts")
