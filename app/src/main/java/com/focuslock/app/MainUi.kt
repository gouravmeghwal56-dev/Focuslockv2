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
    box.setPadding(dp(6), dp(12), dp(6), dp(8))
    val bg = GradientDrawable()
    bg.setColor(Color.WHITE)
    bg.cornerRadius = dp(26).toFloat()
    bg.setStroke(dp(1), Color.parseColor("#ECE6E3"))
    box.background = bg

    val pop = PopupWindow(box, dp(310), WRAP, true)
    pop.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
    pop.elevation = dp(10).toFloat()

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

    fun row(title: String, value: String?, dotColor: Int?, onClick: () -> Unit) {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.setPadding(dp(14), dp(12), dp(12), dp(12))
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
    row("Profile", if (name.isNullOrBlank()) "Add name" else name, null) { showProfile() }
    row("Theme & Appearance", null, null) { showThemePicker() }

    divider()
    section("BLOCKING")
    row("Notifications", null, null) {
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
        "Accessibility",
        if (accOn) "ON" else "OFF",
        if (accOn) Color.parseColor("#2E9E5B") else C_ORANGE
    ) {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    divider()
    section("DATA")
    row("Backup data", null, null) { startBackup() }
    row("Restore data", null, null) { startRestore() }

    divider()
    row("About FocusLock", null, null) { showAbout() }

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
        l.setMargins(0, dp(10), 0, 0)
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

    // ================= MAIN CARD =================
    val main = stCard()
    main.setPadding(dp(18), dp(16), dp(18), dp(14))

    // ---- hero: aaj ka focus (left) + streak (right)
    val hero = LinearLayout(this)
    hero.orientation = LinearLayout.HORIZONTAL
    hero.gravity = Gravity.CENTER_VERTICAL

    val heroL = LinearLayout(this)
    heroL.orientation = LinearLayout.VERTICAL
    val todayLbl = stText("TODAY", 11f, C_DIMTXT)
    todayLbl.letterSpacing = 0.14f
    heroL.addView(todayLbl, LinearLayout.LayoutParams(WRAP, WRAP))
    val gv = SpannableStringBuilder(hm(todayMin))
    gv.setSpan(AbsoluteSizeSpan(34, true), 0, gv.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    gv.setSpan(StyleSpan(Typeface.BOLD), 0, gv.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    val st0 = gv.length
    gv.append("  / " + hm(goal))
    gv.setSpan(AbsoluteSizeSpan(15, true), st0, gv.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    gv.setSpan(ForegroundColorSpan(C_DIMTXT), st0, gv.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    heroL.addView(stText(gv, 16f, C_TXT), LinearLayout.LayoutParams(WRAP, WRAP))
    hero.addView(heroL, LinearLayout.LayoutParams(0, WRAP, 1f))

    val heroR = LinearLayout(this)
    heroR.orientation = LinearLayout.VERTICAL
    heroR.gravity = Gravity.END
    val sTop = LinearLayout(this)
    sTop.orientation = LinearLayout.HORIZONTAL
    sTop.gravity = Gravity.CENTER_VERTICAL
    sTop.addView(FlameView(this, streak > 0), LinearLayout.LayoutParams(dp(24), dp(32)))
    val sTv = stText(if (streak == 1) "1 day" else "$streak days", 20f, C_TXT, true)
    sTv.setPadding(dp(6), 0, 0, 0)
    sTop.addView(sTv, LinearLayout.LayoutParams(WRAP, WRAP))
    heroR.addView(sTop, LinearLayout.LayoutParams(WRAP, WRAP))
    heroR.addView(stText("best " + (if (bestStreak == 1) "1 day" else "$bestStreak days"), 12f, C_DIMTXT), LinearLayout.LayoutParams(WRAP, WRAP))
    hero.addView(heroR, LinearLayout.LayoutParams(WRAP, WRAP))
    main.addView(hero, LinearLayout.LayoutParams(MATCH, WRAP))

    // ---- patli progress bar
    val pb = ProgressBarView(this)
    pb.fillColor = C_ORANGE
    pb.trackColor = stTrack()
    pb.frac = if (goal > 0) todayMin.toFloat() / goal else 0f
    val pbLp = LinearLayout.LayoutParams(MATCH, dp(6))
    pbLp.topMargin = dp(14)
    main.addView(pb, pbLp)

    val pct = if (goal > 0) minOf(100, todayMin * 100 / goal) else 0
    val pRow = LinearLayout(this)
    pRow.orientation = LinearLayout.HORIZONTAL
    pRow.gravity = Gravity.CENTER_VERTICAL
    pRow.addView(stText("$pct% of daily goal", 12f, C_DIMTXT), LinearLayout.LayoutParams(0, WRAP, 1f))
    val edit = stText("Edit goal", 12f, C_ORANGE, true)
    edit.setPadding(dp(10), dp(6), dp(0), dp(6))
    edit.setOnClickListener { goalDialog() }
    pRow.addView(edit, LinearLayout.LayoutParams(WRAP, WRAP))
    val pRowLp = LinearLayout.LayoutParams(MATCH, WRAP)
    pRowLp.topMargin = dp(2)
    main.addView(pRow, pRowLp)

    // ---- Day / Month / Year (chhoti pill)
    val seg = LinearLayout(this)
    seg.orientation = LinearLayout.HORIZONTAL
    seg.gravity = Gravity.CENTER
    seg.setPadding(dp(3), dp(3), dp(3), dp(3))
    seg.background = rounded(stTrack(), 22)
    for ((i, n) in listOf("Day", "Month", "Year").withIndex()) {
        val on = stMode == i
        val p = stText(n, 14f, if (on) Color.WHITE else C_TXT, on)
        p.gravity = Gravity.CENTER
        p.setPadding(dp(20), dp(7), dp(20), dp(7))
        if (on) p.background = rounded(C_ORANGE, 20)
        p.setOnClickListener { stSetMode(i) }
        seg.addView(p, LinearLayout.LayoutParams(WRAP, WRAP))
    }
    val segLp = LinearLayout.LayoutParams(WRAP, WRAP)
    segLp.gravity = Gravity.CENTER_HORIZONTAL
    segLp.topMargin = dp(16)
    main.addView(seg, segLp)

    // ---- <  SATURDAY 21 NOV 2026  >  (bina dibbon ke)
    val probe = stCal.clone() as Calendar
    when (stMode) {
        0 -> probe.add(Calendar.DAY_OF_YEAR, 1)
        1 -> { probe.set(Calendar.DAY_OF_MONTH, 1); probe.add(Calendar.MONTH, 1) }
        else -> { probe.set(Calendar.MONTH, 0); probe.set(Calendar.DAY_OF_MONTH, 1); probe.add(Calendar.YEAR, 1) }
    }
    val bar = LinearLayout(this)
    bar.orientation = LinearLayout.HORIZONTAL
    bar.gravity = Gravity.CENTER_VERTICAL
    val prev = stText("\u2039", 28f, C_TXT)
    prev.gravity = Gravity.CENTER
    prev.setPadding(dp(16), 0, dp(16), dp(3))
    prev.setOnClickListener { stShift(-1) }
    val next = stText("\u203A", 28f, C_TXT)
    next.gravity = Gravity.CENTER
    next.setPadding(dp(16), 0, dp(16), dp(3))
    next.alpha = if (probe.timeInMillis > nowMs) 0.3f else 1f
    next.setOnClickListener { stShift(1) }
    val lbl = stText(stLabel(), 13f, C_TXT, true)
    lbl.letterSpacing = 0.08f
    lbl.gravity = Gravity.CENTER
    lbl.setPadding(0, dp(10), 0, dp(10))
    lbl.setOnClickListener { monthPickerDialog() }
    bar.addView(prev, LinearLayout.LayoutParams(WRAP, WRAP))
    bar.addView(lbl, LinearLayout.LayoutParams(0, WRAP, 1f))
    bar.addView(next, LinearLayout.LayoutParams(WRAP, WRAP))
    val barLp = LinearLayout.LayoutParams(MATCH, WRAP)
    barLp.topMargin = dp(6)
    main.addView(bar, barLp)

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
                Pair("Week total", hm(vals.sum())),
                Pair("Best day", hm(vals.maxOrNull() ?: 0))
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
                Pair("Total focus", hm(stTotal())),
                Pair("Best day", hm(best)),
                Pair("Avg / day", hm(avg))
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
                Pair("Total focus", hm(stTotal())),
                Pair("Best month", hm(best)),
                Pair("Avg / month", hm(if (upto > 0) sum / upto else 0))
            )
        }
    }

    val sp = stSessions()
    rows = rows + listOf(
        Pair("Sessions", sp.first.toString()),
        Pair("Avg / session", if (sp.first > 0) hm(sp.second / sp.first) else "\u2014")
    )

    val chart = BarChartView(this)
    chart.barColor = C_ORANGE
    chart.trackColor = stTrack()
    chart.textColor = C_TXT
    chart.values = vals
    chart.labels = labels
    chart.futureFrom = future
    chart.selected = sel
    chart.onPick = pick
    val chLp = LinearLayout.LayoutParams(MATCH, dp(150))
    chLp.topMargin = dp(6)
    main.addView(chart, chLp)

    // numbers: 3 + 2 ek line mein, bina divider ke
    val numRow1 = LinearLayout(this)
    numRow1.orientation = LinearLayout.HORIZONTAL
    val numRow2 = LinearLayout(this)
    numRow2.orientation = LinearLayout.HORIZONTAL
    for ((idx, kv) in rows.withIndex()) {
        val one = LinearLayout(this)
        one.orientation = LinearLayout.VERTICAL
        one.addView(stText(kv.first, 11f, C_DIMTXT), LinearLayout.LayoutParams(WRAP, WRAP))
        one.addView(stText(kv.second, 17f, C_TXT, true), LinearLayout.LayoutParams(WRAP, WRAP))
        (if (idx < 3) numRow1 else numRow2).addView(one, LinearLayout.LayoutParams(0, WRAP, 1f))
    }
    val nr1Lp = LinearLayout.LayoutParams(MATCH, WRAP)
    nr1Lp.topMargin = dp(14)
    main.addView(numRow1, nr1Lp)
    val nr2Lp = LinearLayout.LayoutParams(MATCH, WRAP)
    nr2Lp.topMargin = dp(10)
    main.addView(numRow2, nr2Lp)

    col.addView(main, rowLp(WRAP).also { it.topMargin = dp(12) })

    // ================= GOAL COMPLETION =================
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
    gcard.setPadding(dp(18), dp(14), dp(18), dp(14))
    val gTop = LinearLayout(this)
    gTop.orientation = LinearLayout.HORIZONTAL
    gTop.gravity = Gravity.CENTER_VERTICAL
    val period = if (stMode == 2) {
        if (y == now.get(Calendar.YEAR)) "This year" else y.toString()
    } else {
        if (y == now.get(Calendar.YEAR) && m == now.get(Calendar.MONTH)) "This month"
        else SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(stCal.time)
    }
    val gLeft = LinearLayout(this)
    gLeft.orientation = LinearLayout.VERTICAL
    gLeft.addView(stText("Goal completion", 15f, C_TXT, true), LinearLayout.LayoutParams(WRAP, WRAP))
    gLeft.addView(stText(period, 12f, C_DIMTXT), LinearLayout.LayoutParams(WRAP, WRAP))
    gTop.addView(gLeft, LinearLayout.LayoutParams(0, WRAP, 1f))
    val gp = if (elapsed > 0) met * 100 / elapsed else 0
    gTop.addView(stText("$met / $elapsed days  \u00B7  $gp%", 14f, C_TXT, true), LinearLayout.LayoutParams(WRAP, WRAP))
    gcard.addView(gTop, LinearLayout.LayoutParams(MATCH, WRAP))
    if (stMode == 2) {
        val gb = ProgressBarView(this)
        gb.fillColor = C_ORANGE
        gb.trackColor = stTrack()
        gb.frac = if (elapsed > 0) met.toFloat() / elapsed else 0f
        val gbLp = LinearLayout.LayoutParams(MATCH, dp(6))
        gbLp.topMargin = dp(14)
        gcard.addView(gb, gbLp)
    } else {
        val dv = GoalDotsView(this)
        dv.fillColor = C_ORANGE
        dv.trackColor = stTrack()
        dv.states = states
        val dvLp2 = LinearLayout.LayoutParams(MATCH, dp(18))
        dvLp2.topMargin = dp(12)
        gcard.addView(dv, dvLp2)
    }
    col.addView(gcard, rowLp(WRAP))

    val foot = stText("Minutes are counted when a timer starts", 11f, C_DIMTXT)
    foot.gravity = Gravity.CENTER
    val fLp = rowLp(WRAP)
    fLp.topMargin = dp(12)
    col.addView(foot, fLp)
    return sv
}
