import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.javafx)
    alias(libs.plugins.maven.publish)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
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

mavenPublishing {
    configure(KotlinJvm(javadocJar = JavadocJar.Empty(), sourcesJar = true))
    coordinates(artifactId = "log-level-manager")
    publishToMavenCentral(automaticRelease = true)
    // Signing keys come from ORG_GRADLE_PROJECT_signingInMemoryKey* in CI; local builds stay unsigned.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }

    pom {
        name.set("Runtime Log Level Manager")
        description.set("Drop-in JavaFX UI to change logger levels at runtime (Logback, Log4j2, java.util.logging).")
        inceptionYear.set("2026")
        url.set("https://github.com/theaob/runtime-log-level-manager")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("theaob")
                name.set("Onur Baykal")
                url.set("https://github.com/theaob")
            }
        }
        scm {
            url.set("https://github.com/theaob/runtime-log-level-manager")
            connection.set("scm:git:git://github.com/theaob/runtime-log-level-manager.git")
            developerConnection.set("scm:git:ssh://git@github.com/theaob/runtime-log-level-manager.git")
        }
    }
}
