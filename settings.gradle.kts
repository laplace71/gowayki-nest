rootProject.name = "Gowaykinesh"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                // Mapbox arrastra cronet de Chromium vía play-services-cronet.
                includeGroup("org.chromium.net")
            }
        }
        mavenCentral()
        // Mapbox SDK (requiere token de descarga: ~/.gradle/gradle.properties → MAPBOX_DOWNLOADS_TOKEN=sk.…).
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN").getOrElse("")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

include(":androidApp")
include(":shared")