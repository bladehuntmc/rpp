plugins {
    `java-gradle-plugin`

    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    val greeting by plugins.creating {
        id = "net.bladehunt.resourcepack"
        implementationClass = "net.bladehunt.resourcepack.ResourcePackPlugin"
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
