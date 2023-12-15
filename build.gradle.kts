import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version DependencyVersions.Kotlin.version
}

group = JarMetadata.group
version = JarMetadata.version

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = DependencyVersions.Java.version
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${DependencyVersions.Kotlin.version}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${DependencyVersions.Kotlin.coroutinesVersion}")
    testImplementation("io.mockk:mockk:${DependencyVersions.MockK.version}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${DependencyVersions.JUnit.version}")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:${DependencyVersions.JUnit.version}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${DependencyVersions.Kotlin.coroutinesVersion}")
}