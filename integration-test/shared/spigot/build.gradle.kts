plugins {
    `java-library`
}

repositories {
    mavenCentral()
    maven {
        name = "spigotmc-repo"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        url = uri("https://libraries.minecraft.net")
    }
}

dependencies {
    compileOnly(project(":common"))
    compileOnly(project(":spigot"))
    compileOnly(project(":integration-test:shared:core"))
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("com.mojang:brigadier:1.0.18")
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

// IntelliJ asks every Gradle project for this task while importing Kotlin DSL projects.
tasks.register("prepareKotlinBuildScriptModel")
