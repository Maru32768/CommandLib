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
            id = "commandlib.spigot-nms"
            implementationClass = "commandlib.nms.SpigotNmsPlugin"
        }
    }
}

dependencies {
    implementation("net.md-5:SpecialSource:1.11.3")
}
