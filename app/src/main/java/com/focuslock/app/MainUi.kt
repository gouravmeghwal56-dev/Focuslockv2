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
    box.setPadding(dp(8), dp(10), dp(8), dp(8))
    val bg = GradientDrawable()
    bg.setColor(Color.WHITE)
    bg.cornerRadius = dp(24).toFloat()
    bg.setStroke(dp(1), Color.parseColor("#E6E0DE"))
    box.background = bg

    val head = TextView(this)
    head.text = "Settings"
    head.textSize = 13f
    head.letterSpacing = 0.1f
    head.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    head.setTextColor(C_DIMTXT)
    head.setPadding(dp(14), dp(4), dp(14), dp(6))
    box.addView(head, LinearLayout.LayoutParams(MATCH, WRAP))

    val pop = PopupWindow(box, dp(300), WRAP, true)
    pop.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))

    fun row(title: String, sub: String?, onClick: () -> Unit) {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.VERTICAL
        r.setPadding(dp(14), dp(11), dp(14), dp(11))
        val t = TextView(this)
        t.text = title
        t.textSize = 16f
        t.setTextColor(C_DARK)
        r.addView(t, LinearLayout.LayoutParams(WRAP, WRAP))
        if (!sub.isNullOrBlank()) {
            val s = TextView(this)
            s.text = sub
            s.textSize = 12f
            s.setTextColor(C_DIMTXT)
            r.addView(s, LinearLayout.LayoutParams(WRAP, WRAP))
        }
        val rb = GradientDrawable()
        rb.setColor(Color.TRANSPARENT)
        rb.cornerRadius = dp(14).toFloat()
        r.background = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(Color.parseColor("#1AFF6A0D")), rb, null
        )
        r.setOnClickListener {
            pop.dismiss()
            onClick()
        }
        box.addView(r, LinearLayout.LayoutParams(MATCH, WRAP))
    }

    val name = Store.profileName(this)
    row("Profile", if (name.isNullOrBlank()) "Add your name" else name) { showProfile() }
    row("Theme & Appearance", "Colors and backgrounds") { showThemePicker() }
    row("Notifications", "Reminders and session alerts") {
        try {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            )
        } catch (e: Exception) {
            toast("Could not open notification settings")
        }
    }
    row("Accessibility", if (accEnabled()) "Blocking is ON" else "Turn on for blocking") {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
    row("Data & Backup", "Backup and restore your data") { showBackupMenu() }
    row("About FocusLock", null) { showAbout() }

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

/** Ab tak ka sabse lamba streak (pichle ~3 saal). */
fun MainActivity.stBestStreak(): Int {
    val c = Calendar.getInstance()
    c.add(Calendar.DAY_OF_YEAR, -1095)
    val end = Calendar.getInstance()
    var best = 0
    var run = 0
    while (!c.after(end)) {
        if (Store.dayMinutes(this, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)) > 0L) {
            run++
            if (run > best) best = run
        } else {
            run = 0
        }
        c.add(Calendar.DAY_OF_YEAR, 1)
    }
    return best
}

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
        l.setMargins(0, dp(6), 0, 0)
        return l
    }

    val now = Calendar.getInstance()
    val nowMs = now.timeInMillis
    val goal = stGoal()
    val todayMin = Store.dayMinutes(this, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).toInt()
    val streak = stStreak()
    val bestStreak = maxOf(stBestStreak(), streak)

    // ---- title
    col.addView(stText("Stats", 24f, C_TXT, true), LinearLayout.LayoutParams(WRAP, WRAP))
    col.addView(stText("Your focus progress at a glance", 13f, C_DIMTXT), LinearLayout.LayoutParams(WRAP, WRAP))

    // ---- Daily Focus Goal + Streak
    val top = LinearLayout(this)
    top.orientation = LinearLayout.HORIZONTAL

    val gc = stCard()
    val gHead = LinearLayout(this)
    gHead.orientation = LinearLayout.HORIZONTAL
    gHead.gravity = Gravity.CENTER_VERTICAL
    gHead.addView(stText("Daily Focus Goal", 15f, C_TXT, true), LinearLayout.LayoutParams(0, WRAP, 1f))
    val edit = stText("Edit", 13f, C_ORANGE, true)
    edit.setPadding(dp(12), dp(5), dp(12), dp(5))
    edit.background = rounded(stTrack(), 14)
    edit.setOnClickListener { goalDialog() }
    gHead.addView(edit, LinearLayout.LayoutParams(WRAP, WRAP))
    gc.addView(gHead, LinearLayout.LayoutParams(MATCH, WRAP))

    val gv = SpannableStringBuilder(hm(todayMin))
    gv.setSpan(AbsoluteSizeSpan(28, true), 0, gv.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    gv.setSpan(StyleSpan(Typeface.BOLD), 0, gv.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    val st0 = gv.length
    gv.append("  / " + hm(goal))
    gv.setSpan(AbsoluteSizeSpan(16, true), st0, gv.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    gv.setSpan(ForegroundColorSpan(C_DIMTXT), st0, gv.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    val gvTv = stText(gv, 16f, C_TXT)
    gc.addView(gvTv, LinearLayout.LayoutParams(WRAP, WRAP))

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
    val fv = FlameView(this, streak > 0)
    sHead.addView(fv, LinearLayout.LayoutParams(dp(26), dp(34)))
    val sTitle = stText("Streak", 15f, C_TXT, true)
    sTitle.setPadding(dp(8), 0, 0, 0)
    sHead.addView(sTitle, LinearLayout.LayoutParams(WRAP, WRAP))
    sc.addView(sHead, LinearLayout.LayoutParams(MATCH, WRAP))
    sc.addView(stText(if (streak == 1) "1 day" else "$streak days", 28f, C_TXT, true), LinearLayout.LayoutParams(WRAP, WRAP))
    sc.addView(stText("Best: " + (if (bestStreak == 1) "1 day" else "$bestStreak days"), 13f, C_DIMTXT), LinearLayout.LayoutParams(WRAP, WRAP))

    val gl = LinearLayout.LayoutParams(0, WRAP, 1.7f)
    gl.setMargins(0, 0, dp(6), 0)
    top.addView(gc, gl)
    top.addView(sc, LinearLayout.LayoutParams(0, MATCH, 1f))
    val topLp = rowLp(WRAP)
    topLp.topMargin = dp(12)
    col.addView(top, topLp)

    // ---- Day / Month / Year
    val seg = LinearLayout(this)
    seg.orientation = LinearLayout.HORIZONTAL
    for ((i, n) in listOf("Day", "Month", "Year").withIndex()) {
        val t = tile(n, stMode == i) { stSetMode(i) }
        t.textSize = 15f
        val lp = LinearLayout.LayoutParams(0, MATCH, 1f)
        lp.setMargins(dp(3), 0, dp(3), 0)
        seg.addView(t, lp)
    }
    col.addView(seg, rowLp(dp(46)))

    // ---- <  NOVEMBER 2026  >
    val bar = LinearLayout(this)
    bar.orientation = LinearLayout.HORIZONTAL
    val prev = tile("\u2039", false) { stShift(-1) }
    prev.textSize = 26f
    val next = tile("\u203A", false) { stShift(1) }
    next.textSize = 26f
    val probe = stCal.clone() as Calendar
    when (stMode) {
        0 -> probe.add(Calendar.DAY_OF_YEAR, 1)
        1 -> { probe.set(Calendar.DAY_OF_MONTH, 1); probe.add(Calendar.MONTH, 1) }
        else -> { probe.set(Calendar.MONTH, 0); probe.set(Calendar.DAY_OF_MONTH, 1); probe.add(Calendar.YEAR, 1) }
    }
    next.alpha = if (probe.timeInMillis > nowMs) 0.3f else 1f
    val lbl = tile(stLabel(), false) { monthPickerDialog() }
    lbl.letterSpacing = 0.08f
    lbl.textSize = 14f
    bar.addView(prev, LinearLayout.LayoutParams(dp(56), MATCH))
    val ml = LinearLayout.LayoutParams(0, MATCH, 1f)
    ml.setMargins(dp(6), 0, dp(6), 0)
    bar.addView(lbl, ml)
    bar.addView(next, LinearLayout.LayoutParams(dp(56), MATCH))
    col.addView(bar, rowLp(dp(46)))

    // ---- chart + numbers
    val y = stCal.get(Calendar.YEAR)
    val m = stCal.get(Calendar.MONTH)
    var vals = IntArray(0)
    var labels: Array<String> = emptyArray()
    var future = Int.MAX_VALUE
    var sel = -1
    var pick: ((Int) -> Unit)? = null
    var rows: List<Pair<String, String>> = emptyList()

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
            rows = listOf(
                Pair(if (stThisPeriodWord() == "TODAY") "Today" else "Selected day", hm(stTotal())),
                Pair("Week Total", hm(vals.sum())),
                Pair("Best Day", hm(vals.maxOrNull() ?: 0))
            )
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
            val days = stScopeDays()
            val best = days.maxOrNull() ?: 0
            val avg = if (days.isEmpty()) 0 else days.sum() / days.size
            rows = listOf(
                Pair("Total Focus", hm(stTotal())),
                Pair("Best Day", hm(best)),
                Pair("Avg / Day", hm(avg))
            )
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
            val upto = if (future == Int.MAX_VALUE) 12 else future
            var best = 0
            var sum = 0
            for (i in 0 until upto) {
                sum += vals[i]
                if (vals[i] > best) best = vals[i]
            }
            rows = listOf(
                Pair("Total Focus", hm(stTotal())),
                Pair("Best Month", hm(best)),
                Pair("Avg / Month", hm(if (upto > 0) sum / upto else 0))
            )
        }
    }

    val sp = stSessions()
    rows = rows + listOf(
        Pair("Sessions", sp.first.toString()),
        Pair("Avg / Session", if (sp.first > 0) hm(sp.second / sp.first) else "\u2014")
    )

    val cc = stCard()
    cc.orientation = LinearLayout.HORIZONTAL
    cc.setPadding(dp(14), dp(14), dp(14), dp(8))
    val chart = BarChartView(this)
    chart.barColor = C_ORANGE
    chart.trackColor = stTrack()
    chart.textColor = C_TXT
    chart.values = vals
    chart.labels = labels
    chart.futureFrom = future
    chart.selected = sel
    chart.onPick = pick
    cc.addView(chart, LinearLayout.LayoutParams(0, dp(230), 1.6f))

    val divider = View(this)
    divider.setBackgroundColor(stTrack())
    val dvLp = LinearLayout.LayoutParams(dp(1), MATCH)
    dvLp.setMargins(dp(12), dp(4), dp(12), dp(10))
    cc.addView(divider, dvLp)

    val nums = LinearLayout(this)
    nums.orientation = LinearLayout.VERTICAL
    nums.gravity = Gravity.CENTER_VERTICAL
    for ((k, v) in rows) {
        val one = LinearLayout(this)
        one.orientation = LinearLayout.VERTICAL
        one.addView(stText(k, 12f, C_DIMTXT), LinearLayout.LayoutParams(WRAP, WRAP))
        one.addView(stText(v, 18f, C_TXT, true), LinearLayout.LayoutParams(WRAP, WRAP))
        val ol = LinearLayout.LayoutParams(MATCH, WRAP)
        ol.bottomMargin = dp(6)
        nums.addView(one, ol)
    }
    cc.addView(nums, LinearLayout.LayoutParams(0, MATCH, 1f))
    col.addView(cc, rowLp(WRAP))

    // ---- Goal Completion
    var met = 0
    var elapsed = 0
    var states = IntArray(0)
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
    val gcard = stCard()
    val gTop = LinearLayout(this)
    gTop.orientation = LinearLayout.HORIZONTAL
    gTop.gravity = Gravity.CENTER_VERTICAL
    val gLeft = LinearLayout(this)
    gLeft.orientation = LinearLayout.VERTICAL
    gLeft.addView(stText("Goal Completion", 15f, C_TXT, true), LinearLayout.LayoutParams(WRAP, WRAP))
    val period = if (stMode == 2) {
        if (y == now.get(Calendar.YEAR)) "This year" else y.toString()
    } else {
        if (y == now.get(Calendar.YEAR) && m == now.get(Calendar.MONTH)) "This month"
        else SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(stCal.time)
    }
    gLeft.addView(stText(period + "  \u00B7  goal " + hm(goal) + " / day", 12f, C_DIMTXT), LinearLayout.LayoutParams(WRAP, WRAP))
    gTop.addView(gLeft, LinearLayout.LayoutParams(0, WRAP, 1f))
    val gRight = LinearLayout(this)
    gRight.orientation = LinearLayout.VERTICAL
    gRight.gravity = Gravity.END
    gRight.addView(stText("$met / $elapsed days", 16f, C_TXT, true), LinearLayout.LayoutParams(WRAP, WRAP))
    val gp = if (elapsed > 0) met * 100 / elapsed else 0
    gRight.addView(stText("($gp%)", 12f, C_DIMTXT), LinearLayout.LayoutParams(WRAP, WRAP))
    gTop.addView(gRight, LinearLayout.LayoutParams(WRAP, WRAP))
    gcard.addView(gTop, LinearLayout.LayoutParams(MATCH, WRAP))
    if (stMode == 2) {
        val gb = ProgressBarView(this)
        gb.fillColor = C_ORANGE
        gb.trackColor = stTrack()
        gb.frac = if (elapsed > 0) met.toFloat() / elapsed else 0f
        val gbLp = LinearLayout.LayoutParams(MATCH, dp(10))
        gbLp.topMargin = dp(12)
        gcard.addView(gb, gbLp)
    } else {
        val dv = GoalDotsView(this)
        dv.fillColor = C_ORANGE
        dv.trackColor = stTrack()
        dv.states = states
        val dvLp2 = LinearLayout.LayoutParams(MATCH, dp(20))
        dvLp2.topMargin = dp(10)
        gcard.addView(dv, dvLp2)
    }
    col.addView(gcard, rowLp(WRAP))

    val foot = stText("Minutes are counted when a timer starts", 12f, C_DIMTXT)
    foot.gravity = Gravity.CENTER
    val fLp = rowLp(WRAP)
    fLp.topMargin = dp(10)
    col.addView(foot, fLp)
    return sv
}

