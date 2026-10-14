val mcProtocol by configurations.creating

// The spigot jar on a Paper release after its last Paper module, which has to refuse to run and name the paper artifact.
extra["commandlib.integration.platform"] = "paper"
extra["commandlib.integration.minecraftVersion"] = "26.3"
extra["commandlib.integration.javaVersion"] = "25"
extra["commandlib.integration.expectedEnableFailure"] = "use the paper artifact on Paper 26.3"

apply(from = "../../gradle/integration-test-target.gradle.kts")
