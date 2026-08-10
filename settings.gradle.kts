rootProject.name = "kotlin-coroutine-stacktraces-demo"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        // Kotlin 2.4.20 is still EAP at the time of writing.
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/eap")
    }
}
