repositories {
    mavenCentral()
    maven {
        url = uri("https://libraries.minecraft.net")
    }
}

dependencies {
    compileOnlyApi(libs.brigadier)
    compileOnlyApi(libs.annotations)
    // The servers log through Log4j; FunctionLoadErrorFilter uses it when it is there.
    compileOnly(libs.log4j.core)

    testImplementation(libs.brigadier)
    testImplementation(libs.log4j.core)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj.legacy)
}

tasks.test {
    useJUnitPlatform()
}
