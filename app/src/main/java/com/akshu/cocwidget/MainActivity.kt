package com.akshu.cocwidget

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.*
import org.json.JSONObject

class MainActivity : Activity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val box = EditText(this).apply {
            hint = "Paste the Clash of Clans JSON export here"; minLines = 8; gravity = android.view.Gravity.TOP
            setText(getSharedPreferences(TimerWidget.PREFS, 0).getString(TimerWidget.KEY, ""))
        }
        val paste = Button(this).apply { text = "Paste from clipboard" }
        val save = Button(this).apply { text = "Save & update widget" }
        paste.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            box.setText(cm.primaryClip?.getItemAt(0)?.coerceToText(this) ?: "")
        }
        save.setOnClickListener {
            try {
                JSONObject(box.text.toString())
                getSharedPreferences(TimerWidget.PREFS, 0).edit().putString(TimerWidget.KEY, box.text.toString()).apply()
                TimerWidget.refresh(this)
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(this, "Invalid JSON", Toast.LENGTH_LONG).show() }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 32)
            addView(paste); addView(save); addView(box)
        })
    }
}
