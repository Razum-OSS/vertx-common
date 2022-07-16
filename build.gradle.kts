group = "si.razum"
version = "1.0.1-SNAPSHOT"

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.7.10"
	`maven-publish`
    `java-library`
    `java-test-fixtures` // Allows us to expose testing classes to consumer libraries
}

// Include sources in the JAR for easier debugging
java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

configurations {
    // Make the compileOnly classpath available in tests as well
    testFixturesImplementation.get().extendsFrom(compileOnly.get())
    testImplementation.get().extendsFrom(compileOnly.get())
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}


dependencies {
    val vertxVersion = "4.0.2" // should be lowest common version of projects

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    // Dependencies that will be found on the target system, possibly with a different version!
    compileOnly("io.vertx:vertx-core:${vertxVersion}")
    compileOnly("io.vertx:vertx-web:${vertxVersion}")
    compileOnly("io.vertx:vertx-lang-kotlin:${vertxVersion}")
    compileOnly("io.vertx:vertx-lang-kotlin-coroutines:${vertxVersion}")
    compileOnly("io.vertx:vertx-health-check:${vertxVersion}")
    compileOnly("org.jooq:jooq:3.17.2")
    compileOnly("com.fasterxml.jackson.core:jackson-databind:2.11.4")
    compileOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.1")
    compileOnly("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
    compileOnly("com.zaxxer:HikariCP:3.4.5")
    compileOnly("org.flywaydb:flyway-core:7.5.2")
    compileOnly("io.vertx:vertx-config:${vertxVersion}")
    compileOnly("io.vertx:vertx-config-hocon:${vertxVersion}")
    compileOnly("org.apache.commons:commons-lang3:3.11")
    compileOnly("org.apache.commons:commons-text:1.9")
    testFixturesImplementation("org.postgresql:postgresql:42.2.16")
    testFixturesImplementation("org.testcontainers:testcontainers:1.15.3")
    testFixturesImplementation("org.testcontainers:postgresql:1.15.2")
}
