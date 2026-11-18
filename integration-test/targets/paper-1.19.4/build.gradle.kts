val mcProtocol by configurations.creating

extra["cadence.integration.platform"] = "paper"
extra["cadence.integration.minecraftVersion"] = "1.19.4"
extra["cadence.integration.javaVersion"] = "17"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.19.4-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
