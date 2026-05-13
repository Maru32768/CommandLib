plugins {
    `java-library`
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

// IntelliJ asks every Gradle project for this task while importing Kotlin DSL projects.
tasks.register("prepareKotlinBuildScriptModel")
