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

// The sources that compile unchanged on Spigot and Paper.
sourceSets.main {
    java.srcDir(rootProject.file("common/src/bukkit/java"))
}

dependencies {
    api(project(":common"))
    compileOnly(libs.spigot.api)
    compileOnlyApi(libs.annotations)
    // FunctionLoadErrorFilter extends a Log4j class; the servers provide Log4j.
    compileOnly(libs.log4j.core)
    compileOnly(libs.brigadier)

    testImplementation(libs.spigot.api)
    testImplementation(libs.brigadier)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj.legacy)
    testImplementation(libs.mockito.core.legacy)
    testImplementation(libs.mockito.inline.legacy)
    // Mockito's own dependency, for an instance of CommandLib without its constructor, which Mockito cannot mock.
    testImplementation(libs.objenesis)
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
        include("dev/marulabs/commandlib/internal/nms/typed/**")
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
