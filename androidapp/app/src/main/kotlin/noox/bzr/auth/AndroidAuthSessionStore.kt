package noox.bzr.auth

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidAuthSessionStore(private val preferences: SharedPreferences) : AuthSessionStore {
    override var token: String?
        get() {
            val encrypted = preferences.getString("auth_token", null) ?: return null
            val iv = preferences.getString("auth_token_iv", null) ?: return null
            return runCatching {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH, Base64.decode(iv, Base64.NO_WRAP)))
                cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)).toString(Charsets.UTF_8)
            }.getOrNull()
        }
        set(value) {
            if (value == null) {
                preferences.edit().remove("auth_token").remove("auth_token_iv").apply()
                return
            }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            preferences.edit()
                .putString("auth_token", Base64.encodeToString(cipher.doFinal(value.toByteArray()), Base64.NO_WRAP))
                .putString("auth_token_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .apply()
        }

    override var email: String?
        get() = preferences.getString("auth_email", null)
        set(value) {
            preferences.edit().putString("auth_email", value).apply()
        }

    override fun clear() {
        preferences.edit().remove("auth_token").remove("auth_token_iv").remove("auth_email").apply()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "bzr_auth_token"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH = 128
    }
}
