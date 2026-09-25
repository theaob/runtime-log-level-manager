import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.javafx)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

javafx {
    version = "21.0.5"
    modules = listOf("javafx.controls")
}

dependencies {
    implementation(project(":log-level-manager"))
    implementation(libs.logback.classic)
}

application {
    mainClass.set("tr.com.onurbaykal.loglevel.sample.SampleAppKt")
}
