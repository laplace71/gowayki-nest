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
            implementation(libs.androidx.core.ktx)
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

val generateAppConfigTask = tasks.register("generateAppConfig") {
    val envFile = rootProject.file(".env")
    val configFile = file("src/commonMain/kotlin/com/gowayki/nesh/core/config/AppConfig.kt")
    if (envFile.exists()) inputs.file(envFile)
    outputs.file(configFile)

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

        // Si no existe .env o faltan variables, usar valores por defecto del AppConfig.kt actual
        val existingConfig = if (configFile.exists()) configFile.readText() else ""
        
        fun extractValue(key: String): String {
            val pattern = Regex("""const val """ + key + """\s*:\s*String\s*=\s*"([^"]+)""")
            return pattern.find(existingConfig)?.groupValues?.get(1) ?: ""
        }
        
        val supabaseUrl = envMap.getOrElse("SUPABASE_URL") { extractValue("SUPABASE_URL") }
        val supabaseAnonKey = envMap.getOrElse("SUPABASE_ANON_KEY") { extractValue("SUPABASE_ANON_KEY") }
        val googleWebClientId = envMap.getOrElse("GOOGLE_WEB_CLIENT_ID") { extractValue("GOOGLE_WEB_CLIENT_ID") }

        // Si aún faltan, usar defaults hardcodeados
        val finalSupabaseUrl = supabaseUrl.ifBlank { "https://bvxgdlawivdoiornfigo.supabase.co" }
        val finalSupabaseAnonKey = supabaseAnonKey.ifBlank { "sb_publishable_-_Vc3gCY-q48pGHTjmFaog_zYFLbf8c" }
        val finalGoogleWebClientId = googleWebClientId.ifBlank { "21304726166-16dakb4a1qfjl6d4qpsifnlr3m1q4sl3.apps.googleusercontent.com" }

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
