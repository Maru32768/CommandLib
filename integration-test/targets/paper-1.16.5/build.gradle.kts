val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "paper"
extra["commandlib.integration.minecraftVersion"] = "1.16.5"
extra["commandlib.integration.javaVersion"] = "11"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.16.5-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
