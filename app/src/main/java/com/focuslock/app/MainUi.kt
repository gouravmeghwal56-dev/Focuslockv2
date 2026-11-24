package com.focuslock.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.view.animation.DecelerateInterpolator
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ScrollView
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import android.widget.PopupWindow
import android.widget.NumberPicker
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Settings popup, Data & Backup aur naya Stats page (MainActivity ko chhota rakhne ke liye alag file)

// ---------- Settings: upar right corner se khulta popup ----------

fun MainActivity.showSettings() {
    val anchor = gearBtn ?: return
    val box = LinearLayout(this)
    box.orientation = LinearLayout.VERTICAL
    box.setPadding(dp(8), dp(14), dp(8), dp(10))
    val bg = GradientDrawable()
    bg.setColor(Color.WHITE)
    bg.cornerRadius = dp(28).toFloat()
    bg.setStroke(dp(1), Color.parseColor("#ECE6E3"))
    box.background = bg

    val pop = PopupWindow(box, dp(330), WRAP, true)
    pop.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
    pop.elevation = dp(12).toFloat()

    // ---- header: gear icon + Settings
    val head = LinearLayout(this)
    head.orientation = LinearLayout.HORIZONTAL
    head.gravity = Gravity.CENTER_VERTICAL
    head.setPadding(dp(14), dp(2), dp(14), dp(8))
    val hIcon = LineIconView(this, LineIconView.GEAR)
    hIcon.tint = C_DARK
    head.addView(hIcon, LinearLayout.LayoutParams(dp(24), dp(24)))
    val hTv = TextView(this)
    hTv.text = "Settings"
    hTv.textSize = 20f
    hTv.typeface = Typeface.create("sans-serif", Typeface.BOLD)
    hTv.setTextColor(C_DARK)
    hTv.setPadding(dp(10), 0, 0, 0)
    head.addView(hTv, LinearLayout.LayoutParams(WRAP, WRAP))
    box.addView(head, LinearLayout.LayoutParams(MATCH, WRAP))

    fun section(label: String) {
        val s = TextView(this)
        s.text = label
        s.textSize = 11f
        s.letterSpacing = 0.14f
        s.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        s.setTextColor(C_DIMTXT)
        s.setPadding(dp(14), dp(10), dp(14), dp(2))
        box.addView(s, LinearLayout.LayoutParams(MATCH, WRAP))
    }

    fun row(icon: Int, title: String, value: String?, dotColor: Int?, onClick: () -> Unit) {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.setPadding(dp(10), dp(9), dp(12), dp(9))

        val ib = FrameLayout(this)
        val ibg = GradientDrawable()
        ibg.setColor(Color.parseColor("#F6F1EF"))
        ibg.cornerRadius = dp(12).toFloat()
        ib.background = ibg
        val ic = LineIconView(this, icon)
        ic.tint = C_DARK
        ib.addView(ic, FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
        val ibLp = LinearLayout.LayoutParams(dp(38), dp(38))
        ibLp.setMargins(0, 0, dp(12), 0)
        r.addView(ib, ibLp)

        val t = TextView(this)
        t.text = title
        t.textSize = 16f
        t.setTextColor(C_DARK)
        r.addView(t, LinearLayout.LayoutParams(0, WRAP, 1f))
        if (dotColor != null) {
            val dot = View(this)
            val dg = GradientDrawable()
            dg.shape = GradientDrawable.OVAL
            dg.setColor(dotColor)
            dot.background = dg
            val dl = LinearLayout.LayoutParams(dp(8), dp(8))
            dl.setMargins(0, 0, dp(6), 0)
            r.addView(dot, dl)
        }
        if (!value.isNullOrBlank()) {
            val v = TextView(this)
            v.text = value
            v.textSize = 13f
            v.setTextColor(C_DIMTXT)
            r.addView(v, LinearLayout.LayoutParams(WRAP, WRAP))
        }
        val ch = TextView(this)
        ch.text = "\u203A"
        ch.textSize = 22f
        ch.setTextColor(C_DIMTXT)
        ch.setPadding(dp(8), 0, 0, 0)
        r.addView(ch, LinearLayout.LayoutParams(WRAP, WRAP))
        val rb = GradientDrawable()
        rb.setColor(Color.TRANSPARENT)
        rb.cornerRadius = dp(16).toFloat()
        r.background = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(Color.parseColor("#1AFF6A0D")), rb, null
        )
        r.setOnClickListener {
            pop.dismiss()
            onClick()
        }
        box.addView(r, LinearLayout.LayoutParams(MATCH, WRAP))
    }

    fun divider() {
        val d = View(this)
        d.setBackgroundColor(Color.parseColor("#F1ECEA"))
        val l = LinearLayout.LayoutParams(MATCH, dp(1))
        l.setMargins(dp(14), dp(6), dp(14), dp(2))
        box.addView(d, l)
    }

    val name = Store.profileName(this)
    section("YOU")
    row(LineIconView.PERSON, "Profile", if (name.isNullOrBlank()) "Add name" else name, null) { showProfile() }
    row(LineIconView.PALETTE, "Theme & Appearance", null, null) { showThemePicker() }

    divider()
    section("BLOCKING")
    row(LineIconView.BELL, "Notifications", null, null) {
        try {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            )
        } catch (e: Exception) {
            toast("Could not open notification settings")
        }
    }
    val accOn = accEnabled()
    row(
        LineIconView.ACCESS,
        "Accessibility",
        if (accOn) "ON" else "OFF",
        if (accOn) Color.parseColor("#2E9E5B") else C_ORANGE
    ) {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    divider()
    section("DATA")
    row(LineIconView.BACKUP, "Backup data", null, null) { startBackup() }
    row(LineIconView.RESTORE, "Restore data", null, null) { startRestore() }

    divider()
    row(LineIconView.INFO, "About FocusLock", null, null) { showAbout() }

    pop.showAsDropDown(anchor, 0, dp(6), Gravity.END)
}

