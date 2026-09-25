plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.javafx) apply false
    alias(libs.plugins.maven.publish) apply false
}

allprojects {
    group = "tr.com.onurbaykal"
    // Release builds pass -PreleaseVersion=x.y.z (see .github/workflows/release.yml)
    version = providers.gradleProperty("releaseVersion").getOrElse("0.1.0-SNAPSHOT")
}
