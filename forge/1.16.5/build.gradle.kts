plugins {
    id("net.minecraftforge.gradle") version "6.0.36"
}

repositories {
    maven { url = uri("https://maven.minecraftforge.net/") }
}

minecraft {
    mappings("snapshot", "20210309-1.16.5")
}

dependencies {
    api(project(":common"))
    add("minecraft", "net.minecraftforge:forge:1.16.5-36.2.20")
}
