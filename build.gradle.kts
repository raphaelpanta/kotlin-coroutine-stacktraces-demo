plugins {
    kotlin("jvm") version "2.4.20-Beta2"
    application
}

repositories {
    mavenCentral()
    // StackTraceRecoverable ships in the 2.4.20 standard library, still EAP here.
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/eap")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.add("kotlin.coroutines.ExperimentalStdlibCoroutineSupportApi")
    }
}

application {
    mainClass.set("com.pantaleontech.stacktraces.DemoKt")
}

// ---------------------------------------------------------------------------
// Debug mode is read ONCE, when kotlinx.coroutines initialises. You cannot flip
// it inside a running JVM — which is why the demo and the tests are split into
// separate tasks with separate JVMs.
// ---------------------------------------------------------------------------

tasks.named<JavaExec>("run") {
    // Explicit "off" so the default (auto -> follows -ea) can't surprise us.
    systemProperty("kotlinx.coroutines.debug", "off")
}

val runWithDebug by tasks.registering(JavaExec::class) {
    group = "demo"
    description = "Runs the demo with coroutine debug mode ON (recovery active)."
    mainClass.set("com.pantaleontech.stacktraces.DemoKt")
    classpath = sourceSets["main"].runtimeClasspath
    systemProperty("kotlinx.coroutines.debug", "on")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    // Note: Gradle sets enableAssertions = true by default, which would make
    // coroutine debug mode "auto" resolve to ON. We are explicit either way.
    systemProperty("kotlinx.coroutines.debug", "on")
    filter { excludeTestsMatching("*RecoveryDisabledTest") }
    testLogging { showStandardStreams = true }
}

val testDebugOff by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs the recovery-disabled test in a JVM with debug mode OFF."
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform()
    systemProperty("kotlinx.coroutines.debug", "off")
    filter { includeTestsMatching("*RecoveryDisabledTest") }
    testLogging { showStandardStreams = true }
}

tasks.named("check") { dependsOn(testDebugOff) }
