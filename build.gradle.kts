import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.vaadin)
    application
}

defaultTasks("clean", "build")

repositories {
    mavenCentral()
    maven("https://repo.styx.moe/releases")
    maven("https://repo.styx.moe/snapshots")
    maven("https://jitpack.io")
    maven("https://maven.vaadin.com/vaadin-addons")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
    }
}

dependencies {
    // Kotlin Stuff
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    // Vaadin Stuff
    implementation(libs.vaadin.core) {
        if (vaadin.effective.productionMode.get()) {
            exclude(module = "vaadin-dev")
        }
    }
    implementation(libs.vaadin.boot)
    implementation(libs.vaadin.filesystemdataprovider)
    implementation(libs.karibu.dsl)
    implementation(libs.lineawesome)

    // Misc
    implementation(libs.postgres)
    implementation(libs.slf4j.simple)
    implementation(libs.jsoup)

    // Custom
    implementation(libs.styx.db)
    implementation(libs.styx.downloader)
    implementation(libs.anitomyj)

    // Image Processing
    implementation(libs.scrimage.core)
    implementation(libs.scrimage.webp)
    implementation(libs.scrimage.extra)

    // test support
    testImplementation(libs.karibu.testing)
    testImplementation(libs.dynatest)
}

kotlin {
    jvmToolchain(21)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

application {
    mainClass = "moe.styx.web.MainKt"
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get(),
            "Class-Path" to configurations.runtimeClasspath.get().joinToString(" ") { it.name }
        )
        duplicatesStrategy = DuplicatesStrategy.INHERIT
    }
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
}

tasks.register("shadow-ci") {
    dependsOn("shadowJar")
    doLast {
        val buildDir = File(projectDir, "build")
        buildDir.walk().find { it.extension == "jar" && it.name.contains("-all") }?.copyTo(File(projectDir, "app.jar"))
    }
}