import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val vaadinVersion: String by extra
val karibuDslVersion: String by extra
val ktorVersion: String by extra
val scrimageVersion: String by extra

plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    id("application")
    id("com.vaadin")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

defaultTasks("clean", "build")

repositories {
    mavenCentral()
    maven("https://repo.styx.moe/releases")
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
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.23")

    // Vaadin Stuff
    implementation("com.vaadin:vaadin-core:$vaadinVersion") {
        afterEvaluate {
            if (vaadin.productionMode.get()) {
                exclude(module = "vaadin-dev")
            }
        }
    }
    implementation("com.github.mvysny.karibudsl:karibu-dsl-v23:$karibuDslVersion")
    //implementation("eu.vaadinonkotlin:vok-util-vaadin:0.16.0")
    //implementation("in.virit:viritin:2.3.1")
    implementation("com.github.mvysny.vaadin-boot:vaadin-boot:12.2")
    implementation("org.parttio:line-awesome:2.0.0")
    implementation("org.vaadin.filesystemdataprovider:filesystemdataprovider:4.0.0")

    // Misc
    implementation("com.mysql:mysql-connector-j:8.3.0")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("net.peanuuutz.tomlkt:tomlkt:0.3.7")
    implementation("org.jsoup:jsoup:1.17.2")

    // Custom
    implementation("moe.styx:styx-db:0.0.8")
    implementation("moe.styx:styx-downloader:0.0.5")

    // Image Processing
    implementation("com.sksamuel.scrimage:scrimage-core:$scrimageVersion")
    implementation("com.sksamuel.scrimage:scrimage-webp:$scrimageVersion")
    implementation("com.sksamuel.scrimage:scrimage-formats-extra:$scrimageVersion")

    // test support
    testImplementation("com.github.mvysny.kaributesting:karibu-testing-v24:2.1.0")
    testImplementation("com.github.mvysny.dynatest:dynatest:0.24")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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