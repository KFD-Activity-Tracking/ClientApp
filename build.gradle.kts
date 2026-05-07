plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
    application
}

application{
    mainClass.set("MainKt")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.exposed:exposed-core:1.2.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:1.2.0")
    implementation("org.xerial:sqlite-jdbc:3.53.0.0")
    implementation("io.ktor:ktor-client-core:3.4.2")
    implementation("io.ktor:ktor-client-cio:3.4.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.4.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("com.github.kwhat:jnativehook:2.2.2")
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

// Fat JAR needed for jpackage
tasks.jar {
    manifest { attributes["Main-Class"] = "MainKt" }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

// Creates a platform-specific installer in build/installer/
tasks.register("installer") {
    dependsOn(tasks.jar)
    group = "distribution"
    description = "Package the app with jpackage"
    doLast {
        val javaHome = System.getProperty("java.home") ?: error("JAVA_HOME not set")
        val jpackage = "$javaHome/bin/jpackage"
        val jarPath  = layout.buildDirectory.file("libs/${project.name}-${project.version}.jar").get().asFile
        val outDir   = layout.buildDirectory.dir("installer").get().asFile
        outDir.mkdirs()
        val exitCode = ProcessBuilder(
            jpackage,
            "--type", "app-image",
            "--input", jarPath.parentFile.absolutePath,
            "--main-jar", jarPath.name,
            "--main-class", "MainKt",
            "--name", "KFD-Tracker",
            "--app-version", "${project.version}",
            "--dest", outDir.absolutePath,
        ).inheritIO().start().waitFor()
        if (exitCode != 0) error("jpackage failed with exit code $exitCode")
    }
}