plugins {
    alias(libs.plugins.shadow)
}

tasks.processResources {
    filesMatching("paper-plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    // Bundled dependencies are relocated so another plugin on the same server
    // bundling a different version of the same library can never collide with ours.
    relocate("com.zaxxer.hikari", "com.spunish.libs.hikari")
    relocate("com.mysql", "com.spunish.libs.mysql")
    relocate("com.google.protobuf", "com.spunish.libs.protobuf")
    relocate("org.spongepowered.configurate", "com.spunish.libs.configurate")
    relocate("io.leangen.geantyref", "com.spunish.libs.geantyref")
    relocate("net.kyori.option", "com.spunish.libs.option")
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

// The smoke test loads the shaded jar in an isolated classloader and opens a
// real connection through it, so it needs the jar to exist and gets its own
// Testcontainers-backed source set/task for the same reason spunish-common's
// integration tests do: it needs Docker, which local checks should never assume.
sourceSets {
    create("integrationTest") {
        java.srcDir("src/integrationTest/java")
        resources.srcDir("src/integrationTest/resources")
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += output + compileClasspath
    }
}

val integrationTestImplementation = configurations.getByName("integrationTestImplementation") {
    extendsFrom(configurations.testImplementation.get())
}
configurations.getByName("integrationTestRuntimeOnly") {
    extendsFrom(configurations.testRuntimeOnly.get())
}

tasks.register<Test>("integrationTest") {
    description = "Runs Testcontainers-backed integration tests, including the shaded-jar smoke test. Requires Docker."
    group = "verification"
    dependsOn(tasks.shadowJar)
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks.test)
    systemProperty("spunish.shadedJar", tasks.shadowJar.get().archiveFile.get().asFile.absolutePath)
}

dependencies {
    implementation(project(":spunish-common"))
    compileOnly(libs.paper.api)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)

    integrationTestImplementation(platform(libs.junit.bom))
    integrationTestImplementation(libs.junit.jupiter)
    integrationTestImplementation(libs.assertj.core)
    integrationTestImplementation(platform(libs.testcontainers.bom))
    integrationTestImplementation(libs.testcontainers.junit.jupiter)
    integrationTestImplementation(libs.testcontainers.mysql)
}
