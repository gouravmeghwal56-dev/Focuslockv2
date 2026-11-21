package com.focuslock.app

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import java.util.Calendar

object Store {
    private fun p(c: Context) = c.getSharedPreferences("focus", Context.MODE_PRIVATE)

    // ---------- Bags (3 app lists) ----------
    // Abhi sirf Bag 1 chalta hai (max 4 apps). Bag 2 aur 3 locked hain, future me padhai ke apps ke liye.
    const val MAX_BAG_APPS = 4
    const val UNLOCKED_BAGS = 1
    fun activeBag(c: Context) = 0
    fun setActiveBag(c: Context, i: Int) { p(c).edit().putInt("bag_active", 0).apply() }
    private fun bagKey(i: Int) = if (i == 0) "sel" else "sel$i"
    fun bag(c: Context, i: Int): Set<String> {
        val all = HashSet(p(c).getStringSet(bagKey(i), emptySet()) ?: emptySet())
        return if (i == 0) all.toList().sorted().take(MAX_BAG_APPS).toSet() else all
    }
    fun setBag(c: Context, i: Int, s: Set<String>) {
        val v = if (i == 0) s.toList().sorted().take(MAX_BAG_APPS).toSet() else s
        p(c).edit().putStringSet(bagKey(i), HashSet(v)).apply()
    }
    fun selected(c: Context): Set<String> = bag(c, activeBag(c))

    // ---------- Detox (sirf Shorts / Reels band, baaki sab chalega) ----------
    fun detoxShorts(c: Context) = p(c).getBoolean("dx_shorts", false)
    fun setDetoxShorts(c: Context, v: Boolean) { p(c).edit().putBoolean("dx_shorts", v).apply() }
    fun detoxReels(c: Context) = p(c).getBoolean("dx_reels", false)
    fun setDetoxReels(c: Context, v: Boolean) { p(c).edit().putBoolean("dx_reels", v).apply() }

    // ---------- Manual stopwatch (black screen, top right) ----------
    fun swRunning(c: Context) = p(c).getLong("sw_start", 0L) > 0L
    fun swElapsed(c: Context): Long {
        val st = p(c).getLong("sw_start", 0L)
        val base = p(c).getLong("sw_base", 0L)
        return if (st > 0L) base + (System.currentTimeMillis() - st).coerceAtLeast(0L) else base
    }
    fun swToggle(c: Context) {
        val e = swElapsed(c)
        if (swRunning(c)) {
            p(c).edit().putLong("sw_base", e).putLong("sw_start", 0L).apply()
        } else {
            p(c).edit().putLong("sw_base", e).putLong("sw_start", System.currentTimeMillis()).apply()
        }
    }
    fun swReset(c: Context) { p(c).edit().putLong("sw_base", 0L).putLong("sw_start", 0L).apply() }

    // ---------- Profile ----------
    fun profileName(c: Context): String = p(c).getString("profile_name", "") ?: ""
    fun setProfileName(c: Context, v: String) { p(c).edit().putString("profile_name", v.trim()).apply() }

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

    // +15 hold: session ko lamba karta hai (max 24 ghante)
    fun extendSession(c: Context, minutes: Long) {
        val now = System.currentTimeMillis()
        val u = until(c)
        if (now >= u) return
        val cap = now + 1440L * 60_000L
        val nu = minOf(u + minutes * 60_000L, cap)
        val added = (nu - u) / 60_000L
        if (added <= 0L) return
        p(c).edit()
            .putLong("until", nu)
            .putInt("sess_min", sessionMinutes(c) + added.toInt())
            .apply()
        addMinutes(c, added)
    }

    // Parachute: roz 1 milta hai (strict session se emergency exit)
    fun chutes(c: Context): Int {
        val cal = Calendar.getInstance()
        val today = (cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)).toString()
        if (p(c).getString("chute_day", "") != today) {
            p(c).edit().putString("chute_day", today).putInt("chutes", 1).apply()
        }
        return p(c).getInt("chutes", 1)
    }
    fun useChute(c: Context): Boolean {
        val n = chutes(c)
        if (n <= 0) return false
        p(c).edit().putInt("chutes", n - 1).apply()
        return true
    }
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

    fun anyRoutineOn(c: Context) = (0..2).any { rtOn(c, it) }

    fun active(c: Context) = sessionOn(c) || inSchedule(c)
}

class BlockService : AccessibilityService() {
    private var lastAt = 0L
    private var homeAt = 0L
    private val handler = Handler(Looper.getMainLooper())

