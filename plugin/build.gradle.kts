plugins {
    `java-gradle-plugin`

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

gradlePlugin {
    val greeting by plugins.creating {
        id = "net.bladehunt.rpp"
        implementationClass = "net.bladehunt.rpp.RppPlugin"
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
