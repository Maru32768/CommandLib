import commandlib.nms.SpigotRemapTask

plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

// Paper runs with Mojang names from 1.20.5, so the paperweight dev bundle compiles against them. Paper still remaps a
// plugin from Spigot names to Mojang names unless the plugin declares Mojang mappings, and some Spigot names are Mojang
// names of other classes, so :spigot also bundles the reobfuscated jar moved to a package of its own. MappingProbe
// tells NMSClassRegistry which of the two packages matches the plugin at runtime.
repositories {
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

// The classes target an older Java release than the dev bundle needs, for the shading tools of plugins that bundle the
// spigot jar (see typedNmsRelease).
configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    disableAutoTargetJvm()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(commandlib.nms.typedNmsRelease("1.20.6"))
}

dependencies {
    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT")
    compileOnly(project(":spigot"))
}

val typedPackage = "net/kunmc/lab/commandlib/nms/paper_1_20_6"

val writeSpigotPackageMapping by tasks.registering {
    val mapping = layout.buildDirectory.file("remap/spigot-package.srg")
    inputs.property("typedPackage", typedPackage)
    outputs.file(mapping)
    doLast {
        mapping.get().asFile.writeText("PK: $typedPackage ${typedPackage}_spigot\n")
    }
}

val relocateReobfJar by tasks.registering(SpigotRemapTask::class) {
    val reobfJar = tasks.named("reobfJar")
    dependsOn(reobfJar)
    inputJar.fileProvider(reobfJar.map { it.outputs.files.singleFile })
    outputJar.set(layout.buildDirectory.file("remap/${project.name}-spigot.jar"))
    mappingFiles.from(writeSpigotPackageMapping)
    reverse.set(false)
}

configurations.create("nmsElements") {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add("nmsElements", tasks.jar)
    add("nmsElements", relocateReobfJar.flatMap { it.outputJar })
}
