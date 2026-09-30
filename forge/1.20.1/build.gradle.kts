plugins {
    id("net.minecraftforge.gradle") version "6.0.36"
}

repositories {
    maven { url = uri("https://maven.minecraftforge.net/") }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

minecraft {
    mappings("official", "1.20.1")
}

dependencies {
    api(project(":common"))
    add("minecraft", "net.minecraftforge:forge:1.20.1-47.4.10")
}