fun MainActivity.showAbout() {
    val ver = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
    } catch (e: Exception) {
        "1.0"
    }
    AlertDialog.Builder(this)
        .setTitle("About FocusLock")
        .setMessage("FocusLock\nVersion $ver\n\nBlock distractions, stay focused, build your streak.")
        .setPositiveButton("OK", null)
        .show()
}

fun MainActivity.showBackupMenu() {
    AlertDialog.Builder(this)
        .setTitle("Data & Backup")
        .setItems(arrayOf("Backup Data", "Restore Data")) { _, i ->
            if (i == 0) startBackup() else startRestore()
        }
        .show()
}

fun MainActivity.startBackup() {
    val fname = "focuslock-backup-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) + ".json"
    try {
        val i = Intent(Intent.ACTION_CREATE_DOCUMENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "application/json"
        i.putExtra(Intent.EXTRA_TITLE, fname)
        startActivityForResult(i, REQ_BACKUP)
    } catch (e: Exception) {
        toast("File picker nahi khula")
    }
}

fun MainActivity.startRestore() {
    if (Store.active(this)) {
        toast("Session chal rahi hai, restore baad mein karo")
        return
    }
    AlertDialog.Builder(this)
        .setTitle("Restore Data")
        .setMessage("Backup file ka data abhi ke data ki jagah aa jayega. Continue?")
        .setPositiveButton("Choose file") { _, _ ->
            try {
                val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.type = "*/*"
                startActivityForResult(i, REQ_RESTORE)
            } catch (e: Exception) {
                toast("File picker nahi khula")
            }
        }
        .setNegativeButton("Cancel", null)
        .show()
}

/** Saari saved settings/data (SharedPreferences) ek JSON file mein. */
fun MainActivity.backupJson(): String {
    val prefs = JSONObject()
    val dir = java.io.File(applicationInfo.dataDir, "shared_prefs")
    val files = dir.listFiles() ?: emptyArray()
    for (f in files) {
        if (!f.name.endsWith(".xml")) continue
        val pname = f.name.removeSuffix(".xml")
        val sp = getSharedPreferences(pname, android.content.Context.MODE_PRIVATE)
        val o = JSONObject()
        for ((k, v) in sp.all) {
            val e = JSONObject()
            when (v) {
                is Boolean -> { e.put("t", "b"); e.put("v", v) }
                is Int -> { e.put("t", "i"); e.put("v", v) }
                is Long -> { e.put("t", "l"); e.put("v", v) }
                is Float -> { e.put("t", "f"); e.put("v", v.toDouble()) }
                is String -> { e.put("t", "s"); e.put("v", v) }
                is Set<*> -> {
                    val a = JSONArray()
                    for (x in v) a.put(x.toString())
                    e.put("t", "set"); e.put("v", a)
                }
                else -> continue
            }
            o.put(k, e)
        }
        prefs.put(pname, o)
    }
    val root = JSONObject()
    root.put("app", "FocusLock")
    root.put("version", 1)
    root.put("prefs", prefs)
    return root.toString()
}

fun MainActivity.restoreJson(text: String): Boolean {
    return try {
        val root = JSONObject(text)
        if (root.optString("app") != "FocusLock") return false
        val prefs = root.getJSONObject("prefs")
        val names = prefs.keys()
        while (names.hasNext()) {
            val pname = names.next()
            val o = prefs.getJSONObject(pname)
            val ed = getSharedPreferences(pname, android.content.Context.MODE_PRIVATE).edit()
            ed.clear()
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val e = o.getJSONObject(k)
                when (e.getString("t")) {
                    "b" -> ed.putBoolean(k, e.getBoolean("v"))
                    "i" -> ed.putInt(k, e.getInt("v"))
                    "l" -> ed.putLong(k, e.getLong("v"))
                    "f" -> ed.putFloat(k, e.getDouble("v").toFloat())
                    "s" -> ed.putString(k, e.getString("v"))
                    "set" -> {
                        val a = e.getJSONArray("v")
                        val s = HashSet<String>()
                        for (i in 0 until a.length()) s.add(a.getString(i))
                        ed.putStringSet(k, s)
                    }
                }
            }
            ed.commit()
        }
        true
    } catch (e: Exception) {
        false
    }
}

// ---------- Page 3: Stats (naya design: goal, streak, chart, goal completion) ----------
fun MainActivity.stGoal(): Int = getSharedPreferences("fl_ui", android.content.Context.MODE_PRIVATE).getInt("goal_min", 180)

fun MainActivity.stTrack(): Int = (C_TXT and 0x00FFFFFF) or (0x1A shl 24)

const val STREAK_MIN_MINUTES = 30   // ek din ki streak ke liye kam se kam itne minute
const val FREEZE_CAP = 5            // ek saath maximum itne frozen streak

class StreakInfo(val streak: Int, val best: Int, val freezes: Int)

