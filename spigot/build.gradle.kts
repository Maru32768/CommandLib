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
    compileOnly("com.mojang:brigadier:1.0.18")

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
    // The real-jar NMS resolver test reads the server jar that BuildTools installs for :nms:spigot-1.16.5.
    systemProperty("commandlib.nmsTestJar.1_16_5",
                   File(commandlib.nms.mavenLocalRepository(),
                        "org/spigotmc/spigot/1.16.5-R0.1-SNAPSHOT/spigot-1.16.5-R0.1-SNAPSHOT.jar").absolutePath)
}

// Typed NMS implementations compiled in the :nms modules are bundled into the spigot jar. NMSClassRegistry looks
// them up by class name, so the jar still works with the reflection implementations when they are left out.
val typedNms by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    rootProject.subprojects
        .filter { it.parent?.path == ":nms" }
        .forEach { typedNms(project(it.path, "nmsElements")) }
}

val collectTypedNms by tasks.registering(Sync::class) {
    description = "Collects the typed NMS jars bundled into the spigot jar."
    from(typedNms)
    into(layout.buildDirectory.dir("typed-nms"))
}

tasks.named<Jar>("jar") {
    // Only the classes: each module jar also carries the LICENSE that this jar already has.
    from(typedNms.elements.map { files -> files.map { zipTree(it.asFile) } }) {
        include("net/kunmc/lab/commandlib/nms/**")
    }
}
