plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

dependencies {
    implementation(libs.kotlinx.serialization.json.jvm)
}

application {
    mainClass.set("com.gowayki.nesh.sos.server.MainKt")
}

kotlin {
    jvmToolchain(17)
}