/**
 * Streak hisaab (din ke hisaab se, purane se naye):
 * - Din ki streak tab bante jab us din kam se kam 30 min focus ho.
 * - Har Sunday ko 1 frozen streak milti hai (max FREEZE_CAP).
 * - Streak chal rahi ho aur din miss ho: frozen streak ho to apne aap use ho jati hai (streak bani rehti hai,
 *   par wo din gine nahi jate). Frozen na ho to streak 0, agle din se 1.
 * - Aaj abhi 30 min nahi hue to aaj ko miss nahi ginte.
 */
fun MainActivity.stStreakInfo(): StreakInfo {
    val today = Calendar.getInstance()
    var started = false
    var streak = 0
    var best = 0
    var freezes = 0
    for (i in 1095 downTo 0) {
        val c = today.clone() as Calendar
        c.add(Calendar.DAY_OF_YEAR, -i)
        val m = Store.dayMinutes(this, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
        if (!started) {
            if (m <= 0L) continue
            started = true
        }
        if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) freezes = minOf(FREEZE_CAP, freezes + 1)
        if (m >= STREAK_MIN_MINUTES) {
            streak++
            if (streak > best) best = streak
        } else if (i > 0 && streak > 0) {
            if (freezes > 0) freezes-- else streak = 0
        }
    }
    return StreakInfo(streak, best, freezes)
}

/** Ab tak ka sabse lamba streak. */
fun MainActivity.stBestStreak(): Int = stStreakInfo().best

/** Scope (din/mahina/saal) ki sessions aur un dino ke minutes (jinki sessions count hui). */
fun MainActivity.stSessions(): Pair<Int, Int> {
    val y = stCal.get(Calendar.YEAR)
    val mo = stCal.get(Calendar.MONTH)
    var ses = 0
    var mins = 0
    fun day(yy: Int, mm: Int, dd: Int) {
        val s = Store.daySessions(this, yy, mm, dd)
        if (s > 0) {
            ses += s
            mins += Store.dayMinutes(this, yy, mm, dd).toInt()
        }
    }
    if (stMode == 0) {
        day(y, mo, stCal.get(Calendar.DAY_OF_MONTH))
    } else {
        val months = if (stMode == 2) 0..11 else mo..mo
        for (mm in months) {
            val c = Calendar.getInstance()
            c.clear()
            c.set(y, mm, 1)
            for (dd in 1..c.getActualMaximum(Calendar.DAY_OF_MONTH)) day(y, mm, dd)
        }
    }
    return Pair(ses, mins)
}

fun MainActivity.stText(s: CharSequence, sp: Float, color: Int, bold: Boolean = false): TextView {
    val t = TextView(this)
    t.text = s
    t.textSize = sp
    t.setTextColor(color)
    if (bold) t.typeface = Typeface.create("sans-serif", Typeface.BOLD)
    return t
}

fun MainActivity.stCard(): LinearLayout {
    val l = LinearLayout(this)
    l.orientation = LinearLayout.VERTICAL
    l.background = tileBg(false)
    l.setPadding(dp(16), dp(12), dp(16), dp(12))
    return l
}

fun MainActivity.goalDialog() {
    val cur = stGoal()
    val ph = NumberPicker(this)
    ph.minValue = 0
    ph.maxValue = 12
    ph.value = (cur / 60).coerceIn(0, 12)
    val pm = NumberPicker(this)
    pm.minValue = 0
    pm.maxValue = 11
    pm.displayedValues = Array(12) { String.format("%02d", it * 5) }
    pm.value = ((cur % 60) / 5).coerceIn(0, 11)
    val row = LinearLayout(this)
    row.orientation = LinearLayout.HORIZONTAL
    row.gravity = Gravity.CENTER
    row.setPadding(dp(20), dp(12), dp(20), 0)
    fun lbl(t: String): TextView {
        val tv = TextView(this)
        tv.text = t
        tv.textSize = 16f
        tv.setPadding(dp(8), 0, dp(16), 0)
        return tv
    }
    row.addView(ph)
    row.addView(lbl("h"))
    row.addView(pm)
    row.addView(lbl("min"))
    AlertDialog.Builder(this)
        .setTitle("Daily Focus Goal")
        .setView(row)
        .setPositiveButton("Save") { _, _ ->
            var total = ph.value * 60 + pm.value * 5
            if (total < 5) total = 5
            getSharedPreferences("fl_ui", android.content.Context.MODE_PRIVATE).edit().putInt("goal_min", total).apply()
            refreshStats()
        }
        .setNegativeButton("Cancel", null)
        .show()
}

private val CH_LEVELS = intArrayOf(3, 7, 14, 30, 50, 100, 365)
private fun chLevelName(d: Int): String = when (d) {
    3 -> "Spark"
    7 -> "Week Warrior"
    14 -> "Momentum"
    30 -> "Iron Month"
    50 -> "Deep Work"
    100 -> "Century Club"
    365 -> "Year of Focus"
    else -> "My Challenge"
}

private fun MainActivity.chPrefs() = getSharedPreferences("fl_ui", android.content.Context.MODE_PRIVATE)

/** Challenge ke steps, jaise [3, 7, 14, 30]. Ek step khatam hote hi agla apne aap chalu. */
fun MainActivity.stChallengeSteps(): List<Int> {
    val p = chPrefs()
    val raw = p.getString("ch_steps", null)
    if (!raw.isNullOrBlank()) {
        val l = raw.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..365 }
        if (l.isNotEmpty()) return l
    }
    val old = p.getInt("ch_days", 0)   // purana save kiya hua challenge
    return if (old > 0) listOf(old) else emptyList()
}

fun MainActivity.stChallengeStart(): Long = chPrefs().getLong("ch_start", 0L)

