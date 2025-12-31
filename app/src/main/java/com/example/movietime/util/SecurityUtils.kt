package com.example.movietime.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.movietime.BuildConfig
import java.security.MessageDigest

/**
 * Security utilities for the MovieTime app.
 * Provides secure storage, device fingerprinting, and security checks.
 */
object SecurityUtils {

    private const val ENCRYPTED_PREFS_NAME = "secure_prefs"
    private const val REGULAR_PREFS_NAME = "app_prefs"

    /**
     * Get encrypted SharedPreferences for storing sensitive data.
     * Falls back to regular SharedPreferences on older devices or if encryption fails.
     */
    fun getSecurePreferences(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular prefs if encryption fails
            if (BuildConfig.DEBUG) {
                android.util.Log.e("SecurityUtils", "Failed to create encrypted prefs", e)
            }
            context.getSharedPreferences(REGULAR_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Get regular (non-encrypted) SharedPreferences for non-sensitive data.
     */
    fun getRegularPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(REGULAR_PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Generate a device fingerprint for fraud detection / device binding.
     * Does not use hardware IDs that require permissions.
     */
    fun getDeviceFingerprint(context: Context): String {
        val fingerprintData = StringBuilder().apply {
            append(Build.BOARD)
            append(Build.BRAND)
            append(Build.DEVICE)
            append(Build.HARDWARE)
            append(Build.MANUFACTURER)
            append(Build.MODEL)
            append(Build.PRODUCT)
            // Android ID - unique per app/user combination
            append(Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID))
        }
        
        return sha256(fingerprintData.toString())
    }

    /**
     * Check if the device appears to be rooted.
     * This is a basic check and can be bypassed by sophisticated users.
     */
    fun isDeviceRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        
        for (path in paths) {
            if (java.io.File(path).exists()) {
                return true
            }
        }
        
        return try {
            Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if the app is running in an emulator.
     */
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }

    /**
     * Check if the app is running with debugger attached.
     */
    fun isDebuggerAttached(): Boolean {
        return android.os.Debug.isDebuggerConnected() || android.os.Debug.waitingForDebugger()
    }

    /**
     * Validates that the app signature matches expected value.
     * Useful for detecting repackaged/modified APKs.
     */
    fun isValidSignature(context: Context, expectedSignatureHash: String): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            signatures?.any { signature ->
                sha256(signature.toByteArray()) == expectedSignatureHash
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Sanitize user input to prevent injection attacks.
     */
    fun sanitizeInput(input: String): String {
        return input
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("&", "&amp;")
            .trim()
    }

    /**
     * Validate email format.
     */
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Hash a string using SHA-256.
     */
    private fun sha256(input: String): String {
        return sha256(input.toByteArray())
    }

    private fun sha256(input: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input)
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    /**
     * Securely clear a char array (for passwords).
     */
    fun clearCharArray(array: CharArray) {
        array.fill('\u0000')
    }

    /**
     * Check if running in debug mode.
     */
    fun isDebugBuild(): Boolean {
        return BuildConfig.DEBUG
    }
}
