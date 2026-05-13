val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "paper"
extra["commandlib.integration.minecraftVersion"] = "1.19.4"
extra["commandlib.integration.javaVersion"] = "17"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.19.4-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
