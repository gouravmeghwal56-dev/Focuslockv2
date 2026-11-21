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
    private var isWide = true
    private lateinit var pillRow: LinearLayout
    private var rtStatusTv: TextView? = null
    private val rtTimeTvs = arrayOfNulls<TextView>(3)

    // focus (black) screen
    private var focus: FocusScreen? = null
    private lateinit var normalRoot: View
    private var origStatus = 0
    private var origNav = 0
    private var origUi = 0

    // animations: ruler slide + clock colour
    private var rulerAnim: ValueAnimator? = null
    private var rulerShown = 180f
    private var clockColor = C_ORANGE
    private var clockTarget = C_ORANGE
    private var clockAnim: ValueAnimator? = null

    private lateinit var host: FrameLayout
    private lateinit var pc: PixelClockView
    private lateinit var dots: DotsView
    private lateinit var bagsView: BagsView
    private lateinit var detoxView: DetoxView
    private lateinit var scene: SceneView
    private lateinit var caption: TextView
    private lateinit var pillText: TextView
    private lateinit var pillIcon: IconView
    private lateinit var controls: LinearLayout
    private val railBtns = ArrayList<IconView>()
    private val tabKinds = intArrayOf(IconView.TIMER, IconView.BAG, IconView.GRID, IconView.BARS, IconView.DETOX)
    private val tabNames = arrayOf("Focus Session", "Bags", "Routines", "Stats", "Detox")

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
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 7)
        }
        bagSel = Store.activeBag(this)
        val wide = resources.displayMetrics.widthPixels > resources.displayMetrics.heightPixels
        isWide = wide

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(C_BG)
        root.setPadding(dp(14), dp(26), dp(14), dp(12))

        root.addView(buildHeader(), LinearLayout.LayoutParams(MATCH, dp(56)))

        val body = LinearLayout(this)
        body.orientation = if (wide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL

        val left = buildLeft()
        host = FrameLayout(this)
        host.clipChildren = true
        host.clipToPadding = true
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
        normalRoot = root
        origStatus = window.statusBarColor
        origNav = window.navigationBarColor
        origUi = window.decorView.systemUiVisibility
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
        pillRow = LinearLayout(this)
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
        detoxView = DetoxView(this)
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
        content.addView(detoxView, LinearLayout.LayoutParams(MATCH, 0, 1f))
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
        for (i in 0..4) {
            val t = IconView(this, tabKinds[i])
            t.setPadding(dp(15), dp(15), dp(15), dp(15))
            t.setOnClickListener {
                if (tab != i) {
                    val dir = if (i > tab) 1 else -1
                    tab = i
                    render(true, dir)
                    updateLeft()
                    leftAssemble()
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

    private fun render(animate: Boolean = false, dir: Int = 1) {
        // finish/cancel any running slide so stale pages are removed
        val kids = ArrayList<View>()
        for (i in 0 until host.childCount) kids.add(host.getChildAt(i))
        for (k in kids) k.animate().cancel()
        val old = if (host.childCount > 0) host.getChildAt(host.childCount - 1) else null

        val page = when (tab) {
            0 -> timerPage()
            1 -> bagsPage()
            2 -> routinesPage()
            3 -> statsPage()
            else -> detoxPage()
        }
        if (!animate || old == null || host.width == 0 || host.height == 0) {
            host.removeAllViews()
            host.addView(page, FrameLayout.LayoutParams(MATCH, MATCH))
            return
        }
        while (host.childCount > 1) host.removeViewAt(0)
        host.addView(page, FrameLayout.LayoutParams(MATCH, MATCH))
        val dist = (if (isWide) host.height else host.width).toFloat()
        val d = dir.toFloat()
        val dur = 380L
        val ip = DecelerateInterpolator(1.6f)
        if (isWide) {
            page.translationY = d * dist
            page.animate().translationY(0f).setDuration(dur).setInterpolator(ip).start()
            old.animate().translationY(-d * dist).setDuration(dur).setInterpolator(ip)
                .withEndAction { host.removeView(old) }.start()
        } else {
            page.translationX = d * dist
            page.animate().translationX(0f).setDuration(dur).setInterpolator(ip).start()
            old.animate().translationX(-d * dist).setDuration(dur).setInterpolator(ip)
                .withEndAction { host.removeView(old) }.start()
        }
    }

    /** Left panel: bags / digits rebuild themselves from scattered pixel blocks. */
    private fun leftAssemble() {
        when (tab) {
            1 -> bagsView.assemble()
            4 -> detoxView.assemble()
            else -> pc.assemble()
        }
    }

    // ---------- Clock colour ----------
    private fun timeColor(min: Int): Int {
        val t = (min / 180f).coerceIn(0f, 1f)
        val a = Color.parseColor("#5B4A44")
        val r = (Color.red(a) + (Color.red(C_ORANGE) - Color.red(a)) * t).toInt()
        val g = (Color.green(a) + (Color.green(C_ORANGE) - Color.green(a)) * t).toInt()
        val b = (Color.blue(a) + (Color.blue(C_ORANGE) - Color.blue(a)) * t).toInt()
        return Color.rgb(r, g, b)
    }

    private fun clockColorTo(target: Int) {
        if (target == clockTarget) return
        clockTarget = target
        clockAnim?.cancel()
        val a = ValueAnimator.ofObject(ArgbEvaluator(), clockColor, target)
        a.duration = 260L
        a.addUpdateListener {
            clockColor = it.animatedValue as Int
            if (tab == 0 && focus == null && !Store.active(this)) pc.show(durMin, clockColor)
        }
        a.start()
        clockAnim = a
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
        pc.visibility = if (tab == 1 || tab == 4) View.GONE else View.VISIBLE
        detoxView.visibility = if (tab == 4) View.VISIBLE else View.GONE
        bagsView.visibility = if (tab == 1) View.VISIBLE else View.GONE
        dots.visibility = if (tab == 3) View.VISIBLE else View.GONE
        controls.visibility = if (tab == 0) View.VISIBLE else View.GONE
        scene.visibility = if (tab == 0) View.VISIBLE else View.GONE
        caption.visibility = if (tab == 3) View.VISIBLE else View.GONE
        pillRow.visibility = if (tab == 1 || tab == 2 || tab == 4) View.GONE else View.VISIBLE
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
                    clockColorTo(timeColor(durMin))
                    pc.show(durMin, clockColor)
                    scene.progress = 0.3f
                }
            }
            1 -> {
                bagsView.active = bagSel
            }
            2 -> {
                val on = Store.rtOn(this, rtSel)
                pc.showPair(
                    Store.rtStart(this, rtSel), Store.rtEnd(this, rtSel),
                    if (on) C_ORANGE else C_GRAY,
                    if (on) C_ORANGE else Color.parseColor("#F2B89C")
                )
            }
            4 -> {
                detoxView.shorts = Store.detoxShorts(this)
                detoxView.reels = Store.detoxReels(this)
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
        if (!fromRuler) animateRuler(durMin)
    }

    private fun animateRuler(target: Int) {
        val rl = ruler ?: return
        rulerAnim?.cancel()
        val from = rulerShown
        val to = target.toFloat().coerceAtLeast(5f)
        if (Math.abs(to - from) < 0.5f) {
            rulerShown = to
            rl.setMinutes(target)
            return
        }
        val a = ValueAnimator.ofFloat(from, to)
        a.duration = 380L
        a.interpolator = DecelerateInterpolator()
        a.addUpdateListener {
            val f = it.animatedValue as Float
            rulerShown = f
            rl.setMinutes(f.toInt())
        }
        a.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                if (rulerShown == to) rl.setMinutes(target)
            }
        })
        a.start()
        rulerAnim = a
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

        rulerAnim?.cancel()
        val rl = RulerView(this) { m ->
            durMin = m
            rulerShown = m.toFloat()
            refreshDur(true)
            updateLeft()
        }
        rl.onBlocked = {
            rulerAnim?.cancel()
            if (locked()) {
                lockedToast()
                true
            } else {
                false
            }
        }
        rl.setMinutes(durMin)
        rulerShown = durMin.toFloat()
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
            syncFocus()
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
    private val C_CELL = Color.parseColor("#F3EFEF")
    private val C_DIMTXT = Color.parseColor("#A9A2A0")

    private fun bagTab(i: Int): TextView {
        if (i == 0) {
            val t = tile("Bag 1", false) {}
            t.setTextColor(Color.WHITE)
            t.background = rounded(C_ORANGE, 26)
            return t
        }
        val t = tile("Bag ${i + 1}", false) {
            toast("Bag ${i + 1} locked  \u00B7  future me padhai wale apps ke liye")
        }
        t.setTextColor(C_DIMTXT)
        t.background = rounded(C_CELL, 26)
        return t
    }

    private fun bagsPage(): View {
        val col = vbox()
        val panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.background = rounded(C_TILE, 28)
        panel.setPadding(dp(8), dp(8), dp(8), dp(8))

        panel.addView(hrow(bagTab(0), bagTab(1), bagTab(2)), lpV(dp(52)))
        val list = Store.bag(this, 0).toList().sorted()
        val cells = ArrayList<View>()
        for (i in 0 until 6) {
            val cell = FrameLayout(this)
            cell.background = rounded(C_CELL, 20)
            if (i < Store.MAX_BAG_APPS) {
                if (i < list.size) {
                    val iv = ImageView(this)
                    iv.setImageDrawable(icon(list[i]))
                    val lp = FrameLayout.LayoutParams(dp(44), dp(44))
                    lp.gravity = Gravity.CENTER
                    cell.addView(iv, lp)
                }
                cell.setOnClickListener { showPicker() }
            } else {
                val lk = IconView(this, IconView.LOCK)
                lk.tint = Color.parseColor("#F0A98B")
                val lp = FrameLayout.LayoutParams(dp(30), dp(34))
                lp.gravity = Gravity.CENTER
                cell.addView(lk, lp)
                cell.setOnClickListener { toast("Bag 1 me sirf ${Store.MAX_BAG_APPS} apps le sakte ho") }
            }
            cells.add(cell)
        }
        panel.addView(hrow(cells[0], cells[1], cells[2]), lpV())
        panel.addView(hrow(cells[3], cells[4], cells[5]), lpV())
        val pl = LinearLayout.LayoutParams(MATCH, 0, 1f)
        pl.setMargins(0, dp(5), 0, dp(5))
        col.addView(panel, pl)

        col.addView(hrow(
            tile("Add / remove apps  ${list.size}/${Store.MAX_BAG_APPS}", true) { showPicker() },
            tile("Clear bag", false) {
                if (locked()) {
                    lockedToast()
                } else {
                    Store.setBag(this, 0, emptySet())
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
        val set = HashSet(Store.bag(this, 0))
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
            if (p in set) {
                set.remove(p)
            } else if (set.size >= Store.MAX_BAG_APPS) {
                toast("Bag 1 me max ${Store.MAX_BAG_APPS} apps")
            } else {
                set.add(p)
            }
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
        val bNone = Button(this)
        bNone.text = "Clear all"
        bNone.setOnClickListener {
            set.clear()
            ad.notifyDataSetChanged()
        }
        bar.addView(bNone, LinearLayout.LayoutParams(0, WRAP, 1f))
        col.addView(bar)
        col.addView(lv, LinearLayout.LayoutParams(MATCH, dp(360)))

        AlertDialog.Builder(this)
            .setTitle("Bag 1 apps (max ${Store.MAX_BAG_APPS})")
            .setView(col)
            .setPositiveButton("Save") { _, _ ->
                Store.setBag(this, 0, set)
                render()
                updateLeft()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---------- Page 2: Routines (blockit style) ----------
    private fun rtDuration(): CharSequence {
        val on = Store.rtOn(this, rtSel)
        val d = ((Store.rtEnd(this, rtSel) - Store.rtStart(this, rtSel)) + 1440) % 1440
        val dur = if (d / 60 == 0) "${d % 60}m" else "${d / 60}h ${d % 60}m"
        val head = if (on) "On" else "Off"
        val sb = SpannableStringBuilder(head)
        sb.setSpan(ForegroundColorSpan(if (on) C_TXT else C_DIMTXT), 0, head.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append("  \u00B7  ")
        val st = sb.length
        sb.append(dur)
        sb.setSpan(ForegroundColorSpan(if (on) C_ORANGE else Color.parseColor("#F2B89C")), st, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
    }

    private fun rtCard(i: Int): View {
        val sel = rtSel == i
        val on = Store.rtOn(this, i)
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER
        card.background = if (sel) rounded(Color.parseColor("#E0DAD9"), 20) else null
        val top = TextView(this)
        top.text = "ROUTINE ${i + 1}"
        top.textSize = 11f
        top.letterSpacing = 0.15f
        top.setTextColor(if (sel) C_TXT else C_DIMTXT)
        top.gravity = Gravity.CENTER
        val tm = TextView(this)
        tm.text = fmt(Store.rtStart(this, i))
        tm.textSize = 24f
        tm.setTextColor(if (sel) C_TXT else C_DIMTXT)
        tm.gravity = Gravity.CENTER
        rtTimeTvs[i] = tm
        val line = View(this)
        line.background = rounded(if (on) C_ORANGE else Color.parseColor("#D2CBCA"), 2)
        card.addView(top, LinearLayout.LayoutParams(WRAP, WRAP))
        val tl = LinearLayout.LayoutParams(WRAP, WRAP)
        tl.setMargins(0, dp(6), 0, dp(10))
        card.addView(tm, tl)
        card.addView(line, LinearLayout.LayoutParams(dp(56), dp(3)))
        card.setOnClickListener {
            if (rtSel != i) {
                rtSel = i
                render()
                updateLeft()
                pc.assemble()
            }
        }
        return card
    }

    private fun routinesPage(): View {
        val panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.background = rounded(C_TILE, 28)
        panel.setPadding(dp(10), dp(10), dp(10), dp(10))

        // top: 3 routine cards + lock / on-off column
        val on = Store.rtOn(this, rtSel)
        val topRow = LinearLayout(this)
        topRow.orientation = LinearLayout.HORIZONTAL
        for (i in 0..2) {
            val lp = LinearLayout.LayoutParams(0, MATCH, 1f)
            lp.setMargins(dp(3), 0, dp(3), 0)
            topRow.addView(rtCard(i), lp)
        }
        val side = LinearLayout(this)
        side.orientation = LinearLayout.VERTICAL
        val lockBox = FrameLayout(this)
        val lbg = GradientDrawable()
        lbg.setColor(Color.TRANSPARENT)
        lbg.cornerRadius = dp(14).toFloat()
        lbg.setStroke(dp(1), C_LINE)
        lockBox.background = lbg
        val lk = IconView(this, IconView.LOCK)
        lk.tint = C_DARK
        val lkp = FrameLayout.LayoutParams(dp(20), dp(22))
        lkp.gravity = Gravity.CENTER
        lockBox.addView(lk, lkp)
        lockBox.setOnClickListener { toast("Coming soon") }
        val onBox = View(this)
        onBox.background = rounded(if (on) C_ORANGE else Color.parseColor("#7A706D"), 14)
        onBox.setOnClickListener {
            if (locked()) {
                lockedToast()
            } else {
                Store.setRtOn(this, rtSel, !Store.rtOn(this, rtSel))
                render()
                updateLeft()
            }
        }
        val sl1 = LinearLayout.LayoutParams(MATCH, 0, 1f)
        sl1.bottomMargin = dp(4)
        val sl2 = LinearLayout.LayoutParams(MATCH, 0, 1.3f)
        sl2.topMargin = dp(4)
        side.addView(lockBox, sl1)
        side.addView(onBox, sl2)
        val sp = LinearLayout.LayoutParams(dp(60), MATCH)
        sp.setMargins(dp(6), 0, 0, 0)
        topRow.addView(side, sp)
        panel.addView(topRow, LinearLayout.LayoutParams(MATCH, 0, 1.7f))

        // days: vertical on/off sliders
        val names = arrayOf("S", "M", "T", "W", "T", "F", "S")
        val mask0 = Store.rtDays(this, rtSel)
        val letters = ArrayList<TextView>()
        val dayRow = LinearLayout(this)
        dayRow.orientation = LinearLayout.HORIZONTAL
        for (d in 0..6) {
            val dOn = ((mask0 shr d) and 1) == 1
            val c = LinearLayout(this)
            c.orientation = LinearLayout.VERTICAL
            c.gravity = Gravity.CENTER_HORIZONTAL
            val tv = TextView(this)
            tv.text = names[d]
            tv.textSize = 20f
            tv.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            tv.gravity = Gravity.CENTER
            tv.setTextColor(if (dOn) C_TXT else C_DIMTXT)
            letters.add(tv)
            val tog = DayToggleView(this, dOn) { nv ->
                if (locked()) {
                    lockedToast()
                    false
                } else {
                    val m = Store.rtDays(this, rtSel)
                    Store.setRtDays(this, rtSel, if (nv) m or (1 shl d) else m and (1 shl d).inv())
                    letters[d].setTextColor(if (nv) C_TXT else C_DIMTXT)
                    updateLeft()
                    true
                }
            }
            c.addView(tv, LinearLayout.LayoutParams(MATCH, dp(40)))
            c.addView(tog, LinearLayout.LayoutParams(MATCH, 0, 1f))
            val lp = LinearLayout.LayoutParams(0, MATCH, 1f)
            lp.setMargins(dp(5), 0, dp(5), 0)
            dayRow.addView(c, lp)
        }
        val dl = LinearLayout.LayoutParams(MATCH, 0, 3.6f)
        dl.setMargins(0, dp(8), 0, dp(4))
        panel.addView(dayRow, dl)

        // status: Off · 5h 35m
        val st = TextView(this)
        st.gravity = Gravity.CENTER
        st.textSize = 18f
        st.text = rtDuration()
        rtStatusTv = st
        panel.addView(st, LinearLayout.LayoutParams(MATCH, dp(34)))

        // bottom: start / end rulers
        fun rulerCol(label: String, start: Boolean): LinearLayout {
            val box = LinearLayout(this)
            box.orientation = LinearLayout.VERTICAL
            val init = if (start) Store.rtStart(this, rtSel) else Store.rtEnd(this, rtSel)
            val r = HRulerView(this, init) { v ->
                if (start) Store.setRtStart(this, rtSel, v) else Store.setRtEnd(this, rtSel, v)
                rtStatusTv?.text = rtDuration()
                rtTimeTvs[rtSel]?.text = fmt(Store.rtStart(this, rtSel))
                updateLeft()
            }
            r.onBlocked = {
                if (locked()) {
                    lockedToast()
                    true
                } else {
                    false
                }
            }
            val lb = TextView(this)
            lb.text = label
            lb.textSize = 12f
            lb.letterSpacing = 0.2f
            lb.gravity = Gravity.CENTER
            lb.setTextColor(C_TXT)
            box.addView(r, LinearLayout.LayoutParams(MATCH, 0, 1f))
            box.addView(lb, LinearLayout.LayoutParams(MATCH, dp(28)))
            return box
        }
        panel.addView(hrow(rulerCol("START", true), rulerCol("END", false)), LinearLayout.LayoutParams(MATCH, 0, 1.5f))
        return panel
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

    // ---------- Page 4: Detox (only Shorts / Reels blocked) ----------
    private fun detoxCard(title: String, pkg: String, on: Boolean, onClick: () -> Unit): View {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.background = rounded(if (on) C_ORANGE else C_CELL, 24)
        card.setPadding(dp(20), dp(10), dp(20), dp(10))
        val iv = ImageView(this)
        iv.setImageDrawable(icon(pkg))
        card.addView(iv, LinearLayout.LayoutParams(dp(52), dp(52)))
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), 0, 0, 0)
        val t = TextView(this)
        t.text = title
        t.textSize = 20f
        t.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        t.setTextColor(if (on) Color.WHITE else C_TXT)
        val st = TextView(this)
        st.text = if (on) "BLOCKED" else "ALLOWED"
        st.textSize = 12f
        st.letterSpacing = 0.2f
        st.setTextColor(if (on) Color.WHITE else C_DIMTXT)
        col.addView(t)
        col.addView(st)
        card.addView(col, LinearLayout.LayoutParams(0, WRAP, 1f))
        card.setOnClickListener { onClick() }
        card.setOnTouchListener { v, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
            }
            false
        }
        return card
    }

    private fun detoxPage(): View {
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
        val panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.background = rounded(C_TILE, 28)
        panel.setPadding(dp(10), dp(10), dp(10), dp(10))
        val s = detoxCard("YouTube Shorts", "com.google.android.youtube", Store.detoxShorts(this)) {
            Store.setDetoxShorts(this, !Store.detoxShorts(this))
            render()
            updateLeft()
        }
        val r = detoxCard("Instagram Reels", "com.instagram.android", Store.detoxReels(this)) {
            Store.setDetoxReels(this, !Store.detoxReels(this))
            render()
            updateLeft()
        }
        val l1 = LinearLayout.LayoutParams(MATCH, 0, 1f)
        l1.setMargins(0, dp(4), 0, dp(4))
        val l2 = LinearLayout.LayoutParams(MATCH, 0, 1f)
        l2.setMargins(0, dp(4), 0, dp(4))
        panel.addView(s, l1)
        panel.addView(r, l2)
        val pl = LinearLayout.LayoutParams(MATCH, 0, 1f)
        pl.setMargins(0, dp(5), 0, dp(5))
        col.addView(panel, pl)
        col.addView(tile("Sirf Shorts / Reels band  \u00B7  baaki sab chalega", false) {}, lpV(dp(64)))
        return col
    }

    // ---------- Focus (black) screen ----------
    private fun syncFocus() {
        val on = Store.sessionOn(this)
        if (on && focus == null) {
            enterFocus()
        } else if (!on && focus != null) {
            exitFocus()
        }
    }

    private fun enterFocus() {
        val f = FocusScreen(this) { syncFocus() }
        focus = f
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0
        val lp = window.attributes
        lp.screenBrightness = 0.08f
        window.attributes = lp
        setContentView(f.view)
        f.update()
    }

    private fun exitFocus() {
        focus = null
        val lp = window.attributes
        lp.screenBrightness = -1f
        window.attributes = lp
        window.statusBarColor = origStatus
        window.navigationBarColor = origNav
        window.decorView.systemUiVisibility = origUi
        setContentView(normalRoot)
        lastRunning = Store.active(this)
        render()
        updateLeft()
        styleRail()
    }

    // ---------- Lifecycle ----------
    private val tick = object : Runnable {
        override fun run() {
            syncFocus()
            Notif.syncSession(this@MainActivity)
            val f = focus
            if (f != null) {
                f.update()
            } else {
                val r = Store.active(this@MainActivity)
                if (r != lastRunning) {
                    lastRunning = r
                    render()
                }
                updateLeft()
            }
            ui.postDelayed(this, 1000)
        }
    }

    override fun onResume() {
        super.onResume()
        Notif.cancelSetup(this)
        Notif.syncSession(this)
        lastRunning = Store.active(this)
        render()
        updateLeft()
        styleRail()
        syncFocus()
        ui.removeCallbacks(tick)
        ui.postDelayed(tick, 1000)
    }

    override fun onPause() {
        super.onPause()
        if (!accEnabled()) Notif.showAccessibilityNeeded(this)
        ui.removeCallbacks(tick)
    }
}
