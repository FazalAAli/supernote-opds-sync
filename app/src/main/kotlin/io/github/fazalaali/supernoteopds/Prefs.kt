package io.github.fazalaali.supernoteopds

import android.content.Context
import android.content.SharedPreferences

// ponytail: password sits in app-private prefs, unencrypted. Fine for a single-user e-reader;
// wrap it with an Android Keystore key if that threat model changes.
class Prefs(ctx: Context) {
    val cfg: SharedPreferences = ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE)
    /** Entry id to the file name it was saved as. Kept after local deletion so books aren't re-downloaded. */
    val synced: SharedPreferences = ctx.getSharedPreferences("synced", Context.MODE_PRIVATE)

    val feedUrl get() = cfg.getString("url", "")!!
    val username get() = cfg.getString("user", "")!!
    val password get() = cfg.getString("pass", "")!!
    /** Optional extra auth header, e.g. Bindery's X-Api-Key or Komga's X-API-Key. */
    val headerName get() = cfg.getString("headerName", "")!!
    val headerValue get() = cfg.getString("headerValue", "")!!
    val folder get() = cfg.getString("folder", "Document/Books")!!
    /** Sync interval in hours; 0 disables periodic sync. */
    val hours get() = cfg.getInt("hours", 6)
    val status get() = cfg.getString("status", "Not synced yet")!!

    fun setStatus(s: String) = cfg.edit().putString("status", s).apply()
}
