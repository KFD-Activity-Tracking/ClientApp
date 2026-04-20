plugins {
    kotlin("jvm") version "2.3.10"
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
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}