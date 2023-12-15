import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

plugins {
    kotlin("jvm") version DependencyVersions.Kotlin.VERSION
    `maven-publish`
}

group = JarMetadata.GROUP
version = JarMetadata.VERSION

kotlin {
    jvmToolchain {
        kotlin {
            jvmToolchain {
                languageVersion.set(JavaLanguageVersion.of(DependencyVersions.Java.VERSION))
            }
        }
    }
}

repositories {
    mavenCentral()
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kreact/kreact")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${DependencyVersions.Kotlin.VERSION}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${DependencyVersions.Kotlin.COROUTINES_VERSION}")
    testImplementation("io.mockk:mockk:${DependencyVersions.MockK.VERSION}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${DependencyVersions.JUnit.VERSION}")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:${DependencyVersions.JUnit.VERSION}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${DependencyVersions.Kotlin.COROUTINES_VERSION}")
}