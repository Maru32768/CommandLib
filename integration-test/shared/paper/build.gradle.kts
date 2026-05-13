plugins {
    `java-library`
}

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly(project(":common"))
    compileOnly(project(":paper"))
    compileOnly(project(":integration-test:shared:core"))
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly("com.mojang:brigadier:1.0.18")
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// IntelliJ asks every Gradle project for this task while importing Kotlin DSL projects.
tasks.register("prepareKotlinBuildScriptModel")
