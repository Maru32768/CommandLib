extra["platformDependencies"] = "org.spigotmc:spigot-api:1.18.1-R0.1-SNAPSHOT"
extra["javaVersion"] = "17"
extra["platform"] = "spigot"
extra["commandlibModule"] = "spigot"
extra["minecraftServerVersion"] = "1.18.1"

apply(from = "../../../shared/integration-test.gradle.kts")
