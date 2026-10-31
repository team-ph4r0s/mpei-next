package kekmech.ru.feature_bars_impl.data.network

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kekmech.ru.lib_bars_client.BarsSessionCodec
import kekmech.ru.lib_bars_client.BarsSessionStore
import kekmech.ru.lib_bars_client.BarsStoredSession
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps the BARS session (the cookies are bearer secrets) in a private file of the app, encrypted with a key that never
 * leaves the Android Keystore. A session that cannot be read (the key is gone, the data is damaged) is no session:
 * the student signs in again.
 */
internal class KeystoreBarsSessionStore(context: Context) : BarsSessionStore {

    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override suspend fun load(): BarsStoredSession? = loadNow()

    override suspend fun save(session: BarsStoredSession) {
        val saved = runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
            val encrypted = cipher.doFinal(BarsSessionCodec.encode(session).toByteArray(Charsets.UTF_8))
            val value = Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
            preferences.edit().putString(KEY_SESSION, value).commit()
        }.getOrDefault(false)
        if (!saved) clearNow()
    }

    override suspend fun clear() = clearNow()

    /** For the code that cannot suspend (the logout from a screen): the session is gone when it returns. */
    fun clearNow() {
        preferences.edit().remove(KEY_SESSION).commit()
    }

    private fun loadNow(): BarsStoredSession? = runCatching {
        val value = preferences.getString(KEY_SESSION, null) ?: return@runCatching null
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
        }
        val text = String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
        BarsSessionCodec.decode(text)
    }.getOrNull()

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply {
                init(
                    KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(KEY_BITS)
                        .build(),
                )
            }
            .generateKey()
    }

    private companion object {
        const val PREFERENCES = "bars_session_v2"
        const val KEY_SESSION = "session"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "mpeix_bars_session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BITS = 256
        const val TAG_BITS = 128
        const val IV_BYTES = 12
    }
}
