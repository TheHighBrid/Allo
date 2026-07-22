package com.kenza.callsim.memory

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores Kenza's private memory as AES-GCM encrypted JSON in the app's private
 * files directory. The encryption key is generated inside Android Keystore and
 * is never written to disk or committed to the repository.
 */
internal class SecureMemoryStorage(context: Context) {

    private val appContext = context.applicationContext
    private val memoryFile = File(appContext.filesDir, FILE_NAME)
    private val tempFile = File(appContext.filesDir, "$FILE_NAME.tmp")

    @Synchronized
    fun read(): String? {
        if (!memoryFile.exists()) return null
        return runCatching {
            val payload = JSONObject(memoryFile.readText(Charsets.UTF_8))
            val iv = Base64.decode(payload.getString("iv"), Base64.NO_WRAP)
            val encrypted = Base64.decode(payload.getString("data"), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.onFailure {
            Log.e(TAG, "Could not decrypt memory store", it)
            preserveUnreadableFile()
        }.getOrNull()
    }

    @Synchronized
    fun write(plainText: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val payload = JSONObject()
            .put("version", 1)
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .toString()

        tempFile.writeText(payload, Charsets.UTF_8)
        if (!tempFile.renameTo(memoryFile)) {
            tempFile.copyTo(memoryFile, overwrite = true)
            tempFile.delete()
        }
    }

    @Synchronized
    fun clear() {
        memoryFile.delete()
        tempFile.delete()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun preserveUnreadableFile() {
        if (!memoryFile.exists()) return
        val backup = File(appContext.filesDir, "$FILE_NAME.unreadable.${System.currentTimeMillis()}")
        runCatching { memoryFile.renameTo(backup) }
    }

    private companion object {
        const val TAG = "SecureMemoryStorage"
        const val FILE_NAME = "kenza_memory_v2.enc"
        const val KEY_ALIAS = "allo_kenza_memory_aes_v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
    }
}
