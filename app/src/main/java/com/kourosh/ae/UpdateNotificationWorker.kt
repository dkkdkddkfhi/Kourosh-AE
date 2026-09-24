package com.kourosh.ae

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Background release check. GitHub does not push events to installed APKs, so
 * this is the reliable no-server fallback: the OS runs it only while network
 * is available and applies its own battery-aware scheduling.
 */
class UpdateNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val release = runCatching { fetchLatestRelease() }.getOrNull() ?: return Result.retry()
        val current = currentVersion()
        if (!isNewer(release.version, current)) return Result.success()

        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_NOTIFIED_VERSION, null) == release.version) return Result.success()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        ensureChannel()
        val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_PAGE_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            701,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notes = release.notes
            .lineSequence()
            .map(String::trim)
            .firstOrNull { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("-") }
            ?.take(150)
            ?: Strings.t("New features and important fixes are available.")
        val body = Strings.tf("Version %s is available. %s", release.version, notes)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_kourosh_notification)
            .setContentTitle(Strings.t("Kourosh-AE update available"))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            // Same royal gold as the VPN row — one brand across both notifications.
            .setColor(KouroshAeVpnService.NOTIFICATION_ACCENT)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        prefs.edit().putString(KEY_NOTIFIED_VERSION, release.version).apply()
        return Result.success()
    }

    private fun fetchLatestRelease(): Release {
        val connection = (URL(RELEASE_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Kourosh-AE-Android")
        }
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "GitHub returned ${connection.responseCode}"
            }
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            return Release(
                version = json.getString("tag_name").removePrefix("v"),
                notes = json.optString("body"),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun currentVersion(): String = applicationContext.packageManager
        .getPackageInfo(applicationContext.packageName, 0).versionName ?: "0.0.0"

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    Strings.t("App updates"),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = Strings.t("New Kourosh-AE versions and important release notes")
                },
            )
        }
    }

    private data class Release(val version: String, val notes: String)

    companion object {
        private const val UNIQUE_PERIODIC_WORK = "kourosh-ae-update-check"
        private const val UNIQUE_FIRST_CHECK = "kourosh-ae-update-check-now"
        private const val CHANNEL_ID = "kourosh_ae_updates"
        private const val NOTIFICATION_ID = 701
        private const val PREFS = "kourosh_ae_update_notifications"
        private const val KEY_NOTIFIED_VERSION = "notified_version"
        private const val RELEASE_URL = "https://api.github.com/repos/dkkdkddkfhi/Kourosh-AE/releases/latest"
        private const val RELEASES_PAGE_URL = "https://github.com/dkkdkddkfhi/Kourosh-AE/releases/latest"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val periodic = PeriodicWorkRequest.Builder(
                UpdateNotificationWorker::class.java,
                6,
                TimeUnit.HOURS,
                1,
                TimeUnit.HOURS,
            ).setConstraints(constraints).build()
            val manager = WorkManager.getInstance(context.applicationContext)
            manager.enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                periodic,
            )
            val first = OneTimeWorkRequest.Builder(UpdateNotificationWorker::class.java)
                .setConstraints(constraints)
                .build()
            manager.enqueueUniqueWork(UNIQUE_FIRST_CHECK, ExistingWorkPolicy.REPLACE, first)
        }

        private fun isNewer(remote: String, local: String): Boolean {
            val remoteParts = remote.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
            val localParts = local.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
            for (index in 0 until maxOf(remoteParts.size, localParts.size)) {
                val comparison = remoteParts.getOrElse(index) { 0 }
                    .compareTo(localParts.getOrElse(index) { 0 })
                if (comparison != 0) return comparison > 0
            }
            return false
        }
    }
}
