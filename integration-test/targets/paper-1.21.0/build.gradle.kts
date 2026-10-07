val mcProtocol by configurations.creating

extra["commandlib.integration.platform"] = "paper"
extra["commandlib.integration.minecraftVersion"] = "1.21.0"
extra["commandlib.integration.javaVersion"] = "21"
extra["commandlib.integration.commandlibModule"] = "paper"

dependencies {
    mcProtocol("com.github.steveice10:mcprotocollib:1.21-1")
}

apply(from = "../../gradle/integration-test-target.gradle.kts")
