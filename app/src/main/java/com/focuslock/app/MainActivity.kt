package com.focuslock.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
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
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ---------- Activity ----------

class MainActivity : Activity() {

    private class App(val label: String, val pkg: String)

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val ui = Handler(Looper.getMainLooper())
    private val iconCache = HashMap<String, Drawable?>()
    private var allApps: List<App> = emptyList()
    private var tab = 0
    private var durMin = 180
    private var bagSel = 0
    private var rtSel = 0
    private var lastRunning = false

    private lateinit var host: FrameLayout
    private lateinit var pc: PixelClockView
    private lateinit var dots: DotsView
    private lateinit var bagsView: BagsView
    private lateinit var scene: SceneView
    private lateinit var caption: TextView
    private lateinit var pillText: TextView
    private lateinit var pillIcon: IconView
    private lateinit var controls: LinearLayout
    private val railBtns = ArrayList<IconView>()
    private val tabKinds = intArrayOf(IconView.TIMER, IconView.BAG, IconView.GRID, IconView.BARS)
    private val tabNames = arrayOf("Focus Session", "Bags", "Routines", "Stats")

    // timer page refs (so tiles can update without rebuilding the page while dragging)
    private var t15: TextView? = null
    private var t45: TextView? = null
    private var t180: TextView? = null
    private var tCustom: TextView? = null
    private var ruler: RulerView? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_LONG).show()
    private fun fmt(min: Int) = String.format("%02d:%02d", min / 60, min % 60)
    private fun locked() = Store.active(this) && Store.strict(this)
    private fun lockedToast() = toast("Strict mode is ON, can't change right now")

    private fun rounded(color: Int, r: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(r).toFloat()
        return g
    }

    private fun oval(color: Int): GradientDrawable {
        val g = GradientDrawable()
        g.shape = GradientDrawable.OVAL
        g.setColor(color)
        return g
    }

    private fun twoLine(top: String, bottom: String): CharSequence {
        val sb = SpannableStringBuilder()
        sb.append(top)
        sb.setSpan(AbsoluteSizeSpan(34, true), 0, top.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(StyleSpan(Typeface.BOLD), 0, top.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append("\n")
        sb.append(bottom)
        return sb
    }

    private fun tile(text: CharSequence, selected: Boolean, onClick: () -> Unit): TextView {
        val t = TextView(this)
        t.text = text
        t.gravity = Gravity.CENTER
        t.textSize = 16f
        t.setTextColor(if (selected) Color.WHITE else C_TXT)
        t.background = rounded(if (selected) C_DARK else C_TILE, 20)
        t.setOnClickListener { onClick() }
        t.setOnTouchListener { v, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
            }
            false
        }
        return t
    }

    private fun styleTile(t: TextView?, sel: Boolean) {
        if (t == null) return
        t.setTextColor(if (sel) Color.WHITE else C_TXT)
        t.background = rounded(if (sel) C_DARK else C_TILE, 20)
    }

    private fun hrow(vararg v: View): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        for (x in v) {
            val lp = LinearLayout.LayoutParams(0, MATCH, 1f)
            lp.setMargins(dp(4), 0, dp(4), 0)
            r.addView(x, lp)
        }
        return r
    }

    private fun vbox(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(2), dp(2), dp(2), dp(2))
        return c
    }

    private fun lpV(h: Int = 0): LinearLayout.LayoutParams {
        val lp = if (h == 0) LinearLayout.LayoutParams(MATCH, 0, 1f) else LinearLayout.LayoutParams(MATCH, h)
        lp.setMargins(0, dp(5), 0, dp(5))
        return lp
    }

    private fun icon(pkg: String): Drawable? =
        iconCache.getOrPut(pkg) {
            try { packageManager.getApplicationIcon(pkg) } catch (e: Exception) { null }
        }

    private fun accEnabled(): Boolean {
        val am = getSystemService(AccessibilityManager::class.java)
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    private fun loadApps() {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        allApps = packageManager.queryIntentActivities(i, 0)
            .filter { it.activityInfo.packageName != packageName }
            .map { App(it.loadLabel(packageManager).toString(), it.activityInfo.packageName) }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadApps()
        bagSel = Store.activeBag(this)
        val wide = resources.displayMetrics.widthPixels > resources.displayMetrics.heightPixels

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(C_BG)
        root.setPadding(dp(14), dp(26), dp(14), dp(12))

        root.addView(buildHeader(), LinearLayout.LayoutParams(MATCH, dp(56)))

        val body = LinearLayout(this)
        body.orientation = if (wide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL

        val left = buildLeft()
        host = FrameLayout(this)
        val rail = buildRail(wide)

        fun lp(w: Int, h: Int, wt: Float): LinearLayout.LayoutParams {
            val l = LinearLayout.LayoutParams(w, h, wt)
            l.setMargins(dp(6), dp(6), dp(6), dp(6))
            return l
        }
        if (wide) {
            body.addView(left, lp(0, MATCH, 1f))
            body.addView(host, lp(0, MATCH, 1.05f))
            body.addView(rail, lp(WRAP, WRAP, 0f).also { it.gravity = Gravity.CENTER_VERTICAL })
        } else {
            body.addView(left, lp(MATCH, 0, 1f))
            body.addView(host, lp(MATCH, 0, 1.3f))
            body.addView(rail, lp(MATCH, dp(68), 0f))
        }
        root.addView(body, LinearLayout.LayoutParams(MATCH, 0, 1f))
        setContentView(root)
    }

    private fun circleBtn(kind: Int, onClick: () -> Unit): IconView {
        val v = IconView(this, kind)
        v.setPadding(dp(12), dp(12), dp(12), dp(12))
        v.background = oval(C_CARD)
        v.setOnClickListener { onClick() }
        return v
    }

    private fun buildHeader(): FrameLayout {
        val f = FrameLayout(this)
        val menu = circleBtn(IconView.MENU) { toast("Coming soon") }
        val gear = circleBtn(IconView.GEAR) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        f.addView(menu, FrameLayout.LayoutParams(dp(48), dp(48), Gravity.START or Gravity.CENTER_VERTICAL))
        f.addView(gear, FrameLayout.LayoutParams(dp(48), dp(48), Gravity.END or Gravity.CENTER_VERTICAL))

        val logo = LinearLayout(this)
        logo.orientation = LinearLayout.HORIZONTAL
        logo.gravity = Gravity.CENTER
        val lk = IconView(this, IconView.LOCK)
        lk.tint = C_ORANGE
        logo.addView(lk, LinearLayout.LayoutParams(dp(30), dp(34)))
        val tv = TextView(this)
        val sp = SpannableString("FocusLock")
        sp.setSpan(ForegroundColorSpan(C_ORANGE), 5, 9, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        tv.text = sp
        tv.textSize = 26f
        tv.setTextColor(C_TXT)
        tv.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        tv.setPadding(dp(8), 0, 0, 0)
        logo.addView(tv, LinearLayout.LayoutParams(WRAP, WRAP))
        f.addView(logo, FrameLayout.LayoutParams(WRAP, MATCH, Gravity.CENTER))
        return f
    }

    private fun buildLeft(): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = rounded(C_CARD, 28)
        card.clipToOutline = true

        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(16), dp(16), dp(16), dp(10))

        // pill
        val pillRow = LinearLayout(this)
        pillRow.orientation = LinearLayout.HORIZONTAL
        pillRow.gravity = Gravity.CENTER_VERTICAL
        pillRow.background = rounded(C_TILE, 24)
        pillRow.setPadding(dp(16), dp(8), dp(20), dp(8))
        pillIcon = IconView(this, IconView.TIMER)
        pillText = TextView(this)
        pillText.textSize = 15f
        pillText.setTextColor(C_TXT)
        pillText.setPadding(dp(10), 0, 0, 0)
        pillRow.addView(pillIcon, LinearLayout.LayoutParams(dp(22), dp(22)))
        pillRow.addView(pillText, LinearLayout.LayoutParams(WRAP, WRAP))
        val pl = LinearLayout.LayoutParams(WRAP, WRAP)
        pl.gravity = Gravity.CENTER_HORIZONTAL
        content.addView(pillRow, pl)

        pc = PixelClockView(this)
        bagsView = BagsView(this)
        dots = DotsView(this)
        caption = TextView(this)
        caption.gravity = Gravity.CENTER
        caption.textSize = 13f
        caption.letterSpacing = 0.1f
        caption.setTextColor(C_TXT)

        // controls: reset / play / stop
        controls = LinearLayout(this)
        controls.orientation = LinearLayout.HORIZONTAL
        controls.gravity = Gravity.CENTER

        val reset = IconView(this, IconView.RESET)
        reset.setPadding(dp(14), dp(14), dp(14), dp(14))
        reset.background = oval(C_TILE)
        reset.setOnClickListener {
            if (Store.active(this)) {
                toast("Can't change while a session is running")
            } else {
                durMin = 180
                refreshDur(false)
                updateLeft()
            }
        }
        val play = IconView(this, IconView.PLAY)
        play.tint = Color.WHITE
        play.setPadding(dp(30), dp(14), dp(30), dp(14))
        play.background = rounded(C_DARK, 28)
        play.setOnClickListener {
            if (Store.active(this)) toast("A session is already running") else startFocus()
        }
        val stop = IconView(this, IconView.STOP)
        stop.setPadding(dp(14), dp(14), dp(14), dp(14))
        stop.background = oval(C_TILE)
        stop.setOnClickListener {
            if (!Store.active(this)) {
                toast("No session is running")
            } else if (Store.strict(this)) {
                toast("Strict mode is ON, can't end early")
            } else if (Store.sessionOn(this)) {
                Store.stopSession(this)
                render()
                updateLeft()
            } else {
                toast("Routine is running, turn it off from the Routines tab")
            }
        }
        fun cl(w: Int): LinearLayout.LayoutParams {
            val l = LinearLayout.LayoutParams(w, dp(56))
            l.setMargins(dp(10), 0, dp(10), 0)
            return l
        }
        controls.addView(reset, cl(dp(56)))
        controls.addView(play, cl(dp(112)))
        controls.addView(stop, cl(dp(56)))

        content.addView(pc, LinearLayout.LayoutParams(MATCH, 0, 1f))
        content.addView(bagsView, LinearLayout.LayoutParams(MATCH, 0, 1f))
        content.addView(controls, LinearLayout.LayoutParams(MATCH, WRAP))
        content.addView(dots, LinearLayout.LayoutParams(MATCH, dp(16)))
        val capLp = LinearLayout.LayoutParams(MATCH, WRAP)
        capLp.topMargin = dp(10)
        content.addView(caption, capLp)

        scene = SceneView(this)
        card.addView(content, LinearLayout.LayoutParams(MATCH, 0, 1f))
        card.addView(scene, LinearLayout.LayoutParams(MATCH, dp(110)))
        return card
    }

    private fun buildRail(wide: Boolean): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = if (wide) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER
        r.background = rounded(Color.parseColor("#FFFFFF"), 32)
        r.setPadding(dp(4), dp(4), dp(4), dp(4))
        railBtns.clear()
        for (i in 0..3) {
            val t = IconView(this, tabKinds[i])
            t.setPadding(dp(15), dp(15), dp(15), dp(15))
            t.setOnClickListener {
                if (tab != i) {
                    tab = i
                    render(true)
                    updateLeft()
                    styleRail()
                }
            }
            railBtns.add(t)
            val lp = if (wide) LinearLayout.LayoutParams(dp(58), dp(58)) else LinearLayout.LayoutParams(0, dp(52), 1f)
            lp.setMargins(dp(4), dp(4), dp(4), dp(4))
            r.addView(t, lp)
        }
        styleRail()
        return r
    }

    private fun styleRail() {
        for (i in railBtns.indices) {
            railBtns[i].background = if (i == tab) rounded(C_DARK, 26) else null
            railBtns[i].tint = if (i == tab) Color.WHITE else C_DARK
        }
    }

    private fun render(animate: Boolean = false) {
        host.removeAllViews()
        val page = when (tab) {
            0 -> timerPage()
            1 -> bagsPage()
            2 -> routinesPage()
            else -> statsPage()
        }
        host.addView(page, FrameLayout.LayoutParams(MATCH, MATCH))
        if (animate) {
            page.alpha = 0f
            page.translationX = dp(28).toFloat()
            page.animate().alpha(1f).translationX(0f).setDuration(220).start()
        }
    }

    // ---------- Left panel ----------
    private fun monthTotal(): Int {
        val cal = Calendar.getInstance()
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH)
        var sum = 0L
        for (d in 1..cal.getActualMaximum(Calendar.DAY_OF_MONTH)) sum += Store.dayMinutes(this, y, m, d)
        return sum.toInt()
    }

    private fun updateLeft() {
        pc.visibility = if (tab == 1) View.GONE else View.VISIBLE
        bagsView.visibility = if (tab == 1) View.VISIBLE else View.GONE
        dots.visibility = if (tab == 3) View.VISIBLE else View.GONE
        controls.visibility = if (tab == 0) View.VISIBLE else View.GONE
        scene.visibility = if (tab == 0) View.VISIBLE else View.GONE
        caption.visibility = if (tab == 0) View.GONE else View.VISIBLE
        pillIcon.kind = tabKinds[tab]
        pillIcon.invalidate()
        pillText.text = tabNames[tab]

        when (tab) {
            0 -> {
                if (Store.sessionOn(this)) {
                    val remMs = Store.until(this) - System.currentTimeMillis()
                    val remMin = ((remMs + 59_999L) / 60_000L).toInt()
                    pc.show(remMin, C_ORANGE)
                    val total = Store.sessionMinutes(this) * 60_000L
                    val frac = if (total > 0L) 1f - remMs.toFloat() / total.toFloat() else 0f
                    scene.progress = frac
                    pillText.text = "Focus Running"
                } else if (Store.inSchedule(this)) {
                    pc.show(0, C_ORANGE)
                    scene.progress = 0.5f
                    pillText.text = "Routine Active"
                } else {
                    pc.show(durMin, C_ORANGE)
                    scene.progress = 0.3f
                }
            }
            1 -> {
                bagsView.active = bagSel
                bagsView.invalidate()
                val mode = if (Store.allowMode(this)) "ALLOW ONLY" else "BLOCK"
                caption.text = "${Store.bag(this, bagSel).size} apps  \u00B7  Bag ${bagSel + 1}  \u00B7  $mode"
            }
            2 -> {
                val on = Store.rtOn(this, rtSel)
                pc.show(Store.rtStart(this, rtSel), if (on) C_ORANGE else C_GRAY)
                caption.text = "TO ${fmt(Store.rtEnd(this, rtSel))}  \u00B7  ROUTINE ${rtSel + 1}  \u00B7  " + if (on) "ON" else "OFF"
            }
            else -> {
                val cal = Calendar.getInstance()
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH)
                val n = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                pc.show(monthTotal(), C_ORANGE)
                dots.total = n
                dots.marks = BooleanArray(n) { i -> Store.dayMinutes(this, y, m, i + 1) > 0L }
                dots.invalidate()
                caption.text = "THIS MONTH  \u00B7  HH:MM"
            }
        }
    }

    // ---------- Page 0: Timer ----------
    private fun refreshDur(fromRuler: Boolean) {
        styleTile(t15, durMin == 15)
        styleTile(t45, durMin == 45)
        styleTile(t180, durMin == 180)
        val preset = durMin == 15 || durMin == 45 || durMin == 180
        tCustom?.text = twoLine(fmt(durMin), "Custom")
        styleTile(tCustom, !preset)
        if (!fromRuler) ruler?.setMinutes(durMin)
    }

    private fun durTile(min: Int, top: String, bottom: String): TextView {
        return tile(twoLine(top, bottom), durMin == min) {
            if (locked()) {
                lockedToast()
            } else {
                durMin = min
                refreshDur(false)
                updateLeft()
            }
        }
    }

    private fun stepDur(d: Int) {
        if (locked()) {
            lockedToast()
            return
        }
        durMin = (((durMin + d) / 5) * 5).coerceIn(5, 1440)
        refreshDur(false)
        updateLeft()
    }

    private fun customDialog() {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.HORIZONTAL
        box.setPadding(dp(16), dp(8), dp(16), dp(0))
        val eh = EditText(this)
        eh.hint = "hours"
        eh.inputType = InputType.TYPE_CLASS_NUMBER
        eh.setText((durMin / 60).toString())
        val em = EditText(this)
        em.hint = "minutes"
        em.inputType = InputType.TYPE_CLASS_NUMBER
        em.setText((durMin % 60).toString())
        box.addView(eh, LinearLayout.LayoutParams(0, WRAP, 1f))
        box.addView(em, LinearLayout.LayoutParams(0, WRAP, 1f))
        AlertDialog.Builder(this)
            .setTitle("Custom time (max 24 hours)")
            .setView(box)
            .setPositiveButton("OK") { _, _ ->
                val t = (eh.text.toString().toLongOrNull() ?: 0L) * 60L + (em.text.toString().toLongOrNull() ?: 0L)
                if (t in 1L..1440L) {
                    durMin = t.toInt()
                    refreshDur(false)
                    updateLeft()
                } else {
                    toast("Pick between 1 minute and 24 hours")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleStrict() {
        if (Store.active(this)) { toast("Can't change during a session"); return }
        Store.setStrict(this, !Store.strict(this))
        render()
    }

    private fun toggleMode() {
        if (Store.active(this)) { toast("Can't change during a session"); return }
        Store.setAllowMode(this, !Store.allowMode(this))
        render()
        updateLeft()
    }

    private fun timerPage(): View {
        val col = vbox()
        if (!accEnabled()) {
            val b = tile("Accessibility OFF  \u00B7  tap to turn ON", false) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            b.textSize = 14f
            b.setTextColor(Color.WHITE)
            b.background = rounded(C_ORANGE, 16)
            col.addView(b, lpV(dp(52)))
        }

        val a = durTile(15, "15", "Minutes")
        val b = durTile(45, "45", "Minutes")
        val c = durTile(180, "3", "Hours")
        val preset = durMin == 15 || durMin == 45 || durMin == 180
        val d = tile(twoLine(fmt(durMin), "Custom"), !preset) {
            if (locked()) lockedToast() else customDialog()
        }
        t15 = a
        t45 = b
        t180 = c
        tCustom = d

        val tilesCol = LinearLayout(this)
        tilesCol.orientation = LinearLayout.VERTICAL
        tilesCol.addView(hrow(a, b), lpV())
        val lowLp = LinearLayout.LayoutParams(MATCH, 0, 1.7f)
        lowLp.setMargins(0, dp(5), 0, dp(5))
        tilesCol.addView(hrow(c, d), lowLp)

        val rl = RulerView(this) { m ->
            durMin = m
            refreshDur(true)
            updateLeft()
        }
        rl.onBlocked = {
            if (locked()) {
                lockedToast()
                true
            } else {
                false
            }
        }
        rl.setMinutes(durMin)
        ruler = rl

        val up = IconView(this, IconView.UP)
        up.setOnClickListener { stepDur(5) }
        val down = IconView(this, IconView.DOWN)
        down.setOnClickListener { stepDur(-5) }
        val stepBox = LinearLayout(this)
        stepBox.orientation = LinearLayout.VERTICAL
        val sbg = GradientDrawable()
        sbg.setColor(Color.TRANSPARENT)
        sbg.cornerRadius = dp(24).toFloat()
        sbg.setStroke(dp(1), C_LINE)
        stepBox.background = sbg
        stepBox.addView(up, LinearLayout.LayoutParams(MATCH, 0, 1f))
        val divider = View(this)
        divider.setBackgroundColor(C_LINE)
        val dl = LinearLayout.LayoutParams(MATCH, dp(1))
        dl.setMargins(dp(12), 0, dp(12), 0)
        stepBox.addView(divider, dl)
        stepBox.addView(down, LinearLayout.LayoutParams(MATCH, 0, 1f))

        val rulerCol = LinearLayout(this)
        rulerCol.orientation = LinearLayout.VERTICAL
        val rlp = LinearLayout.LayoutParams(MATCH, 0, 1f)
        rlp.bottomMargin = dp(8)
        rulerCol.addView(rl, rlp)
        rulerCol.addView(stepBox, LinearLayout.LayoutParams(MATCH, dp(84)))

        val main = LinearLayout(this)
        main.orientation = LinearLayout.HORIZONTAL
        val tl = LinearLayout.LayoutParams(0, MATCH, 1f)
        tl.rightMargin = dp(8)
        main.addView(tilesCol, tl)
        main.addView(rulerCol, LinearLayout.LayoutParams(dp(64), MATCH))
        col.addView(main, lpV())

        val strictTile = tile("STRICT\n" + if (Store.strict(this)) "ON" else "OFF", Store.strict(this)) { toggleStrict() }
        val modeTile = tile("MODE\n" + if (Store.allowMode(this)) "BAG (allow only)" else "BLOCK", Store.allowMode(this)) { toggleMode() }
        strictTile.textSize = 14f
        modeTile.textSize = 14f
        col.addView(hrow(strictTile, modeTile), lpV(dp(60)))

        if (!Store.active(this)) {
            col.addView(SlideView(this) { startFocus() }, lpV(dp(64)))
        } else if (Store.sessionOn(this) && !Store.strict(this)) {
            col.addView(tile("End session", false) {
                Store.stopSession(this)
                render()
                updateLeft()
            }, lpV(dp(64)))
        } else if (Store.strict(this)) {
            col.addView(tile("STRICT ON  \u00B7  locked until the timer ends", false) {}, lpV(dp(64)))
        } else {
            col.addView(tile("Routine running (turn it off from the Routines tab)", false) {}, lpV(dp(64)))
        }
        return col
    }

    private fun startFocus() {
        if (Store.selected(this).isEmpty()) {
            toast("Add apps in the Bags tab first")
            return
        }
        if (!accEnabled()) {
            toast("Turn Accessibility ON first")
            return
        }
        val go = {
            Store.startSession(this, durMin.toLong())
            render()
            updateLeft()
        }
        if (Store.strict(this)) {
            AlertDialog.Builder(this)
                .setTitle("Strict mode")
                .setMessage("You won't be able to end for ${durMin / 60}h ${durMin % 60}m and Settings will be blocked. Sure?")
                .setPositiveButton("Yes") { _, _ -> go() }
                .setNegativeButton("No", null)
                .show()
        } else {
            go()
        }
    }

    // ---------- Page 1: Bags ----------
    private fun bagTab(i: Int): TextView {
        val sel = bagSel == i
        val t = tile("Bag ${i + 1}", false) {
            if (locked()) {
                lockedToast()
            } else {
                bagSel = i
                Store.setActiveBag(this, i)
                render()
                updateLeft()
            }
        }
        if (sel) {
            t.setTextColor(Color.WHITE)
            t.background = rounded(C_ORANGE, 26)
        }
        return t
    }

    private fun bagsPage(): View {
        val col = vbox()
        col.addView(hrow(bagTab(0), bagTab(1), bagTab(2)), lpV(dp(52)))
        val list = Store.bag(this, bagSel).toList().sorted()
        val cells = ArrayList<View>()
        for (i in 0 until 6) {
            val cell = FrameLayout(this)
            cell.background = rounded(C_TILE, 20)
            if (i == 5 && list.size > 6) {
                val tv = TextView(this)
                tv.text = "+${list.size - 5}"
                tv.textSize = 22f
                tv.gravity = Gravity.CENTER
                tv.setTextColor(C_TXT)
                cell.addView(tv, FrameLayout.LayoutParams(MATCH, MATCH))
            } else if (i < list.size) {
                val iv = ImageView(this)
                iv.setImageDrawable(icon(list[i]))
                val lp = FrameLayout.LayoutParams(dp(44), dp(44))
                lp.gravity = Gravity.CENTER
                cell.addView(iv, lp)
            }
            cell.setOnClickListener { showPicker() }
            cells.add(cell)
        }
        col.addView(hrow(cells[0], cells[1], cells[2]), lpV())
        col.addView(hrow(cells[3], cells[4], cells[5]), lpV())
        col.addView(hrow(
            tile("Add / remove apps", true) { showPicker() },
            tile("Clear bag", false) {
                if (locked()) {
                    lockedToast()
                } else {
                    Store.setBag(this, bagSel, emptySet())
                    render()
                    updateLeft()
                }
            }
        ), lpV(dp(80)))
        return col
    }

    private fun showPicker() {
        if (locked()) {
            lockedToast()
            return
        }
        val set = HashSet(Store.bag(this, bagSel))
        var shown: List<App> = allApps

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(12), dp(8), dp(12), dp(0))
        val search = EditText(this)
        search.hint = "Search app"
        search.isSingleLine = true
        col.addView(search)

        val lv = ListView(this)
        val ad = object : BaseAdapter() {
            override fun getCount() = shown.size
            override fun getItem(i: Int): Any = shown[i]
            override fun getItemId(i: Int) = i.toLong()
            override fun getView(i: Int, v: View?, parent: ViewGroup?): View {
                val a = shown[i]
                val row = (v as? LinearLayout) ?: LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(8), dp(6), dp(8), dp(6))
                    val iv = ImageView(this@MainActivity)
                    addView(iv, LinearLayout.LayoutParams(dp(40), dp(40)))
                    val tv = TextView(this@MainActivity)
                    tv.textSize = 16f
                    tv.setPadding(dp(12), 0, dp(8), 0)
                    addView(tv, LinearLayout.LayoutParams(0, WRAP, 1f))
                    val cb = CheckBox(this@MainActivity)
                    cb.isClickable = false
                    cb.isFocusable = false
                    addView(cb)
                }
                (row.getChildAt(0) as ImageView).setImageDrawable(icon(a.pkg))
                (row.getChildAt(1) as TextView).text = a.label
                (row.getChildAt(2) as CheckBox).isChecked = a.pkg in set
                return row
            }
        }
        lv.adapter = ad
        lv.setOnItemClickListener { _, _, i, _ ->
            val p = shown[i].pkg
            if (!set.remove(p)) set.add(p)
            ad.notifyDataSetChanged()
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString() ?: ""
                shown = if (q.isBlank()) allApps else allApps.filter { it.label.contains(q, ignoreCase = true) }
                ad.notifyDataSetChanged()
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        val bAll = Button(this)
        bAll.text = "Select all shown"
        bAll.setOnClickListener {
            shown.forEach { set.add(it.pkg) }
            ad.notifyDataSetChanged()
        }
        val bNone = Button(this)
        bNone.text = "Clear all"
        bNone.setOnClickListener {
            set.clear()
            ad.notifyDataSetChanged()
        }
        bar.addView(bAll, LinearLayout.LayoutParams(0, WRAP, 1f))
        bar.addView(bNone, LinearLayout.LayoutParams(0, WRAP, 1f))
        col.addView(bar)
        col.addView(lv, LinearLayout.LayoutParams(MATCH, dp(360)))

        AlertDialog.Builder(this)
            .setTitle("Bag ${bagSel + 1} apps")
            .setView(col)
            .setPositiveButton("Save") { _, _ ->
                Store.setBag(this, bagSel, set)
                render()
                updateLeft()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---------- Page 2: Routines ----------
    private fun rtTab(i: Int): TextView {
        val t = tile("ROUTINE ${i + 1}\n" + fmt(Store.rtStart(this, i)), false) {
            rtSel = i
            render()
            updateLeft()
        }
        if (rtSel == i) {
            t.setTextColor(Color.WHITE)
            t.background = rounded(C_DARK, 20)
        }
        return t
    }

    private fun pickTime(start: Boolean) {
        if (locked()) {
            lockedToast()
            return
        }
        val cur = if (start) Store.rtStart(this, rtSel) else Store.rtEnd(this, rtSel)
        TimePickerDialog(this, { _, h, m ->
            if (start) Store.setRtStart(this, rtSel, h * 60 + m) else Store.setRtEnd(this, rtSel, h * 60 + m)
            render()
            updateLeft()
        }, cur / 60, cur % 60, true).show()
    }

    private fun routinesPage(): View {
        val col = vbox()
        col.addView(hrow(rtTab(0), rtTab(1), rtTab(2)), lpV(dp(72)))

        val mask = Store.rtDays(this, rtSel)
        val names = arrayOf("S", "M", "T", "W", "T", "F", "S")
        val days = ArrayList<View>()
        for (d in 0..6) {
            val on = ((mask shr d) and 1) == 1
            val t = TextView(this)
            t.text = names[d]
            t.gravity = Gravity.CENTER
            t.textSize = 18f
            t.setTextColor(if (on) Color.WHITE else C_TXT)
            t.background = rounded(if (on) C_ORANGE else C_TILE, 16)
            t.setOnClickListener {
                if (locked()) {
                    lockedToast()
                } else {
                    Store.setRtDays(this, rtSel, mask xor (1 shl d))
                    render()
                    updateLeft()
                }
            }
            days.add(t)
        }
        col.addView(hrow(*days.toTypedArray()), lpV())

        col.addView(hrow(
            tile("START\n" + fmt(Store.rtStart(this, rtSel)), false) { pickTime(true) },
            tile("END\n" + fmt(Store.rtEnd(this, rtSel)), false) { pickTime(false) }
        ), lpV())

        val on = Store.rtOn(this, rtSel)
        col.addView(tile("ROUTINE " + if (on) "ON" else "OFF", on) {
            if (locked()) {
                lockedToast()
            } else {
                Store.setRtOn(this, rtSel, !on)
                render()
                updateLeft()
            }
        }, lpV(dp(72)))
        return col
    }

    // ---------- Page 3: Stats ----------
    private fun statsPage(): View {
        val col = vbox()
        val cal = Calendar.getInstance()
        val today = Store.dayMinutes(this, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        val month = monthTotal()
        val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time).uppercase()
        col.addView(tile(label, false) {}, lpV(dp(64)))
        col.addView(tile("TODAY\n$today min", false) {}, lpV())
        col.addView(tile("THIS MONTH\n${month / 60}h ${month % 60}m", true) {}, lpV())
        col.addView(tile("Minutes are counted when a timer starts", false) {}, lpV(dp(64)))
        return col
    }

    // ---------- Lifecycle ----------
    private val tick = object : Runnable {
        override fun run() {
            val r = Store.active(this@MainActivity)
            if (r != lastRunning) {
                lastRunning = r
                render()
            }
            updateLeft()
            ui.postDelayed(this, 1000)
        }
    }

    override fun onResume() {
        super.onResume()
        lastRunning = Store.active(this)
        render()
        updateLeft()
        styleRail()
        ui.removeCallbacks(tick)
        ui.postDelayed(tick, 1000)
    }

    override fun onPause() {
        super.onPause()
        ui.removeCallbacks(tick)
    }
}
