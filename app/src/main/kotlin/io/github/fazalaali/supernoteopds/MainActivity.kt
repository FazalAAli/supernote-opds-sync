package io.github.fazalaali.supernoteopds

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.net.MalformedURLException
import java.net.URL

class MainActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var status: TextView
    private val onPrefChange = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "status") {
            status.text = prefs.status
            // E-ink panels can drop redraws that arrive in quick bursts; redraw once more after it settles.
            status.postInvalidateDelayed(500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)
        status = findViewById(R.id.status)
        val url = findViewById<EditText>(R.id.url)
        val user = findViewById<EditText>(R.id.user)
        val pass = findViewById<EditText>(R.id.pass)
        val headerName = findViewById<EditText>(R.id.headerName)
        val headerValue = findViewById<EditText>(R.id.headerValue)
        val folder = findViewById<EditText>(R.id.folder)
        val hours = findViewById<EditText>(R.id.hours)

        url.setText(prefs.feedUrl)
        user.setText(prefs.username)
        pass.setText(prefs.password)
        headerName.setText(prefs.headerName)
        headerValue.setText(prefs.headerValue)
        folder.setText(prefs.folder)
        hours.setText(prefs.hours.toString())

        findViewById<Button>(R.id.save).setOnClickListener {
            val feed = url.text.toString().trim()
            try {
                if (URL(feed).protocol != "https") return@setOnClickListener url.setError("Must be https://")
            } catch (e: MalformedURLException) {
                return@setOnClickListener url.setError("Not a valid URL")
            }
            val h = hours.text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0
            prefs.cfg.edit()
                .putString("url", feed)
                .putString("user", user.text.toString())
                .putString("pass", pass.text.toString())
                .putString("headerName", headerName.text.toString().trim())
                .putString("headerValue", headerValue.text.toString().trim())
                .putString("folder", folder.text.toString().trim().trim('/').ifEmpty { "Document/Books" })
                .putInt("hours", h)
                .apply()
            SyncWorker.schedule(this, h)
            prefs.setStatus(if (h > 0) "Saved. Syncing every $h h." else "Saved. Periodic sync off.")
        }
        findViewById<Button>(R.id.sync).setOnClickListener { SyncWorker.syncNow(this) }
        findViewById<Button>(R.id.grant).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
            } catch (e: ActivityNotFoundException) {
                status.text = "No settings screen. Run:\nadb shell appops set $packageName MANAGE_EXTERNAL_STORAGE allow"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<View>(R.id.grant).visibility =
            if (Environment.isExternalStorageManager()) View.GONE else View.VISIBLE
        status.text = prefs.status
        prefs.cfg.registerOnSharedPreferenceChangeListener(onPrefChange)
    }

    override fun onPause() {
        super.onPause()
        prefs.cfg.unregisterOnSharedPreferenceChangeListener(onPrefChange)
    }
}
