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

dependencies {
    api(project(":velocity"))
    compileOnly("com.velocitypowered:velocity-api:3.4.0")
    // CommandLibExtension; tests that use it already have JUnit.
    compileOnly("org.junit.jupiter:junit-jupiter-api:5.10.2")
    api("org.mockito:mockito-core:5.11.0")

    testImplementation("com.velocitypowered:velocity-api:3.4.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.25.3")
}

tasks.test {
    useJUnitPlatform()
}
