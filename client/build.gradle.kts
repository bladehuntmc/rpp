plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.2"
}

dependencies {
    compileOnly("org.jetbrains:annotations:25.0.0")
    implementation("cloud.prefab:sse-handler:1.0.1")
}

version = "dev"

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            artifactId = "rpp"
        }
    }
    repositories {
        maven {
            name = "releases"
            url = uri("https://mvn.bladehunt.net/releases")
            credentials(PasswordCredentials::class) {
                username = System.getenv("MAVEN_NAME")
                password = System.getenv("MAVEN_SECRET")
            }
            authentication { create<BasicAuthentication>("basic") }
        }
    }
}