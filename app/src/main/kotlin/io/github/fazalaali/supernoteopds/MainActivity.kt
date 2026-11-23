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

        val source = prefs.source
        url.setText(source.url)
        user.setText(source.username)
        pass.setText(source.password)
        headerName.setText(source.headerName)
        headerValue.setText(source.headerValue)
        folder.setText(source.folder)
        hours.setText(prefs.hours.toString())

        findViewById<Button>(R.id.save).setOnClickListener {
            val feed = url.text.toString().trim()
            try {
                if (URL(feed).protocol != "https") return@setOnClickListener url.setError("Must be https://")
            } catch (e: MalformedURLException) {
                return@setOnClickListener url.setError("Not a valid URL")
            }
            val h = hours.text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0
            prefs.source = Source(
                url = feed,
                username = user.text.toString(),
                password = pass.text.toString(),
                headerName = headerName.text.toString().trim(),
                headerValue = headerValue.text.toString().trim(),
                folder = folder.text.toString().trim().trim('/').ifEmpty { "Document/Books" },
            )
            prefs.hours = h
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