private fun MainActivity.chDayNo(): Int {
    val start = stChallengeStart()
    if (start <= 0L) return 0
    val tc = Calendar.getInstance()
    tc.set(Calendar.HOUR_OF_DAY, 0)
    tc.set(Calendar.MINUTE, 0)
    tc.set(Calendar.SECOND, 0)
    tc.set(Calendar.MILLISECOND, 0)
    return Math.round((tc.timeInMillis - start) / 86400000.0).toInt() + 1
}

/** Abhi chal raha step (3 pura hua to 7, phir 14...). Start date wahi rehti hai, din 4 se aage gine jaate hain. */
fun MainActivity.stChallengeDays(): Int {
    val steps = stChallengeSteps()
    if (steps.isEmpty() || stChallengeStart() <= 0L) return 0
    val dayNo = chDayNo()
    return steps.firstOrNull { dayNo <= it } ?: steps.last()
}

/** Challenge ka naam + level, jaise "Week Warrior · Level 2/7". */
fun MainActivity.stChallengeName(): String {
    val steps = stChallengeSteps()
    val cur = stChallengeDays()
    val custom = chPrefs().getBoolean("ch_custom", false)
    val name = if (custom) "My Challenge" else chLevelName(cur)
    val lvl = steps.indexOf(cur) + 1
    return if (steps.size > 1) "$name \u00B7 Level $lvl/${steps.size}" else name
}

/** Challenge aaj se shuru. steps = [3,7,14...] (khali = band, wapas "This month"). */
fun MainActivity.startChallenge(steps: List<Int>, custom: Boolean = false) {
    val ed = chPrefs().edit()
    if (steps.isEmpty()) {
        ed.remove("ch_days").remove("ch_start").remove("ch_steps").remove("ch_custom")
    } else {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        ed.remove("ch_days")
        ed.putString("ch_steps", steps.joinToString(","))
        ed.putBoolean("ch_custom", custom)
        ed.putLong("ch_start", c.timeInMillis)
    }
    ed.apply()
    refreshStats()
}

fun MainActivity.customChallengeDialog() {
    val et = EditText(this)
    et.inputType = InputType.TYPE_CLASS_TEXT
    et.hint = "Jaise 10  ya  5, 15, 40"
    val note = TextView(this)
    note.text = "Apne din likho (1 - 365). Ek se zyada likho to ek khatam hote hi agla apne aap chalu ho jayega, gine hue din wahi rahenge."
    note.textSize = 13f
    val box = LinearLayout(this)
    box.orientation = LinearLayout.VERTICAL
    box.setPadding(dp(20), dp(8), dp(20), 0)
    box.addView(note, LinearLayout.LayoutParams(MATCH, WRAP))
    box.addView(et, LinearLayout.LayoutParams(MATCH, WRAP))
    AlertDialog.Builder(this)
        .setTitle("Custom challenge")
        .setView(box)
        .setPositiveButton("Start") { _, _ ->
            val l = et.text.toString().split(",", " ", ";")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..365 }
                .distinct().sorted().take(8)
            if (l.isEmpty()) toast("1 se 365 ke beech din likho") else startChallenge(l, true)
        }
        .setNegativeButton("Cancel", null)
        .show()
}

fun MainActivity.challengeDialog() {
    val items = ArrayList<String>()
    items.add("Custom...")
    items.add("This month (default)")
    for (d in CH_LEVELS) items.add(chLevelName(d) + "  \u00B7  " + d + " days")
    items.add("Daily goal...")
    AlertDialog.Builder(this)
        .setTitle("Challenge (aaj se shuru)")
        .setItems(items.toTypedArray()) { _, i ->
            when {
                i == 0 -> customChallengeDialog()
                i == 1 -> startChallenge(emptyList())
                i == items.size - 1 -> goalDialog()
                else -> {
                    // chuna hua level + uske upar ke saare levels (pura hone par agla apne aap)
                    val start = CH_LEVELS[i - 2]
                    startChallenge(CH_LEVELS.filter { it >= start })
                }
            }
        }
        .show()
}

fun MainActivity.stIcon(kind: Int, tint: Int): LineIconView {
    val v = LineIconView(this, kind)
    v.tint = tint
    return v
}

