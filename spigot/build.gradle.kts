repositories {
    mavenCentral()
    maven {
        name = "spigotmc-repo"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://libraries.minecraft.net")
    }
}

dependencies {
    api(project(":common"))
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    compileOnlyApi("org.jetbrains:annotations:16.0.2")
//    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("com.mojang:brigadier:1.0.18")
//    compileOnly(fileTree(mapOf("dir" to "../integration-test/targets/paper-1.16.5/test-plugin/", "include" to listOf("server/cache/patched*.jar"))))
//    compileOnly(fileTree(mapOf("dir" to "../integration-test/targets/paper-1.19.4/test-plugin/", "include" to listOf("server/versions/1.19.4/paper*.jar", "server_mojmap/versions/1.19.4/paper*jar"))))

    testImplementation("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    testImplementation("com.mojang:brigadier:1.0.18")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.25.1")
    testImplementation("org.mockito:mockito-core:4.8.1")
    testImplementation("org.mockito:mockito-inline:4.8.1")
}

tasks.test {
    useJUnitPlatform()
}
