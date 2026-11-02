package com.akshu.cocwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import org.json.JSONArray
import org.json.JSONObject

class TimerWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { mgr.updateAppWidget(it, build(ctx)) }
    }

    companion object {
        const val PREFS = "coc"; const val KEY = "json"

        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, TimerWidget::class.java))
            ids.forEach { mgr.updateAppWidget(it, build(ctx)) }
        }

        private class Job(val name: String, val lvl: Int, val rem: Long)

        private fun walk(o: Any?, elapsed: Long, out: MutableList<Job>) {
            when (o) {
                is JSONArray -> for (i in 0 until o.length()) walk(o.opt(i), elapsed, out)
                is JSONObject -> {
                    if (o.has("timer") && o.has("data"))
                        out.add(Job(Names.of(o.getInt("data")), o.optInt("lvl", 0) + 1,
                            maxOf(0L, o.getLong("timer") - elapsed)))
                    o.keys().forEach { walk(o.opt(it), elapsed, out) }
                }
            }
        }

        private fun jobs(root: JSONObject, keys: List<String>, elapsed: Long): List<Job> {
            val out = mutableListOf<Job>()
            keys.forEach { walk(root.opt(it), elapsed, out) }
            return out.sortedBy { it.rem }
        }

        private fun fmt(s: Long): String {
            val d = s / 86400; val h = s % 86400 / 3600; val m = s % 3600 / 60
            return if (d > 0) "${d}d ${h}h" else if (h > 0) "${h}h ${m}m" else "${m}m"
        }

        private fun fill(ctx: Context, v: RemoteViews, container: Int, header: Int, list: List<Job>) {
            v.removeAllViews(container)
            v.setViewVisibility(header, if (list.isEmpty()) 8 else 0)
            val now = SystemClock.elapsedRealtime()
            for (j in list) {
                val r = RemoteViews(ctx.packageName, R.layout.row)
                r.setTextViewText(R.id.name, "${j.name} → ${j.lvl}")
                r.setChronometerCountDown(R.id.timer, true)
                r.setChronometer(R.id.timer, now + j.rem * 1000, null, true)
                v.addView(container, r)
            }
        }

        fun build(ctx: Context): RemoteViews {
            val v = RemoteViews(ctx.packageName, R.layout.widget)
            v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(ctx, 0,
                Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            val txt = ctx.getSharedPreferences(PREFS, 0).getString(KEY, null)
            val root = try { JSONObject(txt ?: "") } catch (e: Exception) { null }
            if (root == null) {
                v.setTextViewText(R.id.title, "Tap to paste your CoC JSON")
                return v
            }
            val ts = root.optLong("timestamp")
            val elapsed = maxOf(0L, System.currentTimeMillis() / 1000 - ts)
            val b = jobs(root, listOf("buildings", "heroes"), elapsed)
            v.setTextViewText(R.id.title, "${root.optString("tag")}  •  Builders busy: ${b.size}")
            fill(ctx, v, R.id.builders, R.id.h1, b)
            fill(ctx, v, R.id.lab, R.id.h2, jobs(root, listOf("units", "spells", "siege_machines", "equipment"), elapsed))
            fill(ctx, v, R.id.pets, R.id.h3, jobs(root, listOf("pets"), elapsed))
            val parts = mutableListOf<String>()
            val hs = root.optJSONArray("helpers")
            if (hs != null) for (i in 0 until hs.length()) {
                val h = hs.getJSONObject(i)
                val cd = h.optLong("helper_cooldown", 0) - elapsed
                parts.add(Names.of(h.getInt("data")) + " L" + h.optInt("lvl") + if (cd > 0) " (${fmt(cd)})" else " idle")
            }
            parts.add("synced ${fmt(elapsed)} ago")
            v.setTextViewText(R.id.footer, parts.joinToString(" • "))
            return v
        }
    }
}
