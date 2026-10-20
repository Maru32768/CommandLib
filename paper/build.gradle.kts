repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// The Adventure converter and its test are the same on Paper and Velocity.
sourceSets.main {
    java.srcDir(rootProject.file("common/src/adventure/java"))
    // The sources that compile unchanged on Spigot and Paper.
    java.srcDir(rootProject.file("common/src/bukkit/java"))
}
sourceSets.test {
    java.srcDir(rootProject.file("common/src/adventureTest/java"))
}

dependencies {
    api(project(":common"))
    compileOnly(libs.paper.api)
    compileOnlyApi(libs.brigadier)
    compileOnlyApi(libs.annotations)
    // FunctionLoadErrorFilter extends a Log4j class; the servers provide Log4j.
    compileOnly(libs.log4j.core)

    testImplementation(libs.paper.api)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj)
}

tasks.test {
    useJUnitPlatform()
}
