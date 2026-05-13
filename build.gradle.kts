allprojects {
    group = "net.kunmc.lab"
    version = "0.17.2"
}

val publishedProjectPaths = setOf(":common", ":common-testing", ":spigot", ":spigot-testing", ":paper", ":paper-testing", ":forge")

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "idea")

    extensions.configure<org.gradle.plugins.ide.idea.model.IdeaModel> {
        module {
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }

    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(11))
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    if (project.path in publishedProjectPaths) {
        apply(plugin = "maven-publish")

        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    groupId = project.group.toString()
                    artifactId = project.name
                    version = project.version.toString()

                    from(components["java"])

                    pom {
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://opensource.org/licenses/MIT")
                            }
                        }
                    }
                }
            }
        }
    }

    tasks.named<Jar>("jar") {
        doFirst {
            copy {
                from(project.rootDir.toPath().toAbsolutePath())
                into(layout.buildDirectory.dir("resources/main"))
                include("LICENSE*")
            }
        }
    }

    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
    }

    tasks.named<Jar>("sourcesJar") {
        from(rootProject.file("README.md"))
    }
}
