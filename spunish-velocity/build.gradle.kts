plugins {
    alias(libs.plugins.shadow)
}

tasks.shadowJar {
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
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

dependencies {
    implementation(project(":spunish-common"))
    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
}
