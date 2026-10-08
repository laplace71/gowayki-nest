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
    inputs.file(envFile)
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

        val missing = listOf("SUPABASE_URL", "SUPABASE_ANON_KEY", "GOOGLE_WEB_CLIENT_ID")
            .filter { envMap[it].isNullOrBlank() }
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Faltan variables en el .env de la raíz: ${missing.joinToString(", ")}. " +
                    "Copia .env.example a .env y rellena los valores."
            )
        }

        val supabaseUrl = envMap.getValue("SUPABASE_URL").trim()
        val supabaseAnonKey = envMap.getValue("SUPABASE_ANON_KEY").trim()
        val googleWebClientId = envMap.getValue("GOOGLE_WEB_CLIENT_ID").trim()

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
            |    const val SUPABASE_URL: String = "$supabaseUrl"
            |    const val SUPABASE_ANON_KEY: String = "$supabaseAnonKey"
            |
            |    // Google OAuth Web Client ID (ComposeAuth / Credential Manager)
            |    const val GOOGLE_WEB_CLIENT_ID: String = "$googleWebClientId"
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
