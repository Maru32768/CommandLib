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
    // FunctionLoadErrorFilter extends a Log4j class; the servers provide Log4j.
    compileOnly("org.apache.logging.log4j:log4j-core:2.17.1")
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
}

// The typed NMS implementations compiled in the :nms modules are bundled into the spigot jar. They are the only NMS
// implementations, so a jar built with some modules left out (-Pcommandlib.typedNms) lacks the versions they cover.
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
        include("dev/marulabs/commandlib/nms/**")
    }
}

// A jar without some modules has no NMS implementation for their versions, so it must not be published.
val missingTypedNms = rootProject.file("nms")
    .listFiles()
    .orEmpty()
    .filter { it.resolve("build.gradle.kts").isFile && rootProject.findProject(":nms:${it.name}") == null }
    .map { it.name }
    .sorted()
tasks.withType<AbstractPublishToMaven>().configureEach {
    doFirst {
        check(missingTypedNms.isEmpty()) {
            "The spigot jar lacks the typed NMS modules $missingTypedNms, which -Pcommandlib.typedNms left out. " +
                "Publish without that property."
        }
    }
}
