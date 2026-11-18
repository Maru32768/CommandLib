extra["platformDependencies"] = "org.spigotmc:spigot-api:1.21.3-R0.1-SNAPSHOT"
extra["javaVersion"] = "21"
extra["platform"] = "spigot"
extra["cadenceModule"] = "spigot"
extra["minecraftServerVersion"] = "1.21.3"

apply(from = "../../../shared/integration-test.gradle.kts")
