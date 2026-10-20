repositories {
    mavenCentral()
    maven {
        url = uri("https://libraries.minecraft.net")
    }
}

dependencies {
    api(project(":common"))
    compileOnly(libs.brigadier)
    compileOnly(libs.annotations)
}
