package com.focuslock.app

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import java.util.Calendar

object Store {
    private fun p(c: Context) = c.getSharedPreferences("focus", Context.MODE_PRIVATE)

    // ---------- Bags (3 app lists) ----------
    fun activeBag(c: Context) = p(c).getInt("bag_active", 0)
    fun setActiveBag(c: Context, i: Int) { p(c).edit().putInt("bag_active", i).apply() }
    private fun bagKey(i: Int) = if (i == 0) "sel" else "sel$i"
    fun bag(c: Context, i: Int): Set<String> = HashSet(p(c).getStringSet(bagKey(i), emptySet()) ?: emptySet())
    fun setBag(c: Context, i: Int, s: Set<String>) { p(c).edit().putStringSet(bagKey(i), HashSet(s)).apply() }
    fun selected(c: Context): Set<String> = bag(c, activeBag(c))

    // ---------- Mode ----------
    fun allowMode(c: Context) = p(c).getBoolean("allow", false)
    fun setAllowMode(c: Context, v: Boolean) { p(c).edit().putBoolean("allow", v).apply() }
    fun strict(c: Context) = p(c).getBoolean("strict", false)
    fun setStrict(c: Context, v: Boolean) { p(c).edit().putBoolean("strict", v).apply() }

    // ---------- Session ----------
    fun until(c: Context): Long = p(c).getLong("until", 0L)
    fun sessionMinutes(c: Context) = p(c).getInt("sess_min", 0)
    fun startSession(c: Context, minutes: Long) {
        p(c).edit()
            .putLong("until", System.currentTimeMillis() + minutes * 60_000L)
            .putInt("sess_min", minutes.toInt())
            .apply()
        addMinutes(c, minutes)
    }
    fun stopSession(c: Context) { p(c).edit().putLong("until", 0L).apply() }
    fun sessionOn(c: Context) = System.currentTimeMillis() < until(c)

    // ---------- Routines (3) ----------
    fun rtDays(c: Context, r: Int) = p(c).getInt("rt${r}_days", 127)
    fun setRtDays(c: Context, r: Int, v: Int) { p(c).edit().putInt("rt${r}_days", v).apply() }
    fun rtStart(c: Context, r: Int) = p(c).getInt("rt${r}_start", 18 * 60)
    fun rtEnd(c: Context, r: Int) = p(c).getInt("rt${r}_end", 22 * 60)
    fun setRtStart(c: Context, r: Int, v: Int) { p(c).edit().putInt("rt${r}_start", v).apply() }
    fun setRtEnd(c: Context, r: Int, v: Int) { p(c).edit().putInt("rt${r}_end", v).apply() }
    fun rtOn(c: Context, r: Int) = p(c).getBoolean("rt${r}_on", false)
    fun setRtOn(c: Context, r: Int, v: Boolean) { p(c).edit().putBoolean("rt${r}_on", v).apply() }

    fun inSchedule(c: Context): Boolean {
        val cal = Calendar.getInstance()
        val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val dow = cal.get(Calendar.DAY_OF_WEEK) - 1
        val prev = (dow + 6) % 7
        for (r in 0..2) {
            if (!rtOn(c, r)) continue
            val mask = rtDays(c, r)
            val s = rtStart(c, r)
            val e = rtEnd(c, r)
            if (s == e) continue
            val today = ((mask shr dow) and 1) == 1
            val yesterday = ((mask shr prev) and 1) == 1
            if (s < e) {
                if (today && now >= s && now < e) return true
            } else {
                if ((today && now >= s) || (yesterday && now < e)) return true
            }
        }
        return false
    }

    // ---------- Stats ----------
    private fun dayKey(y: Int, m: Int, d: Int) = String.format("min_%04d%02d%02d", y, m + 1, d)
    fun addMinutes(c: Context, minutes: Long) {
        val cal = Calendar.getInstance()
        val k = dayKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        p(c).edit().putLong(k, p(c).getLong(k, 0L) + minutes).apply()
    }
    fun dayMinutes(c: Context, y: Int, m: Int, d: Int): Long = p(c).getLong(dayKey(y, m, d), 0L)

    fun active(c: Context) = sessionOn(c) || inSchedule(c)
}

class BlockService : AccessibilityService() {
    private var lastAt = 0L
    private val alwaysOk = setOf(
        "com.android.systemui", "android", "com.android.phone",
        "com.android.dialer", "com.google.android.dialer", "com.android.emergency",
        "com.android.server.telecom", "com.android.incallui"
    )

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        val pkg = e?.packageName?.toString() ?: return
        if (pkg == packageName || pkg in alwaysOk) return
        if (!Store.active(this)) return
        if (pkg == homePkg() || pkg == imePkg()) return

        val sel = Store.selected(this)
        var block = if (Store.allowMode(this)) pkg !in sel else pkg in sel
        if (Store.strict(this) && pkg == "com.android.settings") block = true
        if (!block) return

        val now = System.currentTimeMillis()
        if (now - lastAt < 600) return
        lastAt = now
        performGlobalAction(GLOBAL_ACTION_HOME)
        Toast.makeText(this, "Focus mode ON", Toast.LENGTH_SHORT).show()
    }

    private fun homePkg(): String? {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
    }

    private fun imePkg(): String? {
        val id = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return id?.substringBefore('/')
    }

    override fun onInterrupt() {}
}
