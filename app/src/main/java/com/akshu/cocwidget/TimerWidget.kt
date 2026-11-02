package com.akshu.cocwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.os.SystemClock
import android.widget.RemoteViews
import org.json.JSONArray
import org.json.JSONObject

class TimerWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { mgr.updateAppWidget(it, build(ctx, widthDp(mgr, it))) }
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle) {
        mgr.updateAppWidget(id, build(ctx, widthDp(mgr, id)))
    }

    companion object {
        const val PREFS = "coc"; const val KEY = "json"
        private const val SCALE = 2f           // bitmap pixels per dp
        private const val MAX_SPAN = 14L * 86400 // seconds; bar length reference

        private fun widthDp(mgr: AppWidgetManager, id: Int): Int =
            mgr.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300).coerceAtLeast(200)

        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            mgr.getAppWidgetIds(ComponentName(ctx, TimerWidget::class.java))
                .forEach { mgr.updateAppWidget(it, build(ctx, widthDp(mgr, it))) }
        }

        private class Job(val id: Int, val name: String, val lvl: Int, val rem: Long)

        private fun walk(o: Any?, elapsed: Long, out: MutableList<Job>) {
            when (o) {
                is JSONArray -> for (i in 0 until o.length()) walk(o.opt(i), elapsed, out)
                is JSONObject -> {
                    if (o.has("timer") && o.has("data")) {
                        val id = o.getInt("data")
                        out.add(Job(id, Names.of(id), o.optInt("lvl", 0) + 1, maxOf(0L, o.getLong("timer") - elapsed)))
                    }
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

        // Icon: drop a PNG named ic_<id>.png into res/drawable to override the generated tile.
        private fun icon(ctx: Context, j: Job, s: Int): Bitmap {
            val res = ctx.resources.getIdentifier("ic_${j.id}", "drawable", ctx.packageName)
            if (res != 0) {
                val b = BitmapFactory.decodeResource(ctx.resources, res)
                if (b != null) return Bitmap.createScaledBitmap(b, s, s, true)
            }
            val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp); val p = Paint(Paint.ANTI_ALIAS_FLAG)
            val rect = RectF(1f, 1f, s - 1f, s - 1f)
            val hue = ((j.id * 47L) % 360).toFloat()
            p.shader = LinearGradient(0f, 0f, 0f, s.toFloat(),
                Color.HSVToColor(floatArrayOf(hue, 0.45f, 0.85f)), Color.HSVToColor(floatArrayOf(hue, 0.65f, 0.50f)), Shader.TileMode.CLAMP)
            c.drawRoundRect(rect, s * 0.22f, s * 0.22f, p); p.shader = null
            p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.07f; p.color = 0xFF1E1208.toInt()
            c.drawRoundRect(rect, s * 0.22f, s * 0.22f, p)
            val initials = j.name.split(" ", "-", ".").filter { it.isNotEmpty() }.take(2)
                .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
            val t = Paint(Paint.ANTI_ALIAS_FLAG); t.typeface = Typeface.DEFAULT_BOLD
            t.textSize = s * 0.42f; t.textAlign = Paint.Align.CENTER
            val y = s / 2f - (t.ascent() + t.descent()) / 2f
            t.style = Paint.Style.STROKE; t.strokeWidth = s * 0.08f; t.color = 0xFF1E1208.toInt()
            c.drawText(initials, s / 2f, y, t)
            t.style = Paint.Style.FILL; t.color = Color.WHITE
            c.drawText(initials, s / 2f, y, t)
            return bmp
        }

        private fun rowBitmap(ctx: Context, j: Job, w: Int, h: Int): Bitmap {
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp); val p = Paint(Paint.ANTI_ALIAS_FLAG)
            val rect = RectF(0f, 0f, w.toFloat(), h.toFloat()); val r = h * 0.25f
            p.color = 0xFF2A1A0E.toInt(); c.drawRoundRect(rect, r, r, p)
            val frac = (1f - minOf(j.rem, MAX_SPAN).toFloat() / MAX_SPAN).coerceIn(0.04f, 1f)
            c.save()
            c.clipPath(Path().apply { addRoundRect(rect, r, r, Path.Direction.CW) })
            p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0xFF8FDB3A.toInt(), 0xFF4A9A1C.toInt(), Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, w * frac, h.toFloat(), p); p.shader = null
            c.restore()
            val pad = h * 0.1f; val s = (h - 2 * pad).toInt()
            c.drawBitmap(icon(ctx, j, s), pad, pad, null)
            val t = Paint(Paint.ANTI_ALIAS_FLAG); t.typeface = Typeface.DEFAULT_BOLD; t.textSize = h * 0.40f
            var name = j.name; val maxW = w * 0.5f
            while (name.length > 3 && t.measureText(name) > maxW) name = name.dropLast(2)
            val x = pad * 2 + s; val y = h / 2f - (t.ascent() + t.descent()) / 2f
            val lvl = "  → ${j.lvl}"
            fun draw(txt: String, tx: Float, col: Int) {
                t.style = Paint.Style.STROKE; t.strokeWidth = h * 0.09f; t.color = 0xFF1B3A0A.toInt()
                c.drawText(txt, tx, y, t)
                t.style = Paint.Style.FILL; t.color = col
                c.drawText(txt, tx, y, t)
            }
            draw(name, x, Color.WHITE)
            draw(lvl, x + t.measureText(name), 0xFFFFE9A8.toInt())
            return bmp
        }

        private fun fill(ctx: Context, v: RemoteViews, container: Int, header: Int, label: String, list: List<Job>, widthDp: Int) {
            v.removeAllViews(container)
            v.setViewVisibility(header, if (list.isEmpty()) 8 else 0)
            v.setTextViewText(header, "$label  ${list.size}")
            val w = ((widthDp - 24) * SCALE).toInt(); val h = (36 * SCALE).toInt()
            val now = SystemClock.elapsedRealtime()
            for (j in list) {
                val r = RemoteViews(ctx.packageName, R.layout.row)
                r.setImageViewBitmap(R.id.bar, rowBitmap(ctx, j, w, h))
                r.setChronometerCountDown(R.id.timer, true)
                r.setChronometer(R.id.timer, now + j.rem * 1000, null, true)
                v.addView(container, r)
            }
        }

        fun build(ctx: Context, widthDp: Int = 320): RemoteViews {
            val v = RemoteViews(ctx.packageName, R.layout.widget)
            val pi = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            v.setOnClickPendingIntent(R.id.root, pi); v.setOnClickPendingIntent(R.id.sync, pi)
            val txt = ctx.getSharedPreferences(PREFS, 0).getString(KEY, null)
            val root = try { JSONObject(txt ?: "") } catch (e: Exception) { null }
            if (root == null) {
                v.setTextViewText(R.id.title, "Tap Sync to paste your JSON")
                listOf(R.id.h1, R.id.h2, R.id.h3).forEach { v.setViewVisibility(it, 8) }
                return v
            }
            val ts = root.optLong("timestamp")
            val elapsed = maxOf(0L, System.currentTimeMillis() / 1000 - ts)
            var th = 0
            val bs = root.optJSONArray("buildings")
            if (bs != null) for (i in 0 until bs.length()) {
                val o = bs.getJSONObject(i); if (o.optInt("data") == 1000001) th = o.optInt("lvl")
            }
            v.setTextViewText(R.id.title, root.optString("tag") + if (th > 0) "  TH$th" else "")
            fill(ctx, v, R.id.builders, R.id.h1, "BUILDERS", jobs(root, listOf("buildings", "heroes"), elapsed), widthDp)
            fill(ctx, v, R.id.lab, R.id.h2, "LAB", jobs(root, listOf("units", "spells", "siege_machines", "equipment"), elapsed), widthDp)
            fill(ctx, v, R.id.pets, R.id.h3, "PETS", jobs(root, listOf("pets"), elapsed), widthDp)
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
