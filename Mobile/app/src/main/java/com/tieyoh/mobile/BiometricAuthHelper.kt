package com.tieyoh.mobile

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

// Backs "faster sign-in" with an Android Keystore key that only unlocks
// after Face ID / Touch ID / fingerprint succeeds — the app never just
// checks a "biometric passed" boolean, it decrypts a real secret that a
// device without that biometric enrolled cannot touch. This is Phase 2
// from the auth architecture doc: a device-bound app-unlock, not yet full
// WebAuthn/passkeys (Phase 3), so there's no backend involvement at all.
object BiometricAuthHelper {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "tieyoh_biometric_session_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun isBiometricAvailable(activity: FragmentActivity): Boolean {
        val manager = BiometricManager.from(activity)
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    // Removes the Keystore key entirely (called on logout) so a signed-out
    // device can't still decrypt an old session even if the encrypted blob
    // were somehow left behind.
    fun deleteKey() {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun buildPromptInfo(title: String, subtitle: String): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Use password instead")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
    }

    // Prompts biometrics, then encrypts [plaintext] with a fresh IV.
    // Returns (ciphertextBase64, ivBase64) ready for BiometricSessionStore.
    suspend fun encryptWithBiometrics(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        plaintext: String
    ): Pair<String, String> = suspendCancellableCoroutine { continuation ->
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                try {
                    val authedCipher = result.cryptoObject?.cipher ?: cipher
                    val ciphertext = authedCipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
                    val ciphertextB64 = Base64.encodeToString(ciphertext, Base64.DEFAULT)
                    val ivB64 = Base64.encodeToString(authedCipher.iv, Base64.DEFAULT)
                    continuation.resume(ciphertextB64 to ivB64)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                continuation.resumeWithException(BiometricCancelledException(errString.toString()))
            }

            override fun onAuthenticationFailed() {
                // A single failed scan doesn't settle the coroutine — the
                // system prompt lets the user try again on its own.
            }
        }

        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
        prompt.authenticate(buildPromptInfo(title, subtitle), BiometricPrompt.CryptoObject(cipher))
    }

    // Prompts biometrics, then decrypts [ciphertextB64] using the IV that
    // was stored alongside it at encryption time.
    suspend fun decryptWithBiometrics(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        ciphertextB64: String,
        ivB64: String
    ): String = suspendCancellableCoroutine { continuation ->
        val iv = Base64.decode(ivB64, Base64.DEFAULT)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        }

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                try {
                    val authedCipher = result.cryptoObject?.cipher ?: cipher
                    val ciphertext = Base64.decode(ciphertextB64, Base64.DEFAULT)
                    val plaintext = String(authedCipher.doFinal(ciphertext), Charsets.UTF_8)
                    continuation.resume(plaintext)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                continuation.resumeWithException(BiometricCancelledException(errString.toString()))
            }

            override fun onAuthenticationFailed() {
                // Let the system prompt handle retries on its own.
            }
        }

        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
        prompt.authenticate(buildPromptInfo(title, subtitle), BiometricPrompt.CryptoObject(cipher))
    }
}

class BiometricCancelledException(message: String) : Exception(message)
