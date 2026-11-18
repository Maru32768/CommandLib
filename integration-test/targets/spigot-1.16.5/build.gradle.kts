val mcProtocol by configurations.creating

extra["cadence.integration.platform"] = "spigot"
extra["cadence.integration.minecraftVersion"] = "1.16.5"
extra["cadence.integration.javaVersion"] = "11"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.16.5-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
