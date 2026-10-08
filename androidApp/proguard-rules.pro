# Reglas de Wayki Nest (release con minify + shrink).

# --- kotlinx.serialization: los serializers generados se buscan por nombre ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclasseswithmembernames class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.gowayki.nesh.**$$serializer { *; }
-keepclassmembers class com.gowayki.nesh.** {
    *** Companion;
}
-keepclasseswithmembers class com.gowayki.nesh.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Ktor client (OkHttp/CIO usan reflexión y ServiceLoader) ---
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class io.ktor.client.engine.** { *; }

# --- Supabase SDK ---
-keep class io.github.jan-tennert.** { *; }
-dontwarn io.github.jan-tennert.**

# --- Compose Multiplatform resources (acceso por nombre generado) ---
-keep class gowaykinesh.shared.generated.resources.** { *; }