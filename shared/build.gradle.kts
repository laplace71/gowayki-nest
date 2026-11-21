import org.gradle.api.GradleException
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    android {
       namespace = "com.gowayki.nesh.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.ktor.client.android)
            // Mapbox SOS (heatmap nativo + pins). Solo Android; iOS usa fallback OSM.
            implementation(libs.mapbox.android)
            implementation(libs.mapbox.compose)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.websockets)
            implementation("io.ktor:ktor-network:3.2.3")
            implementation("io.ktor:ktor-io:3.2.3")
            implementation(libs.compose.icons.feather)
            implementation(libs.compose.icons.fontawesome)
            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.auth)
            implementation(libs.supabase.compose.auth)
            implementation(libs.supabase.postgrest)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

apply(from = rootProject.file("gradle/convertSvgIcons.gradle"))

// Todas las claves de configuración que se inyectan desde fuera del repo:
// GitHub Secrets -> variables de entorno en CI; shell/.env en local.
// Se declaran como providers (y como inputs del task) para que un cambio en
// cualquiera de ellas invalide el AppConfig generado, aunque no cambie el
// .env ni el código fuente. Antes solo se declaraba el .env como input y un
// BACKEND_URL distinto entre dos builds podía dejar el APK anterior embebido.
val appConfigKeys = listOf(
    "BACKEND_URL",
    "SUPABASE_URL",
    "SUPABASE_ANON_KEY",
    "GOOGLE_WEB_CLIENT_ID",
)

val generateAppConfigTask = tasks.register("generateAppConfig") {
    val envFile = rootProject.file(".env")
    val configFile = file("src/commonMain/kotlin/com/gowayki/nesh/core/config/AppConfig.kt")
    if (envFile.exists()) inputs.file(envFile)
    outputs.file(configFile)

    // Providers creados en configuración y capturados como valores locales:
    // el doLast no debe referenciar el objeto script (configuration cache).
    val propertyNames = appConfigKeys.associateWith { "wayki." + it.lowercase().replace('_', '.') }
    val envProviders = appConfigKeys.associateWith { providers.environmentVariable(it).orElse("") }
    val propProviders = appConfigKeys.associateWith {
        providers.gradleProperty(propertyNames.getValue(it)).orElse("")
    }

    // (audit) Registrar TODAS las variables -no solo el .env- como inputs.
    appConfigKeys.forEach { key ->
        inputs.property("env.$key", envProviders.getValue(key))
        inputs.property("prop.$key", propProviders.getValue(key))
    }

    doLast {
        val envMap = mutableMapOf<String, String>()
        if (envFile.exists()) {
            envFile.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                    val parts = trimmed.split("=", limit = 2)
                    envMap[parts[0].trim()] = parts[1].trim()
                }
            }
        }

        // Misma fuente declarada como input arriba: el valor que se comprueba
        // es exactamente el que se escribe en el AppConfig generado.
        fun envOr(key: String): String {
            envProviders.getValue(key).get()
                .takeIf { it.isNotBlank() }?.let { return it }
            propProviders.getValue(key).get()
                .takeIf { it.isNotBlank() }?.let { return it }
            return envMap[key] ?: ""
        }

        // Si no existe .env o faltan variables, usar valores del AppConfig.kt actual
        val existingConfig = if (configFile.exists()) configFile.readText() else ""

        fun extractValue(key: String): String {
            val pattern = Regex("""const val """ + key + """\s*:\s*String\s*=\s*"([^"]+)"""")
            return pattern.find(existingConfig)?.groupValues?.get(1) ?: ""
        }

        val supabaseUrl = envOr("SUPABASE_URL").ifBlank { extractValue("SUPABASE_URL") }
        val supabaseAnonKey = envOr("SUPABASE_ANON_KEY").ifBlank { extractValue("SUPABASE_ANON_KEY") }
        val googleWebClientId = envOr("GOOGLE_WEB_CLIENT_ID").ifBlank { extractValue("GOOGLE_WEB_CLIENT_ID") }
        val backendUrl = envOr("BACKEND_URL").ifBlank { extractValue("BACKEND_URL") }

        // Si aún faltan, usar defaults hardcodeados
        val finalSupabaseUrl = supabaseUrl.ifBlank { "https://bvxgdlawivdoiornfigo.supabase.co" }
        val finalSupabaseAnonKey = supabaseAnonKey.ifBlank { "sb_publishable_-_Vc3gCY-q48pGHTjmFaog_zYFLbf8c" }
        val finalGoogleWebClientId = googleWebClientId.ifBlank { "21304726166-16dakb4a1qfjl6d4qpsifnlr3m1q4sl3.apps.googleusercontent.com" }
        // (audit CWE-319) Antes este default era http:// y el APK publicado
        // enviaba DNI y PIN en texto plano. El backend debe terminar TLS.
        val finalBackendUrl = backendUrl.ifBlank { "https://31.220.72.2/api" }

        // Nunca se embebe un backend http:// en el APK: el login y el registro
        // envían el DNI y el PIN (CWE-319). Solo se tolera localhost para
        // desarrollo contra un servidor en la propia máquina.
        fun requireHttps(value: String, key: String) {
            if (!value.startsWith("http://")) return
            val isLocal = value.startsWith("http://localhost") ||
                value.startsWith("http://127.0.0.1") ||
                value.startsWith("http://[::1]")
            if (isLocal) return
            throw GradleException(
                "$key=$value usa http://. El inicio de sesión envía DNI y PIN en " +
                    "texto plano; configura $key (o ${propertyNames.getValue(key)}) con una URL https://. " +
                    "Solo se permite http:// para localhost/127.0.0.1 en desarrollo."
            )
        }
        requireHttps(finalBackendUrl, "BACKEND_URL")
        requireHttps(finalSupabaseUrl, "SUPABASE_URL")

        val content = """
            |package com.gowayki.nesh.core.config
            |
            |/**
            | * Configuración generada automáticamente desde .env por Gradle.
            | * NO editar directamente este archivo. Edita el .env en la raíz del proyecto.
            | */
            |object AppConfig {
            |    const val APP_TITLE: String = "Gowayki Nesh"
            |    const val SHOW_DEBUG_BANNER: Boolean = false
            |
            |    // Backend Spring Boot API (inyectado desde .env)
            |    const val BACKEND_URL: String = "$finalBackendUrl"
            |
            |    // Supabase (inyectado desde .env)
            |    const val SUPABASE_URL: String = "$finalSupabaseUrl"
            |    const val SUPABASE_ANON_KEY: String = "$finalSupabaseAnonKey"
            |
            |    // Google OAuth Web Client ID (ComposeAuth / Credential Manager)
            |    const val GOOGLE_WEB_CLIENT_ID: String = "$finalGoogleWebClientId"
            |
            |    // Deep link OAuth (Auth scheme/host)
            |    const val AUTH_SCHEME: String = "com.gowayki.nesh"
            |    const val AUTH_HOST: String = "login-callback"
            |}
        """.trimMargin()

        configFile.parentFile.mkdirs()
        configFile.writeText(content)
    }
}

tasks.matching { it.name.startsWith("compileKotlin") || it.name.startsWith("prepareCompose") }.configureEach {
    dependsOn(generateAppConfigTask)
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
