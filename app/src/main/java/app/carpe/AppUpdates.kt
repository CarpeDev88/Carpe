package app.carpe

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class CarpeUpdate(
    val version: String,
    val apkName: String,
    val apkUrl: String,
    val checksumUrl: String
)

class CarpeUpdateException(message: String) : Exception(message)

object CarpeUpdates {
    private const val RELEASES_API =
        "https://api.github.com/repos/CarpeDev88/Carpe/releases?per_page=10"
    private const val APK_PREFIX = "CARPE-v"
    private const val APK_SUFFIX = ".apk"
    private const val MAX_APK_BYTES = 120L * 1024L * 1024L

    fun compareVersions(left: String, right: String): Int {
        val a = versionParts(left) ?: return 0
        val b = versionParts(right) ?: return 0
        for (index in 0 until maxOf(a.size, b.size)) {
            val av = a.getOrElse(index) { 0 }
            val bv = b.getOrElse(index) { 0 }
            if (av != bv) return av.compareTo(bv)
        }
        return 0
    }

    private fun versionParts(value: String): List<Int>? {
        val clean = value.trim().removePrefix("v").removePrefix("V")
        if (!clean.matches(Regex("[0-9]+(\\.[0-9]+)*"))) return null
        return clean.split(".").map { it.toIntOrNull() ?: return null }
    }

    suspend fun checkForUpdate(currentVersion: String): CarpeUpdate? =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL(RELEASES_API).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4_000
                    readTimeout = 5_000
                    setRequestProperty("User-Agent", "CARPE-Android")
                    setRequestProperty("Accept", "application/vnd.github+json")
                }
                try {
                    if (connection.responseCode !in 200..299) return@runCatching null
                    val releases = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
                    for (index in 0 until releases.length()) {
                        val release = releases.optJSONObject(index) ?: continue
                        val version = release.optString("tag_name").removePrefix("v")
                        if (compareVersions(version, currentVersion) <= 0) continue
                        val assets = release.optJSONArray("assets") ?: continue
                        var apkUrl: String? = null
                        var apkName: String? = null
                        var checksumUrl: String? = null
                        for (assetIndex in 0 until assets.length()) {
                            val asset = assets.optJSONObject(assetIndex) ?: continue
                            val name = asset.optString("name")
                            val url = asset.optString("browser_download_url")
                            if (!url.startsWith("https://github.com/CarpeDev88/Carpe/releases/download/")) continue
                            if (name.startsWith(APK_PREFIX) && name.endsWith(APK_SUFFIX)) {
                                apkName = name
                                apkUrl = url
                            } else if (name.startsWith(APK_PREFIX) && name.endsWith("$APK_SUFFIX.sha256")) {
                                checksumUrl = url
                            }
                        }
                        if (apkName != null && apkUrl != null && checksumUrl != null) {
                            return@runCatching CarpeUpdate(version, apkName, apkUrl, checksumUrl)
                        }
                    }
                    null
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
        }

    suspend fun downloadAndVerify(context: Context, update: CarpeUpdate): File =
        withContext(Dispatchers.IO) {
            val folder = File(context.cacheDir, "updates").apply { mkdirs() }
            val part = File(folder, update.apkName + ".part")
            val apk = File(folder, update.apkName)
            try {
                val connection = (URL(update.apkUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", "CARPE-Android")
                }
                try {
                    val status = connection.responseCode
                    if (status !in 200..299) throw CarpeUpdateException("GitHub could not download the update (HTTP $status).")
                    val length = connection.contentLengthLong
                    if (length > MAX_APK_BYTES) throw CarpeUpdateException("The update file is larger than expected.")
                    connection.inputStream.use { input ->
                        part.outputStream().use { output ->
                            val buffer = ByteArray(32 * 1024)
                            var total = 0L
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                total += count
                                if (total > MAX_APK_BYTES) throw CarpeUpdateException("The update file is larger than expected.")
                                output.write(buffer, 0, count)
                            }
                        }
                    }
                } finally {
                    connection.disconnect()
                }

                val checksumConnection = (URL(update.checksumUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5_000
                    readTimeout = 5_000
                    setRequestProperty("User-Agent", "CARPE-Android")
                }
                val checksumText = try {
                    if (checksumConnection.responseCode !in 200..299) {
                        throw CarpeUpdateException("The update checksum could not be downloaded.")
                    }
                    checksumConnection.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    checksumConnection.disconnect()
                }
                val expected = checksumText.trim().split(Regex("\\s+")).firstOrNull()
                    ?.takeIf { it.matches(Regex("[0-9a-fA-F]{64}")) }
                    ?: throw CarpeUpdateException("The update checksum is invalid.")
                val digest = MessageDigest.getInstance("SHA-256")
                part.inputStream().buffered().use { input ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                    }
                }
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (!actual.equals(expected, ignoreCase = true)) {
                    throw CarpeUpdateException("The downloaded update did not pass its integrity check.")
                }
                if (!part.renameTo(apk)) throw CarpeUpdateException("CARPE could not prepare the update for Android.")
                verifyPackageAndSigner(context, apk)
                apk
            } catch (error: Exception) {
                part.delete()
                apk.delete()
                throw error
            }
        }

    private fun verifyPackageAndSigner(context: Context, apk: File) {
        val packageManager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val candidate = packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
            ?: throw CarpeUpdateException("Android could not read the downloaded update.")
        if (candidate.packageName != context.packageName) {
            throw CarpeUpdateException("The downloaded package is not CARPE.")
        }
        val current = packageManager.getPackageInfo(context.packageName, flags)
        if (signerDigests(candidate) != signerDigests(current)) {
            throw CarpeUpdateException(
                "This build was signed differently. Android will not install it over the current app. " +
                    "No data was removed. Keep CARPE installed until a safe one-time migration is ready."
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun signerDigests(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.apkContentsSigners
        } else {
            info.signatures
        } ?: return emptySet()
        return signatures.map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    fun installerIntent(context: Context, apk: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".updates",
            apk
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
