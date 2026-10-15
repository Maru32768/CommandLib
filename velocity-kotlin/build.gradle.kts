repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

// The command factories and their tests are the same on every platform.
sourceSets.main {
    kotlin.srcDir(rootProject.file("common-kotlin/src/platform/kotlin"))
}
sourceSets.test {
    kotlin.srcDir(rootProject.file("common-kotlin/src/platformTest/kotlin"))
}

dependencies {
    api(project(":common-kotlin"))
    api(project(":velocity"))
    compileOnly("com.velocitypowered:velocity-api:3.4.0")

    testImplementation(project(":velocity-testing"))
    testImplementation("com.velocitypowered:velocity-api:3.4.0")
}
