import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val vaadinVersion: String by extra
val karibuDslVersion: String by extra
val ktorVersion: String by extra

plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("application")
    id("com.vaadin")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

defaultTasks("clean", "build")

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://jitpack.io")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.21")

    implementation("com.vaadin:vaadin-core:$vaadinVersion") {
        afterEvaluate {
            if (vaadin.productionMode) {
                exclude(module = "vaadin-dev")
            }
        }
    }
    implementation("com.github.mvysny.karibudsl:karibu-dsl-v23:$karibuDslVersion")
    implementation("com.github.mvysny.vaadin-boot:vaadin-boot:12.1")
    implementation("org.parttio:line-awesome:2.0.0")
    //implementation("org.vaadin:spinkit:3.0.0")
    //implementation("org.vaadin.filesystemdataprovider:filesystemdataprovider:4.0.0")
    // https://mvnrepository.com/artifact/org.vaadin.filesystemdataprovider/filesystemdataprovider
    //implementation("org.vaadin.filesystemdataprovider:filesystemdataprovider:4.0.0")


    // https://mvnrepository.com/artifact/com.mysql/mysql-connector-j
    implementation("com.mysql:mysql-connector-j:8.2.0")
    implementation("org.slf4j:slf4j-simple:2.0.7")

    implementation("moe.styx:styx-db:0.2")
    implementation("net.peanuuutz.tomlkt:tomlkt:0.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-java:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-encoding:$ktorVersion")

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