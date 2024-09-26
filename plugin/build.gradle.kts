import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradle.plugin-publish") version "1.3.0"
    alias(libs.plugins.kotlin.jvm)
    id("com.gradleup.shadow") version "8.3.2"
}

group = "net.bladehunt"
version = "0.1.0-alpha.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.javalin:javalin:6.3.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.named<ShadowJar>("shadowJar") {
    this.archiveClassifier = ""
}

gradlePlugin {
    website = "https://github.com/bladehuntmc/rpp"
    vcsUrl = "https://github.com/bladehuntmc/rpp"

    val greeting by plugins.creating {
        id = "net.bladehunt.rpp"
        displayName = "Resource Pack Processor"
        description = "Plugin to assist with developing resource packs for Minecraft"
        tags = listOf("minecraft", "minestom", "resource pack")

        implementationClass = "net.bladehunt.rpp.RppPlugin"
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
