package ai.rojan.designlab.data.local

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "rojan_token_encryption_key"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_IV_LENGTH_BYTES = 12
private const val GCM_TAG_LENGTH_BITS = 128

/**
 * AES-256-GCM encryption for the JWT access/refresh token pair, backed
 * directly by the Android Keystore — the key itself never leaves
 * hardware-backed/OS-protected storage; only ciphertext (IV-prefixed,
 * Base64-encoded) ever sits in [SharedPreferences]. Deliberately a thin
 * direct use of the platform `java.security`/`javax.crypto` APIs rather
 * than a third-party wrapper library — this codebase had no existing
 * secure-storage precedent to match, so there's no convention being
 * broken by keeping it minimal and dependency-free.
 */
internal object TokenCipher {

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + cipherBytes, Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String): String {
        val combined = Base64.decode(cipherText, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val cipherBytes = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        }
        return String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
    }
}

/**
 * Plain [SharedPreferences] file — every value written here goes through
 * [TokenCipher] first, so nothing unencrypted ever reaches disk.
 */
fun Context.createTokenPreferences(): SharedPreferences =
    applicationContext.getSharedPreferences("secure_token_preferences", Context.MODE_PRIVATE)

/** Preference keys used by [ai.rojan.designlab.data.repository.TokenRepositoryImpl]. */
internal object TokenPreferencesKeys {
    const val ACCESS_TOKEN = "access_token"
    const val REFRESH_TOKEN = "refresh_token"
}
