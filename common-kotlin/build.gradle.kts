repositories {
    mavenCentral()
    maven {
        url = uri("https://libraries.minecraft.net")
    }
}

dependencies {
    api(project(":common"))
    compileOnly("com.mojang:brigadier:1.0.18")
    compileOnly("org.jetbrains:annotations:16.0.2")
}
