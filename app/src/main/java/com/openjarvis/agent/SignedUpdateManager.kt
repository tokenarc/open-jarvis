package com.openjarvis.agent

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest

/** Verifies downloaded APK identity and digest before showing Android's install confirmation UI. */
class SignedUpdateManager(private val context: Context) {
    data class Verification(val valid: Boolean, val message: String)

    fun verifyApk(apk: File, expectedSha256: String): Verification {
        if (!apk.isFile || apk.length() <= 0L) return Verification(false, "APK file is missing or empty")
        val digest = MessageDigest.getInstance("SHA-256")
        apk.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                digest.update(buffer, 0, count)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actual.equals(expectedSha256.trim(), ignoreCase = true)) {
            return Verification(false, "APK SHA-256 mismatch")
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val info = context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
            ?: return Verification(false, "APK package metadata is invalid")
        if (info.packageName != context.packageName) return Verification(false, "APK package name mismatch")
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        val candidateSigners = if (Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.apkContentsSigners?.map { it.toCharsString() }.orEmpty()
        } else info.signatures?.map { it.toCharsString() }.orEmpty()
        val installedSigners = if (Build.VERSION.SDK_INT >= 28) {
            installed.signingInfo?.apkContentsSigners?.map { it.toCharsString() }.orEmpty()
        } else installed.signatures?.map { it.toCharsString() }.orEmpty()
        if (candidateSigners.isEmpty() || installedSigners.isEmpty() ||
            candidateSigners.toSet() != installedSigners.toSet()) {
            return Verification(false, "APK signing certificate does not match installed Open Jarvis")
        }
        return Verification(true, "APK digest, package and signing certificate verified")
    }

    fun verifyAndRequestInstall(apk: File, expectedSha256: String): Verification {
        val result = verifyApk(apk, expectedSha256)
        if (result.valid) requestInstallVerified(apk)
        return result
    }

    private fun requestInstallVerified(apk: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
