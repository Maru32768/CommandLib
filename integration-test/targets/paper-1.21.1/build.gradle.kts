val mcProtocol by configurations.creating

extra["cadence.integration.platform"] = "paper"
extra["cadence.integration.minecraftVersion"] = "1.21.1"
extra["cadence.integration.javaVersion"] = "21"

dependencies {
    // 1.21 and 1.21.1 share their protocol.
    mcProtocol("com.github.steveice10:mcprotocollib:1.21-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
