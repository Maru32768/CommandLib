val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "spigot"
extra["commandlib.integration.minecraftVersion"] = "1.20.4"
extra["commandlib.integration.javaVersion"] = "21"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.20.4-1") {
        exclude(group = "net.kyori", module = "adventure-text-serializer-gson")
        exclude(group = "net.kyori", module = "adventure-text-serializer-json-legacy-impl")
    }
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
