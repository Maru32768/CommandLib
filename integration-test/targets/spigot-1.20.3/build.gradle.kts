val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "spigot"
extra["commandlib.integration.minecraftVersion"] = "1.20.3"
extra["commandlib.integration.javaVersion"] = "21"
// BuildTools now resolves 1.20.3 to the 1.20.4 build, so build the last 1.20.3 build.
extra["commandlib.integration.buildToolsRevision"] = "3961"

dependencies {
    // 1.20.3 and 1.20.4 share their protocol.
    mcProtocol("com.github.steveice10:mcprotocollib:1.20.4-1") {
        exclude(group = "net.kyori", module = "adventure-text-serializer-gson")
        exclude(group = "net.kyori", module = "adventure-text-serializer-json-legacy-impl")
    }
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
