package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.local.TokenCipher
import ai.rojan.designlab.data.local.TokenPreferencesKeys
import ai.rojan.designlab.domain.repository.TokenRepository
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * [TokenRepository] backed by [SharedPreferences] created via
 * [ai.rojan.designlab.data.local.createTokenPreferences] — every value is
 * encrypted via [TokenCipher] before being written and decrypted after
 * being read, so nothing unencrypted ever reaches disk.
 */
class TokenRepositoryImpl(
    private val preferences: SharedPreferences,
) : TokenRepository {

    override fun saveTokens(accessToken: String, refreshToken: String) {
        preferences.edit {
            putString(TokenPreferencesKeys.ACCESS_TOKEN, TokenCipher.encrypt(accessToken))
            putString(TokenPreferencesKeys.REFRESH_TOKEN, TokenCipher.encrypt(refreshToken))
        }
    }

    override fun clearTokens() {
        preferences.edit {
            remove(TokenPreferencesKeys.ACCESS_TOKEN)
            remove(TokenPreferencesKeys.REFRESH_TOKEN)
        }
    }

    override fun accessToken(): String? = decryptStored(TokenPreferencesKeys.ACCESS_TOKEN)

    override fun refreshToken(): String? = decryptStored(TokenPreferencesKeys.REFRESH_TOKEN)

    private fun decryptStored(key: String): String? =
        preferences.getString(key, null)?.let { runCatching { TokenCipher.decrypt(it) }.getOrNull() }
}
