val mcProtocol by configurations.creating

extra["cadence.integration.platform"] = "spigot"
extra["cadence.integration.minecraftVersion"] = "1.20.4"
extra["cadence.integration.javaVersion"] = "21"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.20.4-1") {
        exclude(group = "net.kyori", module = "adventure-text-serializer-gson")
        exclude(group = "net.kyori", module = "adventure-text-serializer-json-legacy-impl")
    }
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
