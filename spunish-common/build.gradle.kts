// java-library (rather than plain java, applied by root convention) so
// `api()` can expose types like Configurate's ConfigurationNode, which
// leaks through common's own public methods (YamlConfigLoader.loadNode(),
// MessageService's constructor) and is used directly by spunish-paper.
plugins {
    `java-library`
}

// Testcontainers-backed tests get their own source set/task, kept out of
// `test`/`build` on purpose: they need Docker, which a plain dev machine or
// this repo's default local checks should never have to assume. CI (section 13)
// runs both `test` and `integrationTest` explicitly.
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
    description = "Runs Testcontainers-backed integration tests. Requires Docker."
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks.test)
}

dependencies {
    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.minimessage)

    implementation(libs.hikaricp)
    implementation(libs.mysql.connector)
    api(libs.configurate.core)
    implementation(libs.configurate.yaml)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
    // Adventure is compileOnly for main (Paper/Velocity provide it at runtime),
    // but tests run standalone and need it on the runtime classpath too.
    testImplementation(libs.adventure.api)
    testImplementation(libs.adventure.minimessage)

    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)

    integrationTestImplementation(platform(libs.junit.bom))
    integrationTestImplementation(libs.junit.jupiter)
    integrationTestImplementation(libs.assertj.core)
    integrationTestImplementation(platform(libs.testcontainers.bom))
    integrationTestImplementation(libs.testcontainers.junit.jupiter)
    integrationTestImplementation(libs.testcontainers.mysql)
}
