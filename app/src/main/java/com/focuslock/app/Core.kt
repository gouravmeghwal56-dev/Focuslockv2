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

    fun selected(c: Context): Set<String> = HashSet(p(c).getStringSet("sel", emptySet()) ?: emptySet())
    fun setSelected(c: Context, s: Set<String>) { p(c).edit().putStringSet("sel", HashSet(s)).apply() }

    fun allowMode(c: Context) = p(c).getBoolean("allow", false)
    fun setAllowMode(c: Context, v: Boolean) { p(c).edit().putBoolean("allow", v).apply() }

    fun strict(c: Context) = p(c).getBoolean("strict", false)
    fun setStrict(c: Context, v: Boolean) { p(c).edit().putBoolean("strict", v).apply() }

    fun until(c: Context): Long = p(c).getLong("until", 0L)
    fun startSession(c: Context, minutes: Long) {
        p(c).edit().putLong("until", System.currentTimeMillis() + minutes * 60_000L).apply()
    }
    fun stopSession(c: Context) { p(c).edit().putLong("until", 0L).apply() }
    fun sessionOn(c: Context) = System.currentTimeMillis() < until(c)

    fun schedOn(c: Context) = p(c).getBoolean("sched_on", false)
    fun setSchedOn(c: Context, v: Boolean) { p(c).edit().putBoolean("sched_on", v).apply() }
    fun schedStart(c: Context) = p(c).getInt("sched_start", 18 * 60)
    fun schedEnd(c: Context) = p(c).getInt("sched_end", 22 * 60)
    fun setSchedStart(c: Context, m: Int) { p(c).edit().putInt("sched_start", m).apply() }
    fun setSchedEnd(c: Context, m: Int) { p(c).edit().putInt("sched_end", m).apply() }

    fun inSchedule(c: Context): Boolean {
        if (!schedOn(c)) return false
        val s = schedStart(c)
        val e = schedEnd(c)
        if (s == e) return false
        val cal = Calendar.getInstance()
        val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return if (s < e) now in s until e else (now >= s || now < e)
    }

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
        // Strict mode: Settings bhi band, taaki Accessibility off na kar sake
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
