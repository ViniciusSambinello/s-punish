dependencies {
    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.minimessage)

    implementation(libs.hikaricp)
    implementation(libs.mysql.connector)
    implementation(libs.configurate.core)
    implementation(libs.configurate.yaml)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
    testCompileOnly(libs.adventure.api)
    testCompileOnly(libs.adventure.minimessage)

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.mysql)
}