    // Phone / emergency apps hamesha chalenge
    private val alwaysOk = setOf(
        "com.android.systemui", "android", "com.android.phone",
        "com.android.dialer", "com.google.android.dialer", "com.android.emergency",
        "com.android.server.telecom", "com.android.incallui",
        "com.samsung.android.dialer", "com.samsung.android.incallui",
        "com.samsung.android.app.telephonyui",
        "com.android.contacts", "com.google.android.contacts"
    )

    // Dialer aur Camera (sirf phone ke system apps) hamesha chalenge
    private val emergency: Set<String> by lazy {
        val out = HashSet<String>()
        val intents = listOf(
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:")),
            Intent(Intent.ACTION_CALL_BUTTON),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        )
        for (intent in intents) {
            try {
                for (r in packageManager.queryIntentActivities(intent, 0)) {
                    val f = r.activityInfo.applicationInfo.flags
                    val sys = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
                    if ((f and sys) != 0) out.add(r.activityInfo.packageName)
                }
            } catch (e: Exception) {
            }
        }
        out
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        val pkg = e?.packageName?.toString() ?: return
        checkDetox(pkg)
        if (e?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (pkg == packageName || pkg in alwaysOk) return
        if (!Store.active(this)) return
        if (pkg in emergency) return
        // Session chal raha ho aur home screen aaye to wapas black focus screen pe bhej do
        if (pkg == homePkg()) {
            val t = System.currentTimeMillis()
            if (Store.sessionOn(this) && t - homeAt > 800L) {
                homeAt = t
                handler.postDelayed({ if (Store.sessionOn(this)) openFocus() }, 150L)
            }
            return
        }
        if (pkg == imePkg()) return

        val sel = Store.selected(this)
        var block = if (Store.allowMode(this)) pkg !in sel else pkg in sel
        if (Store.strict(this) && pkg == "com.android.settings") block = true
        if (!block) return

        val now = System.currentTimeMillis()
        if (now - lastAt < 600) return
        lastAt = now
        performGlobalAction(GLOBAL_ACTION_HOME)
        Toast.makeText(this, "Focus mode ON", Toast.LENGTH_SHORT).show()
        if (Store.sessionOn(this)) handler.postDelayed({ openFocus() }, 250L)
    }

    // ---------- Detox: YouTube Shorts / Instagram Reels ----------
    private var dxAt = 0L
    private var dxLastHit = 0L
    private var dxHits = 0
    private val ytIds = listOf(
        "reel_recycler", "reel_player_page_container", "reel_watch_fragment_root", "shorts_player_page_container"
    )
    private val igIds = listOf(
        "clips_viewer_view_pager", "clips_viewer_container", "clips_swipe_refresh_container",
        "root_clips_layout", "clips_video_container"
    )

    private fun checkDetox(pkg: String) {
        val yt = pkg == "com.google.android.youtube"
        val ig = pkg == "com.instagram.android"
        if (!yt && !ig) return
        if (yt && !Store.detoxShorts(this)) return
        if (ig && !Store.detoxReels(this)) return
        val now = SystemClock.uptimeMillis()
        if (now - dxAt < 350L) return
        dxAt = now
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return
        var hit = false
        for (n in if (yt) ytIds else igIds) {
            val l = root.findAccessibilityNodeInfosByViewId("$pkg:id/$n")
            if (l != null && l.any { it.isVisibleToUser }) {
                hit = true
                break
            }
        }
        if (!hit) return
        if (now - dxLastHit < 2500L) dxHits++ else dxHits = 1
        dxLastHit = now
        if (dxHits >= 3) {
            dxHits = 0
            performGlobalAction(GLOBAL_ACTION_HOME)
        } else {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
        Toast.makeText(this, if (yt) "Shorts blocked" else "Reels blocked", Toast.LENGTH_SHORT).show()
    }

    // Blocked app khulte hi black focus screen samne aaye
    private fun openFocus() {
        try {
            val i = Intent(this, MainActivity::class.java)
            i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            startActivity(i)
        } catch (e: Exception) {
        }
    }

    private fun homePkg(): String? {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
    }

    private fun imePkg(): String? {
        val id = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return id?.substringBefore('/')
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Notif.cancelSetup(this)
    }

    // User ne Accessibility band kar di: agar kuch enforce hona tha to reminder bhejo
    override fun onUnbind(intent: Intent?): Boolean {
        if (Store.sessionOn(this) || Store.detoxShorts(this) || Store.detoxReels(this) || Store.anyRoutineOn(this)) {
            Notif.showAccessibilityNeeded(this)
        }
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {}
}
