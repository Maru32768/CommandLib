plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("spigotNms") {
            id = "cadence.spigot-nms"
            implementationClass = "cadence.nms.SpigotNmsPlugin"
        }
    }
}

dependencies {
    implementation("net.md-5:SpecialSource:1.11.3")
}
