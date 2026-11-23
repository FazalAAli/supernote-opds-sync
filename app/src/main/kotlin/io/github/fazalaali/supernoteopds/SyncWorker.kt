package io.github.fazalaali.supernoteopds

import android.content.Context
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.util.Xml
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

private const val TAG = "SupernoteOpds"

class HttpError(val code: Int, url: URL) : IOException("HTTP $code from ${url.host}")

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    private val prefs = Prefs(ctx)

    // Failures return Result.failure(), not retry(): a periodic job simply runs again next interval,
    // and a manual "Sync now" shouldn't keep retrying in the background (e.g. when away from home).
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        lock.withLock {
            val time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date())
            val source = prefs.source
            when {
                source.url.isBlank() -> fail("Set a feed URL first")
                !Environment.isExternalStorageManager() -> fail("Grant All files access first")
                else -> try {
                    prefs.setStatus("Syncing…")
                    val n = sync(source)
                    prefs.setStatus("$time: synced, $n new book(s)")
                    Result.success()
                } catch (e: HttpError) {
                    fail("$time: ${e.message}" + if (e.code == 401) " (check credentials)" else "")
                } catch (e: IOException) {
                    Log.w(TAG, "sync failed", e)
                    fail("$time: server unreachable (${e.message})")
                }
            }
        }
    }

    private fun fail(msg: String): Result {
        prefs.setStatus(msg)
        return Result.failure()
    }

    private fun sync(source: Source): Int {
        val dir = File(Environment.getExternalStorageDirectory(), source.folder)
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("can't create $dir")
        var count = 0
        var page: URL? = source.home
        val seen = HashSet<String>() // guards against a server whose rel="next" loops
        while (page != null && seen.add(page.toString())) {
            val url: URL = page
            val feed = get(source, url) { parseFeed(Xml.newPullParser(), it, url) }
            for (entry in feed.entries) {
                val key = source.syncKey(entry.id)
                if (prefs.synced.contains(key)) continue
                val (link, ext) = entry.pick() ?: continue
                prefs.setStatus("Downloading ${entry.title}…")
                val name = download(source, link.href, dir, entry.title, ext)
                prefs.synced.edit().putString(key, name).apply()
                Log.i(TAG, "saved $key as $name")
                count++
            }
            page = feed.next
        }
        return count
    }

    /** Downloads to a .part file and renames on completion, so the reader never sees a half-written book. */
    private fun download(source: Source, url: URL, dir: File, title: String, ext: String): String {
        val base = fileName(title)
        val file = generateSequence(1) { it + 1 }
            .map { File(dir, if (it == 1) "$base.$ext" else "$base ($it).$ext") }
            .first { !it.exists() }
        val part = File(dir, "${file.name}.part")
        try {
            get(source, url) { input -> part.outputStream().use { input.copyTo(it) } }
            if (!part.renameTo(file)) throw IOException("rename failed: $file")
        } finally {
            part.delete()
        }
        return file.name
    }

    /**
     * GETs [start], following redirects by hand so credentials are only ever sent over https to the
     * source's own host, never to a third-party link or redirect target.
     */
    private fun <T> get(source: Source, start: URL, read: (InputStream) -> T): T {
        val home = source.home
        var url = start
        repeat(6) {
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 15_000
                conn.readTimeout = 60_000
                conn.instanceFollowRedirects = false
                if (url.protocol == "https" && url.host == home.host && url.port == home.port) {
                    if (source.username.isNotEmpty() || source.password.isNotEmpty()) {
                        val token = Base64.encodeToString("${source.username}:${source.password}".toByteArray(), Base64.NO_WRAP)
                        conn.setRequestProperty("Authorization", "Basic $token")
                    }
                    if (source.headerName.isNotBlank()) conn.setRequestProperty(source.headerName, source.headerValue)
                }
                val code = conn.responseCode
                if (code in 300..399) {
                    url = URL(url, conn.getHeaderField("Location") ?: throw HttpError(code, url))
                    return@repeat
                }
                if (code !in 200..299) throw HttpError(code, url)
                return conn.inputStream.use(read)
            } finally {
                conn.disconnect()
            }
        }
        throw IOException("too many redirects from ${start.host}")
    }

    companion object {
        // Periodic and manual runs share one process; never let them download the same entry twice.
        private val lock = Mutex()
        private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedule(ctx: Context, hours: Int) {
            val wm = WorkManager.getInstance(ctx)
            if (hours <= 0) {
                wm.cancelUniqueWork("periodic")
            } else {
                val req = PeriodicWorkRequestBuilder<SyncWorker>(hours.toLong(), TimeUnit.HOURS)
                    .setConstraints(network).build()
                wm.enqueueUniquePeriodicWork("periodic", ExistingPeriodicWorkPolicy.UPDATE, req)
            }
        }

        fun syncNow(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(network).build()
            WorkManager.getInstance(ctx).enqueueUniqueWork("now", ExistingWorkPolicy.KEEP, req)
        }
    }
}

/** Title to a safe FAT/ext4 file name. */
fun fileName(title: String): String =
    title.replace(Regex("""[\\/:*?"<>|\x00-\x1f]"""), "_").trim().trim('.').take(120).ifEmpty { "book" }
