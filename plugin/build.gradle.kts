import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradle.plugin-publish") version "1.3.0"
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
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
    archiveClassifier = ""
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
