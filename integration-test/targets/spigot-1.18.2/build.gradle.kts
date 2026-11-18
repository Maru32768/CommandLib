val mcProtocol by configurations.creating

extra["cadence.integration.platform"] = "spigot"
extra["cadence.integration.minecraftVersion"] = "1.18.2"
extra["cadence.integration.javaVersion"] = "17"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.18.2-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
