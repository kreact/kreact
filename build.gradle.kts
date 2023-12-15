import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version DependencyVersions.Kotlin.VERSION
}

group = JarMetadata.GROUP
version = JarMetadata.VERSION

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = DependencyVersions.Java.VERSION
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${DependencyVersions.Kotlin.VERSION}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${DependencyVersions.Kotlin.COROUTINES_VERSION}")
    testImplementation("io.mockk:mockk:${DependencyVersions.MockK.VERSION}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${DependencyVersions.JUnit.VERSION}")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:${DependencyVersions.JUnit.VERSION}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${DependencyVersions.Kotlin.COROUTINES_VERSION}")
}