fun MainActivity.statsPage(): View {
    val sv = ScrollView(this)
    sv.isVerticalScrollBarEnabled = false
    sv.overScrollMode = View.OVER_SCROLL_NEVER
    val col = LinearLayout(this)
    col.orientation = LinearLayout.VERTICAL
    col.setPadding(dp(2), dp(2), dp(2), dp(8))
    sv.addView(col, FrameLayout.LayoutParams(MATCH, WRAP))

    fun rowLp(h: Int): LinearLayout.LayoutParams {
        val l = LinearLayout.LayoutParams(MATCH, h)
        l.setMargins(0, dp(10), 0, 0)
        return l
    }

    val dm = resources.displayMetrics
    val wide = dm.widthPixels / dm.density >= 600f
    val orangeSoft = (C_ORANGE and 0x00FFFFFF) or (0x26 shl 24)

    val now = Calendar.getInstance()
    val nowMs = now.timeInMillis
    val goal = stGoal()
    val todayMin = Store.dayMinutes(this, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).toInt()
    val sInfo = stStreakInfo()
    val streak = sInfo.streak
    val bestStreak = maxOf(sInfo.best, streak)

    // ---- title
    val title = stText("Stats", 26f, C_TXT, true)
    title.gravity = Gravity.CENTER
    col.addView(title, LinearLayout.LayoutParams(MATCH, WRAP))
    val sub = stText("Your focus progress at a glance", 13f, C_DIMTXT)
    sub.gravity = Gravity.CENTER
    col.addView(sub, LinearLayout.LayoutParams(MATCH, WRAP))

    // ================= Daily Focus Goal + Streak =================
    val top = LinearLayout(this)
    top.orientation = LinearLayout.HORIZONTAL

    val gc = stCard()
    val gHead = LinearLayout(this)
    gHead.orientation = LinearLayout.HORIZONTAL
    gHead.gravity = Gravity.CENTER_VERTICAL
    gHead.addView(stIcon(LineIconView.TARGET, C_ORANGE), LinearLayout.LayoutParams(dp(22), dp(22)))
    val gT = stText("Daily Focus Goal", 15f, C_TXT, true)
    gT.setPadding(dp(10), 0, 0, 0)
    gHead.addView(gT, LinearLayout.LayoutParams(0, WRAP, 1f))
    val edit = FrameLayout(this)
    edit.background = rounded(orangeSoft, 12)
    edit.addView(stIcon(LineIconView.PENCIL, C_ORANGE), FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER))
    edit.setOnClickListener { goalDialog() }
    gHead.addView(edit, LinearLayout.LayoutParams(dp(36), dp(36)))
    gc.addView(gHead, LinearLayout.LayoutParams(MATCH, WRAP))

    val gv = SpannableStringBuilder(hm(todayMin))
    gv.setSpan(AbsoluteSizeSpan(26, true), 0, gv.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    gv.setSpan(StyleSpan(Typeface.BOLD), 0, gv.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    val st0 = gv.length
    gv.append(" / " + hm(goal))
    gv.setSpan(AbsoluteSizeSpan(16, true), st0, gv.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    gv.setSpan(ForegroundColorSpan(C_DIMTXT), st0, gv.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    val gvLp = LinearLayout.LayoutParams(WRAP, WRAP)
    gvLp.topMargin = dp(6)
    gc.addView(stText(gv, 16f, C_TXT), gvLp)

    val pr = LinearLayout(this)
    pr.orientation = LinearLayout.HORIZONTAL
    pr.gravity = Gravity.CENTER_VERTICAL
    val pb = ProgressBarView(this)
    pb.fillColor = C_ORANGE
    pb.trackColor = stTrack()
    pb.frac = if (goal > 0) todayMin.toFloat() / goal else 0f
    pr.addView(pb, LinearLayout.LayoutParams(0, dp(10), 1f))
    val pct = if (goal > 0) minOf(100, todayMin * 100 / goal) else 0
    val pctTv = stText("$pct%", 14f, C_TXT, true)
    pctTv.setPadding(dp(12), 0, 0, 0)
    pr.addView(pctTv, LinearLayout.LayoutParams(WRAP, WRAP))
    val prLp = LinearLayout.LayoutParams(MATCH, WRAP)
    prLp.topMargin = dp(8)
    gc.addView(pr, prLp)

    val sc = stCard()
    val sHead = LinearLayout(this)
    sHead.orientation = LinearLayout.HORIZONTAL
    sHead.gravity = Gravity.CENTER_VERTICAL
    sHead.addView(FlameView(this, streak > 0), LinearLayout.LayoutParams(dp(26), dp(34)))
    val sTitle = stText("Streak", 15f, C_TXT, true)
    sTitle.setPadding(dp(8), 0, 0, 0)
    sHead.addView(sTitle, LinearLayout.LayoutParams(WRAP, WRAP))
    sc.addView(sHead, LinearLayout.LayoutParams(MATCH, WRAP))
    sc.addView(stText(if (streak == 1) "1 day" else "$streak days", 26f, C_TXT, true), LinearLayout.LayoutParams(WRAP, WRAP))
    sc.addView(stText("Best: " + (if (bestStreak == 1) "1 day" else "$bestStreak days"), 13f, C_DIMTXT), LinearLayout.LayoutParams(WRAP, WRAP))
    sc.addView(
        stText("\u2744 " + sInfo.freezes + (if (sInfo.freezes == 1) " freeze" else " freezes") + "  \u00B7  +1 every Sunday", 12f, C_DIMTXT),
        LinearLayout.LayoutParams(WRAP, WRAP)
    )

    val gl = LinearLayout.LayoutParams(0, MATCH, 1.7f)
    gl.setMargins(0, 0, dp(8), 0)
    top.addView(gc, gl)
    top.addView(sc, LinearLayout.LayoutParams(0, MATCH, 1f))
    col.addView(top, rowLp(WRAP).also { it.topMargin = dp(14) })

    // ================= < Day | Month | Year > =================
    val probe = stCal.clone() as Calendar
    when (stMode) {
        0 -> probe.add(Calendar.DAY_OF_YEAR, 1)
        1 -> { probe.set(Calendar.DAY_OF_MONTH, 1); probe.add(Calendar.MONTH, 1) }
        else -> { probe.set(Calendar.MONTH, 0); probe.set(Calendar.DAY_OF_MONTH, 1); probe.add(Calendar.YEAR, 1) }
    }
    val segRow = LinearLayout(this)
    segRow.orientation = LinearLayout.HORIZONTAL
    segRow.gravity = Gravity.CENTER

    val prev = stText("\u2039", 28f, C_TXT)
    prev.gravity = Gravity.CENTER
    prev.setPadding(dp(16), 0, dp(16), dp(3))
    prev.setOnClickListener { stShift(-1) }
    segRow.addView(prev, LinearLayout.LayoutParams(WRAP, WRAP))

    val seg = LinearLayout(this)
    seg.orientation = LinearLayout.HORIZONTAL
    seg.gravity = Gravity.CENTER
    seg.setPadding(dp(3), dp(3), dp(3), dp(3))
    seg.background = rounded(stTrack(), 24)
    for ((i, n) in listOf("Day", "Month", "Year").withIndex()) {
        val on = stMode == i
        val p = stText(n, 15f, if (on) Color.WHITE else C_TXT, on)
        p.gravity = Gravity.CENTER
        p.setPadding(dp(22), dp(9), dp(22), dp(9))
        if (on) p.background = rounded(C_ORANGE, 22)
        p.setOnClickListener { stSetMode(i) }
        seg.addView(p, LinearLayout.LayoutParams(WRAP, WRAP))
    }
    segRow.addView(seg, LinearLayout.LayoutParams(WRAP, WRAP))

    val next = stText("\u203A", 28f, C_TXT)
    next.gravity = Gravity.CENTER
    next.setPadding(dp(16), 0, dp(16), dp(3))
    next.alpha = if (probe.timeInMillis > nowMs) 0.3f else 1f
    next.setOnClickListener { stShift(1) }
    segRow.addView(next, LinearLayout.LayoutParams(WRAP, WRAP))
    col.addView(segRow, rowLp(WRAP))

    // ================= chart card =================
    val y = stCal.get(Calendar.YEAR)
    val m = stCal.get(Calendar.MONTH)
    var vals = IntArray(0)
    var labels: Array<String> = emptyArray()
    var future = Int.MAX_VALUE
    var sel = -1
    var pick: ((Int) -> Unit)? = null
    var chartTitle = ""
    var headerTotal = 0

    when (stMode) {
        0 -> {
            val c = stCal.clone() as Calendar
            val dow = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7   // Mon = 0
            c.add(Calendar.DAY_OF_YEAR, -dow)
            vals = IntArray(7)
            val starts = LongArray(7)
            for (i in 0 until 7) {
                val d = c.clone() as Calendar
                d.add(Calendar.DAY_OF_YEAR, i)
                d.set(Calendar.HOUR_OF_DAY, 0)
                d.set(Calendar.MINUTE, 0)
                d.set(Calendar.SECOND, 0)
                d.set(Calendar.MILLISECOND, 0)
                starts[i] = d.timeInMillis
                if (d.timeInMillis > nowMs) {
                    if (future == Int.MAX_VALUE) future = i
                } else {
                    vals[i] = Store.dayMinutes(this, d.get(Calendar.YEAR), d.get(Calendar.MONTH), d.get(Calendar.DAY_OF_MONTH)).toInt()
                }
            }
            labels = arrayOf("M", "T", "W", "T", "F", "S", "S")
            sel = dow
            pick = { i ->
                val cp = Calendar.getInstance()
                cp.timeInMillis = starts[i]
                stCal = cp
                refreshStats()
            }
            val weekMs = 7L * 24L * 3600L * 1000L
            chartTitle = if (nowMs >= starts[0] && nowMs < starts[0] + weekMs) "This Week"
            else SimpleDateFormat("'Week of' d MMM", Locale.getDefault()).format(Date(starts[0]))
            headerTotal = vals.sum()
        }
        1 -> {
            val c = Calendar.getInstance()
            c.clear()
            c.set(y, m, 1)
            val n = c.getActualMaximum(Calendar.DAY_OF_MONTH)
            vals = IntArray(n) { Store.dayMinutes(this, y, m, it + 1).toInt() }
            if (y == now.get(Calendar.YEAR) && m == now.get(Calendar.MONTH)) future = now.get(Calendar.DAY_OF_MONTH)
            labels = Array(n) { i -> val dd = i + 1; if (dd == 1 || dd % 5 == 0) dd.toString() else "" }
            pick = { i ->
                stCal = Calendar.getInstance().also { it.clear(); it.set(y, m, i + 1, 0, 0) }
                stMode = 0
                refreshStats()
            }
            chartTitle = if (y == now.get(Calendar.YEAR) && m == now.get(Calendar.MONTH)) "This Month"
            else SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(stCal.time)
            headerTotal = stTotal()
        }
        else -> {
            vals = IntArray(12) { monthSum(y, it) }
            if (y == now.get(Calendar.YEAR)) future = now.get(Calendar.MONTH) + 1
            labels = arrayOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
            pick = { i ->
                stCal = Calendar.getInstance().also { it.clear(); it.set(y, i, 1) }
                if (y == now.get(Calendar.YEAR) && i == now.get(Calendar.MONTH)) stCal = Calendar.getInstance()
                stMode = 1
                refreshStats()
            }
            chartTitle = if (y == now.get(Calendar.YEAR)) "This Year" else y.toString()
            headerTotal = stTotal()
        }
    }

    val chart = BarChartView(this)
    chart.barColor = C_ORANGE
    chart.trackColor = stTrack()
    chart.textColor = C_TXT
    chart.values = vals
    chart.labels = labels
    chart.futureFrom = future
    chart.selected = sel
    chart.onPick = pick

    val cHead = LinearLayout(this)
    cHead.orientation = LinearLayout.HORIZONTAL
    cHead.gravity = Gravity.CENTER_VERTICAL
    cHead.addView(stIcon(LineIconView.BARS, C_ORANGE), LinearLayout.LayoutParams(dp(20), dp(20)))
    val cT = stText(chartTitle, 15f, C_TXT, true)
    cT.setPadding(dp(10), dp(4), dp(10), dp(4))
    cT.setOnClickListener { monthPickerDialog() }
    cHead.addView(cT, LinearLayout.LayoutParams(0, WRAP, 1f))
    cHead.addView(stText(hm(headerTotal), 16f, C_TXT, true), LinearLayout.LayoutParams(WRAP, WRAP))

    val chartCol = LinearLayout(this)
    chartCol.orientation = LinearLayout.VERTICAL
    chartCol.addView(cHead, LinearLayout.LayoutParams(MATCH, WRAP))
    val chLp = LinearLayout.LayoutParams(MATCH, dp(if (wide) 180 else 160))
    chLp.topMargin = dp(10)
    chartCol.addView(chart, chLp)

    // right side: Total / Sessions / Avg (icon ke saath)
    val sp = stSessions()
    val firstLbl = if (stMode == 0) (if (stThisPeriodWord() == "TODAY") "Today" else "Selected day") else "Total Focus"
    val numRows = listOf(
        Triple(LineIconView.STOPWATCH, firstLbl, hm(stTotal())),
        Triple(LineIconView.LAYERS, "Sessions", sp.first.toString()),
        Triple(LineIconView.BARS, "Avg. per Session", if (sp.first > 0) hm(sp.second / sp.first) else "\u2014")
    )
    val rowsBox = LinearLayout(this)
    rowsBox.orientation = LinearLayout.VERTICAL
    rowsBox.gravity = Gravity.CENTER_VERTICAL
    for ((i, r) in numRows.withIndex()) {
        val one = LinearLayout(this)
        one.orientation = LinearLayout.HORIZONTAL
        one.gravity = Gravity.CENTER_VERTICAL
        one.setPadding(0, dp(11), 0, dp(11))
        one.addView(stIcon(r.first, C_ORANGE), LinearLayout.LayoutParams(dp(22), dp(22)))
        val k = stText(r.second, 14f, C_TXT)
        k.setPadding(dp(12), 0, dp(8), 0)
        one.addView(k, LinearLayout.LayoutParams(0, WRAP, 1f))
        one.addView(stText(r.third, 16f, C_TXT, true), LinearLayout.LayoutParams(WRAP, WRAP))
        rowsBox.addView(one, LinearLayout.LayoutParams(MATCH, WRAP))
        if (i < numRows.size - 1) {
            val ln = View(this)
            ln.setBackgroundColor(stTrack())
            rowsBox.addView(ln, LinearLayout.LayoutParams(MATCH, dp(1)))
        }
    }

    val cc = stCard()
    cc.setPadding(dp(18), dp(14), dp(18), dp(12))
    if (wide) {
        cc.orientation = LinearLayout.HORIZONTAL
        cc.addView(chartCol, LinearLayout.LayoutParams(0, WRAP, 1.6f))
        val dv = View(this)
        dv.setBackgroundColor(stTrack())
        val dvLp = LinearLayout.LayoutParams(dp(1), MATCH)
        dvLp.setMargins(dp(16), dp(4), dp(16), dp(4))
        cc.addView(dv, dvLp)
        cc.addView(rowsBox, LinearLayout.LayoutParams(0, MATCH, 1f))
    } else {
        cc.addView(chartCol, LinearLayout.LayoutParams(MATCH, WRAP))
        val dv = View(this)
        dv.setBackgroundColor(stTrack())
        val dvLp = LinearLayout.LayoutParams(MATCH, dp(1))
        dvLp.setMargins(0, dp(10), 0, dp(2))
        cc.addView(dv, dvLp)
        cc.addView(rowsBox, LinearLayout.LayoutParams(MATCH, WRAP))
    }
    col.addView(cc, rowLp(WRAP))

    // ================= Goal Completion (custom goal button ke saath) =================
    var met = 0
    var elapsed = 0
    var states = IntArray(0)
    val chDays = stChallengeDays()
    val chStart = stChallengeStart()
    var gTitle = "Goal Completion"
    var gSub: String
    var gCountText: String
    var gFrac = 0f
    var monthDays = 0
    val useBar: Boolean
    if (chDays > 0 && chStart > 0L) {
        // ---- challenge mode: level ladder (3 -> 7 -> 14 ...) ya custom
        useBar = chDays > 45
        states = IntArray(chDays)
        val base = Calendar.getInstance()
        base.timeInMillis = chStart
        val tc = Calendar.getInstance()
        tc.set(Calendar.HOUR_OF_DAY, 0)
        tc.set(Calendar.MINUTE, 0)
        tc.set(Calendar.SECOND, 0)
        tc.set(Calendar.MILLISECOND, 0)
        val dayNo = Math.round((tc.timeInMillis - chStart) / 86400000.0).toInt() + 1
        for (i in 0 until chDays) {
            val d = base.clone() as Calendar
            d.add(Calendar.DAY_OF_YEAR, i)
            if (d.timeInMillis > nowMs) {
                states[i] = 2
            } else {
                val ok = Store.dayMinutes(this, d.get(Calendar.YEAR), d.get(Calendar.MONTH), d.get(Calendar.DAY_OF_MONTH)) >= goal
                if (ok) {
                    met++
                    states[i] = 1
                }
                // aaj ka din tab tak "miss" nahi ginte jab tak goal poora na ho
                if (ok || i != dayNo - 1) elapsed++
            }
        }
        gTitle = stChallengeName() + "  \u00B7  $chDays days"
        gSub = if (dayNo > chDays) "Completed  \u00B7  goal " + hm(goal) + "/day"
        else "Day $dayNo of $chDays  \u00B7  goal " + hm(goal) + "/day"
        gFrac = met.toFloat() / chDays
        gCountText = "$met / $chDays days  \u00B7  " + (met * 100 / chDays) + "%"
    } else {
        // ---- default: pura mahina / saal
        useBar = stMode == 2
        if (stMode == 2) {
            for (mm in 0..11) {
                val c = Calendar.getInstance()
                c.clear()
                c.set(y, mm, 1)
                for (d in 1..c.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                    c.set(y, mm, d)
                    if (c.timeInMillis > nowMs) break
                    elapsed++
                    if (Store.dayMinutes(this, y, mm, d) >= goal) met++
                }
            }
        } else {
            val c = Calendar.getInstance()
            c.clear()
            c.set(y, m, 1)
            val n = c.getActualMaximum(Calendar.DAY_OF_MONTH)
            monthDays = n
            states = IntArray(n)
            for (d in 1..n) {
                c.set(y, m, d)
                if (c.timeInMillis > nowMs) {
                    states[d - 1] = 2
                } else {
                    elapsed++
                    if (Store.dayMinutes(this, y, m, d) >= goal) {
                        met++
                        states[d - 1] = 1
                    }
                }
            }
        }
        val period = if (stMode == 2) {
            if (y == now.get(Calendar.YEAR)) "This year" else y.toString()
        } else {
            if (y == now.get(Calendar.YEAR) && m == now.get(Calendar.MONTH)) "This month"
            else SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(stCal.time)
        }
        val denom = if (stMode != 2 && monthDays > 0) monthDays else elapsed   // mahine me poore din
        val gp = if (denom > 0) met * 100 / denom else 0
        gSub = period + "  \u00B7  goal " + hm(goal) + "/day"
        gFrac = if (denom > 0) met.toFloat() / denom else 0f
        gCountText = "$met / $denom days  \u00B7  $gp%"
    }

    val gIcon = stIcon(LineIconView.CHECK, C_ORANGE)
    val gLeft = LinearLayout(this)
    gLeft.orientation = LinearLayout.VERTICAL
    gLeft.addView(stText(gTitle, 15f, C_TXT, true), LinearLayout.LayoutParams(WRAP, WRAP))
    gLeft.addView(stText(gSub, 12f, C_DIMTXT), LinearLayout.LayoutParams(WRAP, WRAP))
    val gCount = stText(gCountText, 14f, C_TXT, true)

    val custom = FrameLayout(this)
    custom.background = rounded(orangeSoft, 20)
    custom.addView(stIcon(LineIconView.SLIDERS, C_ORANGE), FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
    custom.setOnClickListener { challengeDialog() }

    val gBar: View
    if (useBar) {
        val pbv = ProgressBarView(this)
        pbv.fillColor = C_ORANGE
        pbv.trackColor = stTrack()
        pbv.frac = gFrac
        gBar = pbv
    } else {
        val dvw = GoalDotsView(this)
        dvw.fillColor = C_ORANGE
        dvw.trackColor = stTrack()
        dvw.states = states
        gBar = dvw
    }

    val gcard = stCard()
    gcard.setPadding(dp(18), dp(14), dp(14), dp(14))
    if (wide) {
        val line = LinearLayout(this)
        line.orientation = LinearLayout.HORIZONTAL
        line.gravity = Gravity.CENTER_VERTICAL
        line.addView(gIcon, LinearLayout.LayoutParams(dp(26), dp(26)))
        val glLp = LinearLayout.LayoutParams(WRAP, WRAP)
        glLp.setMargins(dp(12), 0, dp(16), 0)
        line.addView(gLeft, glLp)
        val barLp = LinearLayout.LayoutParams(0, if (useBar) dp(10) else dp(18), 1f)
        barLp.setMargins(0, 0, dp(16), 0)
        line.addView(gBar, barLp)
        line.addView(gCount, LinearLayout.LayoutParams(WRAP, WRAP))
        val sep = View(this)
        sep.setBackgroundColor(stTrack())
        val sepLp = LinearLayout.LayoutParams(dp(1), dp(30))
        sepLp.setMargins(dp(14), 0, dp(14), 0)
        line.addView(sep, sepLp)
        line.addView(custom, LinearLayout.LayoutParams(dp(40), dp(40)))
        gcard.addView(line, LinearLayout.LayoutParams(MATCH, WRAP))
    } else {
        val line = LinearLayout(this)
        line.orientation = LinearLayout.HORIZONTAL
        line.gravity = Gravity.CENTER_VERTICAL
        line.addView(gIcon, LinearLayout.LayoutParams(dp(26), dp(26)))
        val glLp = LinearLayout.LayoutParams(0, WRAP, 1f)
        glLp.setMargins(dp(12), 0, dp(8), 0)
        line.addView(gLeft, glLp)
        line.addView(custom, LinearLayout.LayoutParams(dp(40), dp(40)))
        gcard.addView(line, LinearLayout.LayoutParams(MATCH, WRAP))
        val cLp = LinearLayout.LayoutParams(MATCH, WRAP)
        cLp.topMargin = dp(10)
        gcard.addView(gCount, cLp)
        val barLp = LinearLayout.LayoutParams(MATCH, if (useBar) dp(10) else dp(18))
        barLp.topMargin = dp(10)
        gcard.addView(gBar, barLp)
    }
    col.addView(gcard, rowLp(WRAP))

    val foot = stText("Minutes are counted when a timer starts", 11f, C_DIMTXT)
    foot.gravity = Gravity.CENTER
    val fLp = rowLp(WRAP)
    fLp.topMargin = dp(12)
    col.addView(foot, fLp)
    return sv
}
