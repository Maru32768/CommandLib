val mcProtocol by configurations.creating

extra["cadence.integration.platform"] = "paper"
extra["cadence.integration.minecraftVersion"] = "1.21.0"
extra["cadence.integration.javaVersion"] = "21"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.21-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
