package io.github.fazalaali.supernoteopds

import android.content.Context
import android.content.SharedPreferences
import java.net.URL

/** One OPDS catalog to sync from. */
data class Source(
    val url: String,
    val username: String = "",
    val password: String = "",
    /** Optional extra auth header, e.g. Bindery's X-Api-Key or Komga's X-API-Key. */
    val headerName: String = "",
    val headerValue: String = "",
    /** Destination, relative to the storage root. */
    val folder: String = "Document/Books",
) {
    /** Credentials are only sent here (https, same host and port). */
    val home: URL get() = URL(url)

    /**
     * Key in the synced store. Scoped by server so two catalogs with the same entry ids don't collide,
     * but not by full URL, so switching feeds on the same server (e.g. /recent to /all) doesn't re-download.
     */
    fun syncKey(entryId: String) = "${home.authority} $entryId"
}

// ponytail: single source stored as flat keys. For "Add new source", store a list (e.g. JSON) and
// loop SyncWorker.sync over it; Source and the synced keys already support that.
// ponytail: password/API key sit in app-private prefs, unencrypted. Fine for a single-user e-reader;
// wrap them with an Android Keystore key if that threat model changes.
class Prefs(ctx: Context) {
    val cfg: SharedPreferences = ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE)
    /** Source.syncKey to the file name it was saved as. Kept after local deletion so books aren't re-downloaded. */
    val synced: SharedPreferences = ctx.getSharedPreferences("synced", Context.MODE_PRIVATE)

    var source: Source
        get() = Source(
            url = cfg.getString("url", "")!!,
            username = cfg.getString("user", "")!!,
            password = cfg.getString("pass", "")!!,
            headerName = cfg.getString("headerName", "")!!,
            headerValue = cfg.getString("headerValue", "")!!,
            folder = cfg.getString("folder", "Document/Books")!!,
        )
        set(s) = cfg.edit()
            .putString("url", s.url)
            .putString("user", s.username)
            .putString("pass", s.password)
            .putString("headerName", s.headerName)
            .putString("headerValue", s.headerValue)
            .putString("folder", s.folder)
            .apply()

    /** Sync interval in hours; 0 disables periodic sync. */
    var hours: Int
        get() = cfg.getInt("hours", 6)
        set(h) = cfg.edit().putInt("hours", h).apply()

    val status get() = cfg.getString("status", "Not synced yet")!!
    fun setStatus(s: String) = cfg.edit().putString("status", s).apply()
}
