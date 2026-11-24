package com.hisaabwise.core.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.hisaabwise.core.model.AppLanguage
import com.hisaabwise.core.model.ContentResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Where the refresh token lives. */
interface TokenStore {
    suspend fun refreshToken(): String?
    suspend fun save(refreshToken: String)
    suspend fun clear()
}

/** "Keep me signed in" off: the token dies with the process. */
class InMemoryTokenStore : TokenStore {
    @Volatile private var token: String? = null
    override suspend fun refreshToken() = token
    override suspend fun save(refreshToken: String) { token = refreshToken }
    override suspend fun clear() { token = null }
}

/**
 * The Android twin of the iOS Keychain item (`AfterFirstUnlockThisDeviceOnly`): the refresh token is
 * encrypted with an AES-256-GCM key that never leaves the Android Keystore, and the app opts out of
 * backup, so the token cannot move to another device.
 */
class KeystoreTokenStore(context: Context) : TokenStore {
    private val prefs: SharedPreferences = context.getSharedPreferences("hw.session", Context.MODE_PRIVATE)

    override suspend fun refreshToken(): String? = withContext(Dispatchers.IO) {
        val stored = prefs.getString(KEY, null) ?: return@withContext null
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_LENGTH)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        cipher.doFinal(bytes.copyOfRange(IV_LENGTH, bytes.size)).decodeToString()
    }

    override suspend fun save(refreshToken: String) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val sealed = cipher.iv + cipher.doFinal(refreshToken.encodeToByteArray())
        prefs.edit().putString(KEY, Base64.encodeToString(sealed, Base64.NO_WRAP)).apply()
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit().remove(KEY).apply()
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "com.hisaabwise.session"
        const val KEY = "refreshToken"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
    }
}

/** Small device preferences: the two UserDefaults keys the iOS app keeps. */
class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("hw.prefs", Context.MODE_PRIVATE)

    var language: AppLanguage?
        get() = AppLanguage.fromTag(prefs.getString("hw.language", null))
        set(value) = prefs.edit().putString("hw.language", value?.tag).apply()

    var walkthroughSeen: Boolean
        get() = prefs.getBoolean("hw.walkthrough.seen", false)
        set(value) = prefs.edit().putBoolean("hw.walkthrough.seen", value).apply()
}

/** Cached content bytes and their ETags. */
interface ContentStore {
    suspend fun data(resource: ContentResource): ByteArray?
    suspend fun etag(resource: ContentResource): String?
    suspend fun save(resource: ContentResource, data: ByteArray, etag: String?)
    suspend fun remove(resource: ContentResource)
}

/** `cacheDir/HisaabWiseContent/<key>.json` + `<key>.etag`, like iOS `Caches/HisaabWiseContent`. */
class FileContentStore(context: Context) : ContentStore {
    private val dir = File(context.cacheDir, "HisaabWiseContent").apply { mkdirs() }
    private fun bytesFile(r: ContentResource) = File(dir, "${r.key}.json")
    private fun etagFile(r: ContentResource) = File(dir, "${r.key}.etag")

    override suspend fun data(resource: ContentResource) = withContext(Dispatchers.IO) {
        bytesFile(resource).takeIf { it.exists() }?.readBytes()
    }

    override suspend fun etag(resource: ContentResource) = withContext(Dispatchers.IO) {
        etagFile(resource).takeIf { it.exists() }?.readText()
    }

    override suspend fun save(resource: ContentResource, data: ByteArray, etag: String?) = withContext(Dispatchers.IO) {
        atomicWrite(bytesFile(resource), data)
        if (etag != null) atomicWrite(etagFile(resource), etag.encodeToByteArray()) else etagFile(resource).delete()
        Unit
    }

    override suspend fun remove(resource: ContentResource) = withContext(Dispatchers.IO) {
        bytesFile(resource).delete()
        etagFile(resource).delete()
        Unit
    }

    private fun atomicWrite(target: File, bytes: ByteArray) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeBytes(bytes)
        tmp.renameTo(target)
    }
}
