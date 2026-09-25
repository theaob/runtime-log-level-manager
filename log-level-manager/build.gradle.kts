import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.javafx)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

// JavaFX is provided by the host application, so it is not exposed as a dependency.
javafx {
    version = "17.0.13"
    modules = listOf("javafx.controls")
    configurations = arrayOf("compileOnly", "testImplementation")
}

dependencies {
    // Logging backends are detected at runtime; the host application brings the one it uses.
    compileOnly(libs.slf4j.api)
    compileOnly(libs.logback.classic)
    compileOnly(libs.log4j.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
    testImplementation(libs.slf4j.api)
    testImplementation(libs.logback.classic)
    testImplementation(libs.log4j.core)
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "tr.com.onurbaykal.loglevel")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "log-level-manager"
            from(components["java"])
            pom {
                name.set("Runtime Log Level Manager")
                description.set("Drop-in JavaFX UI to change logger levels at runtime (Logback, Log4j2, java.util.logging).")
                url.set("https://github.com/theaob/runtime-log-level-manager")
            }
        }
    }
}
