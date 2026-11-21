package com.gowayki.nesh.core.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.gowayki.nesh.app.features.sos.domain.model.AndroidSosContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

/**
 * Token de sesión cifrado con Android Keystore.
 *
 * Los SharedPreferences normales son legibles por cualquier proceso con
 * acceso a los datos de la app (o a un backup). Aquí la clave nunca sale del
 * Keystore y en preferences solo queda el ciphertext + IV en Base64.
 */
actual object AuthTokenStore {
    private const val FILE = "wayki_session"
    private const val KEY = "auth_token"
    private const val ALIAS = "wayki_auth_token_key"

    actual fun read(): String? = runCatching {
        val context = AndroidSosContext.appContext ?: return null
        val guardado = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val datos = Base64.decode(guardado, Base64.DEFAULT)
        val iv = datos.copyOfRange(0, 12)
        val cipherText = datos.copyOfRange(12, datos.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(cipherText)).takeIf { it.isNotBlank() }
    }.getOrNull()

    actual fun write(token: String?) {
        runCatching {
            val context = AndroidSosContext.appContext ?: return
            val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            if (token.isNullOrBlank()) {
                prefs.edit().remove(KEY).apply()
                return
            }

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, clave())
            val cipherText = cipher.doFinal(token.toByteArray())
            val bytes = cipher.iv + cipherText
            prefs.edit().putString(KEY, Base64.encodeToString(bytes, Base64.DEFAULT)).apply()
        }
    }

    private fun clave(): java.security.Key {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existente = keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existente != null) return existente.secretKey

        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply { init(spec) }
            .generateKey()
    }
}
