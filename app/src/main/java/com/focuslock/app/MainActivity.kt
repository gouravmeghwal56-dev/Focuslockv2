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
    private lateinit var detoxView: DetoxArtView
    private lateinit var detoxBox: LinearLayout
    private lateinit var leftCard: LinearLayout
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

    /** App abhi phone mein installed hai? (cache nahi, har baar fresh check) */
    private fun installed(pkg: String): Boolean =
        try { packageManager.getApplicationInfo(pkg, 0); true } catch (e: Exception) { false }

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

    private fun showSettings() {
        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setItems(arrayOf("Profile", "Theme", "Accessibility settings")) { _, i ->
                when (i) {
                    0 -> showProfile()
                    1 -> toast("Theme: coming soon")
                    else -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
            .show()
    }

    private fun showProfile() {
        val et = EditText(this)
        et.hint = "Your name"
        et.setText(Store.profileName(this))
        et.isSingleLine = true
        val box = FrameLayout(this)
        box.setPadding(dp(20), dp(8), dp(20), 0)
        box.addView(et, FrameLayout.LayoutParams(MATCH, WRAP))
        AlertDialog.Builder(this)
            .setTitle("Profile")
            .setView(box)
            .setPositiveButton("Save") { _, _ -> Store.setProfileName(this, et.text.toString()) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun buildHeader(): FrameLayout {
        val f = FrameLayout(this)
        val menu = circleBtn(IconView.MENU) { toast("Coming soon") }
        val gear = circleBtn(IconView.GEAR) { showSettings() }
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
        leftCard = card
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
        detoxView = DetoxArtView(this)
        detoxBox = LinearLayout(this)
        detoxBox.orientation = LinearLayout.VERTICAL
        detoxBox.addView(detoxView, LinearLayout.LayoutParams(MATCH, 0, 1f))
        detoxBox.addView(QuoteView(this), LinearLayout.LayoutParams(MATCH, dp(120)))
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
        content.addView(detoxBox, LinearLayout.LayoutParams(MATCH, 0, 1f))
        content.addView(controls, LinearLayout.LayoutParams(MATCH, WRAP))
        content.addView(dots, LinearLayout.LayoutParams(MATCH, dp(30)))
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
        // kam time = dark grey (#4D4846), 180 min pe poora orange (#F45516), beech me seedha blend
        val t = (min / 180f).coerceIn(0f, 1f)
        val a = Color.parseColor("#4D4846")
        val z = Color.parseColor("#F45516")
        val r = (Color.red(a) + (Color.red(z) - Color.red(a)) * t).toInt()
        val g = (Color.green(a) + (Color.green(z) - Color.green(a)) * t).toInt()
        val b = (Color.blue(a) + (Color.blue(z) - Color.blue(a)) * t).toInt()
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
        detoxBox.visibility = if (tab == 4) View.VISIBLE else View.GONE
        leftCard.background = rounded(if (tab == 4) C_DXLEFT else C_CARD, 28)
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
            }
            else -> updateStatsLeft()
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
        val to = target.toFloat().coerceAtLeast(1f)
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
        durMin = (durMin + d).coerceIn(1, 1440)
        refreshDur(false)
        updateLeft()
    }

    private fun customDialog() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.setPadding(dp(22), dp(20), dp(22), dp(18))
        root.background = rounded(Color.parseColor("#F6F2F1"), 28)

        val title = TextView(this)
        title.text = "Custom time"
        title.textSize = 18f
        title.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        title.setTextColor(C_DARK)
        root.addView(title, LinearLayout.LayoutParams(WRAP, WRAP))

        // live preview (same pixel font as the timer)
        val pv = PixelClockView(this)
        val pvLp = LinearLayout.LayoutParams(dp(260), dp(80))
        pvLp.topMargin = dp(10)
        root.addView(pv, pvLp)

        // scroll wheels: HRS | MIN  (no typing)
        val wheels = LinearLayout(this)
        wheels.orientation = LinearLayout.HORIZONTAL
        wheels.gravity = Gravity.CENTER
        fun picker(max: Int, cur: Int): android.widget.NumberPicker {
            val np = android.widget.NumberPicker(this)
            np.minValue = 0
            np.maxValue = max
            np.value = cur
            np.wrapSelectorWheel = false
            np.setFormatter { String.format("%02d", it) }
            np.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            return np
        }
        val ph = picker(24, (durMin / 60).coerceIn(0, 24))
        val pm = picker(if (ph.value == 24) 0 else 59, if (durMin / 60 >= 24) 0 else durMin % 60)
        fun label(t: String): TextView {
            val tv = TextView(this)
            tv.text = t
            tv.textSize = 12f
            tv.letterSpacing = 0.2f
            tv.setTextColor(C_DIMTXT)
            tv.gravity = Gravity.CENTER
            return tv
        }
        fun col(lbl: String, np: android.widget.NumberPicker): LinearLayout {
            val c = LinearLayout(this)
            c.orientation = LinearLayout.VERTICAL
            c.gravity = Gravity.CENTER_HORIZONTAL
            c.addView(label(lbl), LinearLayout.LayoutParams(WRAP, WRAP))
            c.addView(np, LinearLayout.LayoutParams(dp(90), dp(130)))
            return c
        }
        val sep = TextView(this)
        sep.text = ":"
        sep.textSize = 28f
        sep.setTextColor(C_DARK)
        sep.gravity = Gravity.CENTER
        wheels.addView(col("HRS", ph), LinearLayout.LayoutParams(WRAP, WRAP))
        wheels.addView(sep, LinearLayout.LayoutParams(dp(28), WRAP))
        wheels.addView(col("MIN", pm), LinearLayout.LayoutParams(WRAP, WRAP))
        val wLp = LinearLayout.LayoutParams(WRAP, WRAP)
        wLp.topMargin = dp(6)
        root.addView(wheels, wLp)

        fun refresh() {
            // 24h = max, so minutes must be 00 there
            if (ph.value == 24) {
                pm.value = 0
                pm.maxValue = 0
            } else {
                pm.maxValue = 59
            }
            pv.show(ph.value * 60 + pm.value, C_ORANGE)
        }
        ph.setOnValueChangedListener { _, _, _ -> refresh() }
        pm.setOnValueChangedListener { _, _, _ -> refresh() }
        refresh()

        // quick chips: +5m +10m +30m +1h
        val chips = LinearLayout(this)
        chips.orientation = LinearLayout.HORIZONTAL
        chips.gravity = Gravity.CENTER
        for ((txt, add) in listOf("+5m" to 5, "+10m" to 10, "+30m" to 30, "+1h" to 60)) {
            val ch = TextView(this)
            ch.text = txt
            ch.textSize = 14f
            ch.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            ch.setTextColor(C_DARK)
            ch.gravity = Gravity.CENTER
            ch.background = rounded(C_TILE, 18)
            ch.setOnClickListener {
                val t = (ph.value * 60 + pm.value + add).coerceAtMost(1440)
                ph.value = t / 60
                refresh()
                pm.value = if (t >= 1440) 0 else t % 60
                refresh()
            }
            val lp = LinearLayout.LayoutParams(dp(62), dp(38))
            lp.leftMargin = dp(4)
            lp.rightMargin = dp(4)
            chips.addView(ch, lp)
        }
        val cLp = LinearLayout.LayoutParams(WRAP, WRAP)
        cLp.topMargin = dp(10)
        root.addView(chips, cLp)

        val hint = TextView(this)
        hint.text = "Max 24 hours"
        hint.textSize = 12f
        hint.setTextColor(C_DIMTXT)
        hint.gravity = Gravity.CENTER
        val hLp = LinearLayout.LayoutParams(WRAP, WRAP)
        hLp.topMargin = dp(10)
        root.addView(hint, hLp)

        val dlg = AlertDialog.Builder(this).setView(root).create()

        val btns = LinearLayout(this)
        btns.orientation = LinearLayout.HORIZONTAL
        btns.gravity = Gravity.CENTER
        val cancel = TextView(this)
        cancel.text = "Cancel"
        cancel.textSize = 15f
        cancel.setTextColor(C_DARK)
        cancel.gravity = Gravity.CENTER
        cancel.background = rounded(C_TILE, 24)
        cancel.setOnClickListener { dlg.dismiss() }
        val ok = TextView(this)
        ok.text = "Set"
        ok.textSize = 15f
        ok.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        ok.setTextColor(Color.WHITE)
        ok.gravity = Gravity.CENTER
        ok.background = rounded(C_DARK, 24)
        ok.setOnClickListener {
            val t = ph.value * 60 + pm.value
            if (t in 1..1440) {
                durMin = t
                refreshDur(false)
                updateLeft()
                dlg.dismiss()
            } else {
                toast("Pick between 1 minute and 24 hours")
            }
        }
        val b1 = LinearLayout.LayoutParams(dp(110), dp(48))
        b1.rightMargin = dp(8)
        btns.addView(cancel, b1)
        btns.addView(ok, LinearLayout.LayoutParams(dp(130), dp(48)))
        val bLp = LinearLayout.LayoutParams(WRAP, WRAP)
        bLp.topMargin = dp(14)
        root.addView(btns, bLp)

        dlg.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dlg.show()
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
    private var stMode = 1                       // 0 = Day, 1 = Month, 2 = Year
    private var stCal: Calendar = Calendar.getInstance()

    private fun hm(min: Int): String = if (min >= 60) "${min / 60}h ${min % 60}m" else "${min}m"

    private fun monthSum(y: Int, m: Int): Int {
        val c = Calendar.getInstance()
        c.clear()
        c.set(y, m, 1)
        var sum = 0L
        for (d in 1..c.getActualMaximum(Calendar.DAY_OF_MONTH)) sum += Store.dayMinutes(this, y, m, d)
        return sum.toInt()
    }

    private fun yearSum(y: Int): Int {
        var sum = 0
        for (m in 0..11) sum += monthSum(y, m)
        return sum
    }

    private fun stTotal(): Int {
        val y = stCal.get(Calendar.YEAR)
        val m = stCal.get(Calendar.MONTH)
        return when (stMode) {
            0 -> Store.dayMinutes(this, y, m, stCal.get(Calendar.DAY_OF_MONTH)).toInt()
            1 -> monthSum(y, m)
            else -> yearSum(y)
        }
    }

    private fun stLabel(): String = when (stMode) {
        0 -> SimpleDateFormat("EEEE d MMM yyyy", Locale.getDefault()).format(stCal.time).uppercase()
        1 -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(stCal.time).uppercase()
        else -> stCal.get(Calendar.YEAR).toString()
    }

    private fun stThisPeriodWord(): String {
        val now = Calendar.getInstance()
        val y = stCal.get(Calendar.YEAR)
        val m = stCal.get(Calendar.MONTH)
        return when (stMode) {
            0 -> if (y == now.get(Calendar.YEAR) && stCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) "TODAY" else "THAT DAY"
            1 -> if (y == now.get(Calendar.YEAR) && m == now.get(Calendar.MONTH)) "THIS MONTH" else "THAT MONTH"
            else -> if (y == now.get(Calendar.YEAR)) "THIS YEAR" else "THAT YEAR"
        }
    }

    /** Days (up to today) of the scope: Day -> its month, Month -> month, Year -> year. */
    private fun stScopeDays(): List<Int> {
        val now = Calendar.getInstance()
        val y = stCal.get(Calendar.YEAR)
        val out = ArrayList<Int>()
        val months = if (stMode == 2) 0..11 else stCal.get(Calendar.MONTH)..stCal.get(Calendar.MONTH)
        for (m in months) {
            val c = Calendar.getInstance()
            c.clear()
            c.set(y, m, 1)
            for (d in 1..c.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                c.set(y, m, d)
                if (c.timeInMillis > now.timeInMillis) break
                out.add(Store.dayMinutes(this, y, m, d).toInt())
            }
        }
        return out
    }

    private fun stStreak(): Int {
        val c = Calendar.getInstance()
        if (Store.dayMinutes(this, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)) <= 0L) {
            c.add(Calendar.DAY_OF_YEAR, -1)   // aaj abhi 0 hai to kal se gino
        }
        var n = 0
        while (n < 3650 && Store.dayMinutes(this, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)) > 0L) {
            n++
            c.add(Calendar.DAY_OF_YEAR, -1)
        }
        return n
    }

    private fun stShift(dir: Int) {
        val c = stCal.clone() as Calendar
        when (stMode) {
            0 -> c.add(Calendar.DAY_OF_YEAR, dir)
            1 -> { c.set(Calendar.DAY_OF_MONTH, 1); c.add(Calendar.MONTH, dir) }
            else -> { c.set(Calendar.MONTH, 0); c.set(Calendar.DAY_OF_MONTH, 1); c.add(Calendar.YEAR, dir) }
        }
        if (c.timeInMillis > System.currentTimeMillis()) return
        stCal = c
        refreshStats()
    }

    private fun stSetMode(m: Int) {
        val now = Calendar.getInstance()
        val sameMonth = stCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) && stCal.get(Calendar.MONTH) == now.get(Calendar.MONTH)
        val sameYear = stCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        if ((m != 2 && sameMonth) || (m == 2 && sameYear)) stCal = now
        stMode = m
        refreshStats()
    }

    private fun refreshStats() {
        if (tab != 3) return
        host.removeAllViews()
        host.addView(statsPage(), FrameLayout.LayoutParams(MATCH, MATCH))
        updateLeft()
    }

    /** Left card: big digits = period total, squares = days (or months in Year mode), tap to drill down. */
    private fun updateStatsLeft() {
        val now = Calendar.getInstance()
        val y = stCal.get(Calendar.YEAR)
        val m = stCal.get(Calendar.MONTH)
        val total = stTotal()
        pc.show(total, C_ORANGE)

        val vals: IntArray
        var future = Int.MAX_VALUE
        var sel: Int
        if (stMode == 2) {
            vals = IntArray(12) { monthSum(y, it) }
            if (y == now.get(Calendar.YEAR)) future = now.get(Calendar.MONTH) + 1
            sel = m
        } else {
            val c = Calendar.getInstance()
            c.clear()
            c.set(y, m, 1)
            val n = c.getActualMaximum(Calendar.DAY_OF_MONTH)
            vals = IntArray(n) { Store.dayMinutes(this, y, m, it + 1).toInt() }
            if (y == now.get(Calendar.YEAR) && m == now.get(Calendar.MONTH)) future = now.get(Calendar.DAY_OF_MONTH)
            sel = if (stMode == 0) stCal.get(Calendar.DAY_OF_MONTH) - 1 else -1
        }
        val mx = vals.maxOrNull() ?: 0
        dots.total = vals.size
        dots.levels = IntArray(vals.size) { i ->
            if (mx <= 0 || vals[i] <= 0) 0 else Math.ceil(3.0 * vals[i] / mx).toInt().coerceIn(1, 3)
        }
        dots.futureFrom = future
        dots.selected = sel
        dots.onPick = { i ->
            if (stMode == 2) {
                stCal = Calendar.getInstance().also { it.clear(); it.set(y, i, 1) }
                if (y == now.get(Calendar.YEAR) && i == now.get(Calendar.MONTH)) stCal = Calendar.getInstance()
                stMode = 1
            } else {
                stCal = Calendar.getInstance().also { it.clear(); it.set(y, m, i + 1, 0, 0) }
                stMode = 0
            }
            refreshStats()
        }
        dots.invalidate()
        caption.text = stLabel() + "  \u00B7  " + (if (total <= 0) "NO FOCUS YET" else "HH:MM")
    }

    private fun statTile(top: String, bottom: String): TextView {
        val sb = SpannableStringBuilder()
        sb.append(top)
        sb.setSpan(AbsoluteSizeSpan(22, true), 0, top.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(StyleSpan(Typeface.BOLD), 0, top.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append("\n").append(bottom)
        val t = tile(sb, false) {}
        t.textSize = 11f
        t.letterSpacing = 0.08f
        return t
    }

    private fun monthPickerDialog() {
        val now = Calendar.getInstance()
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.setPadding(dp(22), dp(20), dp(22), dp(18))
        root.background = rounded(Color.parseColor("#F6F2F1"), 28)
        val title = TextView(this)
        title.text = "Pick month"
        title.textSize = 18f
        title.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        title.setTextColor(C_DARK)
        root.addView(title, LinearLayout.LayoutParams(WRAP, WRAP))

        val minY = now.get(Calendar.YEAR) - 5
        val py = android.widget.NumberPicker(this)
        py.minValue = minY
        py.maxValue = now.get(Calendar.YEAR)
        py.value = stCal.get(Calendar.YEAR).coerceIn(minY, now.get(Calendar.YEAR))
        py.wrapSelectorWheel = false
        val names = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val pmo = android.widget.NumberPicker(this)
        pmo.minValue = 0
        pmo.maxValue = 11
        pmo.displayedValues = names
        pmo.value = stCal.get(Calendar.MONTH)
        pmo.wrapSelectorWheel = false
        py.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        pmo.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER
        row.addView(pmo, LinearLayout.LayoutParams(dp(100), dp(130)))
        val gap = View(this)
        row.addView(gap, LinearLayout.LayoutParams(dp(20), 1))
        row.addView(py, LinearLayout.LayoutParams(dp(110), dp(130)))
        val rLp = LinearLayout.LayoutParams(WRAP, WRAP)
        rLp.topMargin = dp(10)
        root.addView(row, rLp)

        val dlg = AlertDialog.Builder(this).setView(root).create()
        val btns = LinearLayout(this)
        btns.gravity = Gravity.CENTER
        val cancel = TextView(this)
        cancel.text = "Cancel"
        cancel.textSize = 15f
        cancel.setTextColor(C_DARK)
        cancel.gravity = Gravity.CENTER
        cancel.background = rounded(C_TILE, 24)
        cancel.setOnClickListener { dlg.dismiss() }
        val ok = TextView(this)
        ok.text = "Go"
        ok.textSize = 15f
        ok.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        ok.setTextColor(Color.WHITE)
        ok.gravity = Gravity.CENTER
        ok.background = rounded(C_DARK, 24)
        ok.setOnClickListener {
            val c = Calendar.getInstance()
            c.clear()
            c.set(py.value, pmo.value, 1, 0, 0)
            if (c.timeInMillis > System.currentTimeMillis()) {
                toast("That month hasn't started yet")
            } else {
                stCal = if (py.value == now.get(Calendar.YEAR) && pmo.value == now.get(Calendar.MONTH)) Calendar.getInstance() else c
                if (stMode == 0) stMode = 1
                dlg.dismiss()
                refreshStats()
            }
        }
        val b1 = LinearLayout.LayoutParams(dp(110), dp(48))
        b1.rightMargin = dp(8)
        btns.addView(cancel, b1)
        btns.addView(ok, LinearLayout.LayoutParams(dp(130), dp(48)))
        val bLp = LinearLayout.LayoutParams(WRAP, WRAP)
        bLp.topMargin = dp(14)
        root.addView(btns, bLp)
        dlg.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dlg.show()
    }

    private fun statsPage(): View {
        val col = vbox()
        val total = stTotal()

        // top bar:  <  NOVEMBER 2026  >
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        val prev = tile("\u2039", false) { stShift(-1) }
        prev.textSize = 28f
        val next = tile("\u203A", false) { stShift(1) }
        next.textSize = 28f
        val probe = stCal.clone() as Calendar
        when (stMode) {
            0 -> probe.add(Calendar.DAY_OF_YEAR, 1)
            1 -> { probe.set(Calendar.DAY_OF_MONTH, 1); probe.add(Calendar.MONTH, 1) }
            else -> { probe.set(Calendar.MONTH, 0); probe.set(Calendar.DAY_OF_MONTH, 1); probe.add(Calendar.YEAR, 1) }
        }
        next.alpha = if (probe.timeInMillis > System.currentTimeMillis()) 0.3f else 1f
        val lbl = tile(stLabel(), false) { monthPickerDialog() }
        lbl.letterSpacing = 0.1f
        bar.addView(prev, LinearLayout.LayoutParams(dp(56), MATCH))
        val ml = LinearLayout.LayoutParams(0, MATCH, 1f)
        ml.setMargins(dp(6), 0, dp(6), 0)
        bar.addView(lbl, ml)
        bar.addView(next, LinearLayout.LayoutParams(dp(56), MATCH))
        col.addView(bar, lpV(dp(64)))

        // middle: Day / Month / Year  |  total + small stats
        val mid = LinearLayout(this)
        mid.orientation = LinearLayout.HORIZONTAL
        val seg = LinearLayout(this)
        seg.orientation = LinearLayout.VERTICAL
        for ((i, n) in listOf("Day", "Month", "Year").withIndex()) {
            seg.addView(tile(n, stMode == i) { stSetMode(i) }, lpV())
        }
        val right = LinearLayout(this)
        right.orientation = LinearLayout.VERTICAL
        val big = tile(twoLine(hm(total), if (total <= 0) "No focus yet \u00B7 start a session" else stThisPeriodWord()), true) {}
        right.addView(big, lpV())

        val days = stScopeDays()
        val best = days.maxOrNull() ?: 0
        val avg = if (days.isEmpty()) 0 else days.sum() / days.size
        val small = LinearLayout(this)
        small.orientation = LinearLayout.HORIZONTAL
        val streak = stStreak()
        val items = listOf(
            statTile(if (streak == 1) "1 day" else "$streak days", "STREAK"),
            statTile(hm(best), "BEST DAY"),
            statTile(hm(avg), "AVG / DAY")
        )
        for (t in items) {
            val lp = LinearLayout.LayoutParams(0, MATCH, 1f)
            lp.setMargins(dp(3), 0, dp(3), 0)
            small.addView(t, lp)
        }
        right.addView(small, lpV())

        val sl = LinearLayout.LayoutParams(0, MATCH, 1f)
        sl.setMargins(0, 0, dp(6), 0)
        mid.addView(seg, sl)
        mid.addView(right, LinearLayout.LayoutParams(0, MATCH, 3f))
        col.addView(mid, LinearLayout.LayoutParams(MATCH, 0, 1f))

        val foot = tile("Minutes are counted when a timer starts", false) {}
        foot.textSize = 13f
        col.addView(foot, lpV(dp(48)))
        return col
    }

    // ---------- Page 4: Detox (block doomscrolling) ----------
    private val C_DXLEFT = Color.parseColor("#FCFAFA")
    private val C_DXPANEL = Color.parseColor("#EBE6E6")
    private val C_DXCARD = Color.parseColor("#F3EFEF")

    private fun appLabel(pkg: String): String = allApps.firstOrNull { it.pkg == pkg }?.label ?: pkg

    private fun detoxCard(title: String, pkg: String, on: Boolean, sub: String?, onLong: (() -> Unit)?, onClick: () -> Unit): View {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.background = rounded(if (on) C_ORANGE else C_DXCARD, 26)
        card.setPadding(dp(22), dp(10), dp(22), dp(10))
        val iv = ImageView(this)
        iv.setImageDrawable(icon(pkg))
        card.addView(iv, LinearLayout.LayoutParams(dp(54), dp(54)))
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(18), 0, 0, 0)
        val t = TextView(this)
        t.text = title
        t.textSize = 22f
        t.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        t.setTextColor(if (on) Color.WHITE else C_TXT)
        t.maxLines = 1
        val st = TextView(this)
        st.text = sub ?: (if (on) "BLOCKED" else "ALLOWED")
        st.textSize = 12f
        st.letterSpacing = 0.25f
        st.setTextColor(if (on) Color.WHITE else C_DIMTXT)
        col.addView(t)
        col.addView(st)
        card.addView(col, LinearLayout.LayoutParams(0, WRAP, 1f))
        card.setOnClickListener { onClick() }
        if (onLong != null) {
            card.setOnLongClickListener {
                onLong()
                true
            }
        }
        card.setOnTouchListener { v, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(90).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
            }
            false
        }
        return card
    }

    // ScrollView aur list ek hi rehte hain, render() par sirf cards dobara bharte hain -> scroll position nahi hilti
    private var dxSv: HalfScrollView? = null
    private var dxList: LinearLayout? = null

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

        // header: Block Doomscrolling
        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        head.background = rounded(C_DXPANEL, 24)
        head.setPadding(dp(14), dp(6), dp(14), dp(6))
        val hi = IconView(this, IconView.INFINITY)
        hi.tint = Color.WHITE
        hi.setPadding(dp(8), dp(8), dp(8), dp(8))
        hi.background = rounded(C_ORANGE, 12)
        head.addView(hi, LinearLayout.LayoutParams(dp(42), dp(42)))
        val ht = TextView(this)
        ht.text = "Block Doomscrolling"
        ht.textSize = 22f
        ht.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        ht.setTextColor(C_TXT)
        ht.setPadding(dp(16), 0, 0, 0)
        head.addView(ht, LinearLayout.LayoutParams(WRAP, WRAP))
        val hl = LinearLayout.LayoutParams(MATCH, dp(60))
        hl.bottomMargin = dp(8)
        col.addView(head, hl)

        // list of cards (2 visible, swipe up/down for more)
        val sv = dxSv ?: HalfScrollView(this).also {
            it.isVerticalScrollBarEnabled = false
            dxSv = it
        }
        val list = dxList ?: LinearLayout(this).also {
            it.orientation = LinearLayout.VERTICAL
            dxList = it
        }
        list.removeAllViews()
        fun addCard(v: View) {
            val lp = LinearLayout.LayoutParams(MATCH, dp(200))
            lp.setMargins(0, dp(6), 0, dp(6))
            list.addView(v, lp)
        }
        if (installed("com.google.android.youtube")) {
            addCard(detoxCard("YouTube Shorts", "com.google.android.youtube", Store.detoxShorts(this), null, { igDbgMenu("com.google.android.youtube") }) {
                Store.setDetoxShorts(this, !Store.detoxShorts(this))
                render()
                updateLeft()
            })
        }
        if (installed("com.instagram.android")) {
            addCard(detoxCard("Instagram Reels", "com.instagram.android", Store.detoxReels(this), null, { igDbgMenu("com.instagram.android") }) {
                Store.setDetoxReels(this, !Store.detoxReels(this))
                render()
                updateLeft()
            })
        }
        val on = Store.detoxAppsOn(this)
        val full = Store.detoxFull(this)
        for (pkg in Store.detoxApps(this).sortedBy { appLabel(it).lowercase() }) {
            val sub = when {
                pkg !in on -> "ALLOWED"
                pkg in full -> "FULL BLOCK"
                else -> "REELS ONLY"
            }
            addCard(detoxCard(appLabel(pkg), pkg, pkg in on, sub, { detoxAppMenu(pkg) }) {
                Store.setDetoxAppOn(this, pkg, pkg !in Store.detoxAppsOn(this))
                render()
                updateLeft()
            })
        }
        (sv.parent as? android.view.ViewGroup)?.removeView(sv)
        if (list.parent == null) sv.addView(list, FrameLayout.LayoutParams(MATCH, WRAP))
        sv.fit()
        val panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.background = rounded(C_DXPANEL, 28)
        panel.setPadding(dp(8), dp(4), dp(8), dp(4))
        panel.addView(sv, LinearLayout.LayoutParams(MATCH, MATCH))
        col.addView(panel, LinearLayout.LayoutParams(MATCH, 0, 1f))

        // Add Apps: tap ya right slide dono
        val addRow = FrameLayout(this)
        addRow.background = rounded(C_DXPANEL, 26)
        val inner = LinearLayout(this)
        inner.orientation = LinearLayout.HORIZONTAL
        inner.gravity = Gravity.CENTER_VERTICAL
        inner.setPadding(dp(12), dp(8), dp(18), dp(8))
        val plus = IconView(this, IconView.PLUS)
        plus.tint = C_TXT
        plus.setPadding(dp(14), dp(14), dp(14), dp(14))
        plus.background = rounded(Color.parseColor("#E0DAD9"), 16)
        inner.addView(plus, LinearLayout.LayoutParams(dp(56), dp(56)))
        val at = TextView(this)
        at.text = "Add Apps"
        at.textSize = 21f
        at.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        at.setTextColor(C_TXT)
        at.setPadding(dp(18), 0, 0, 0)
        inner.addView(at, LinearLayout.LayoutParams(0, WRAP, 1f))
        val ch = IconView(this, IconView.CHEVRON)
        ch.tint = C_DIMTXT
        inner.addView(ch, LinearLayout.LayoutParams(dp(24), dp(24)))
        addRow.addView(inner, FrameLayout.LayoutParams(MATCH, MATCH))
        var downX = 0f
        var moved = false
        addRow.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - downX
                    if (dx > dp(8)) {
                        moved = true
                        plus.translationX = dx.coerceIn(0f, (addRow.width - plus.width - dp(30)).toFloat())
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val dx = ev.rawX - downX
                    val doIt = if (moved) dx > addRow.width * 0.4f else true
                    plus.animate().translationX(0f).setDuration(200).start()
                    if (doIt) pickDetoxApp()
                }
                MotionEvent.ACTION_CANCEL -> plus.animate().translationX(0f).setDuration(200).start()
            }
            true
        }
        val al = LinearLayout.LayoutParams(MATCH, dp(76))
        al.topMargin = dp(8)
        col.addView(addRow, al)

        // info
        val info = LinearLayout(this)
        info.orientation = LinearLayout.HORIZONTAL
        info.gravity = Gravity.CENTER_VERTICAL
        info.background = rounded(C_DXPANEL, 24)
        info.setPadding(dp(20), 0, dp(16), 0)
        val ii = IconView(this, IconView.INFO)
        ii.tint = C_TXT
        info.addView(ii, LinearLayout.LayoutParams(dp(26), dp(26)))
        val itx = TextView(this)
        itx.text = "Only Shorts & Reels are blocked. Everything else works normally."
        itx.textSize = 14f
        itx.setTextColor(C_TXT)
        itx.setPadding(dp(14), 0, 0, 0)
        info.addView(itx, LinearLayout.LayoutParams(0, WRAP, 1f))
        val il = LinearLayout.LayoutParams(MATCH, dp(64))
        il.topMargin = dp(8)
        col.addView(info, il)
        return col
    }

    private fun igDbgMenu(pkg: String) {
        AlertDialog.Builder(this)
            .setTitle("Debug")
            .setItems(arrayOf("Capture IDs (20 sec)", "Show captured IDs / last block")) { _, i ->
                if (i == 0) {
                    Store.dxDbgStart(this, pkg)
                    Toast.makeText(this, "20 sec: app kholo aur wo page kholo (blocking band rahegi)", Toast.LENGTH_LONG).show()
                } else {
                    showCapturedIds()
                }
            }
            .show()
    }

    private fun detoxAppMenu(pkg: String) {
        val name = appLabel(pkg)
        AlertDialog.Builder(this)
            .setTitle(name)
            .setItems(
                arrayOf(
                    "Block only Reels / Shorts",
                    "Block whole app",
                    "Capture IDs (20 sec)",
                    "Show captured IDs",
                    "Remove app"
                )
            ) { _, i ->
                when (i) {
                    0 -> {
                        Store.setDetoxFull(this, pkg, false)
                        render()
                    }
                    1 -> {
                        Store.setDetoxFull(this, pkg, true)
                        render()
                    }
                    2 -> {
                        Store.dxDbgStart(this, pkg)
                        Toast.makeText(this, "20 sec: ab $name kholo aur Reels/Spotlight screen pe jao", Toast.LENGTH_LONG).show()
                    }
                    3 -> showCapturedIds()
                    else -> AlertDialog.Builder(this)
                        .setTitle("Remove $name?")
                        .setPositiveButton("Remove") { _, _ ->
                            Store.removeDetoxApp(this, pkg)
                            render()
                            updateLeft()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
            .show()
    }

    private fun showCapturedIds() {
        val ids = Store.dxDbgIds(this)
        val text = if (ids.isEmpty()) "Kuch capture nahi hua. Capture IDs dabake 20 sec ke andar app kholo." else ids.joinToString("\n")
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 12f
        tv.setTextIsSelectable(true)
        tv.setPadding(dp(20), dp(10), dp(20), dp(10))
        val sc = ScrollView(this)
        sc.addView(tv)
        AlertDialog.Builder(this)
            .setTitle("Captured IDs (${ids.size})")
            .setView(sc)
            .setPositiveButton("Copy") { _, _ ->
                val cm = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("ids", text))
                Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun pickDetoxApp() {
        val have = Store.detoxApps(this)
        val base = allApps.filter {
            it.pkg != packageName && it.pkg !in have &&
                it.pkg != "com.google.android.youtube" && it.pkg != "com.instagram.android"
        }
        var shown: List<App> = base
        var dlg: AlertDialog? = null

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(12), dp(8), dp(12), 0)
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
                    addView(ImageView(this@MainActivity), LinearLayout.LayoutParams(dp(40), dp(40)))
                    val tv = TextView(this@MainActivity)
                    tv.textSize = 16f
                    tv.setPadding(dp(12), 0, dp(8), 0)
                    addView(tv, LinearLayout.LayoutParams(0, WRAP, 1f))
                }
                (row.getChildAt(0) as ImageView).setImageDrawable(icon(a.pkg))
                (row.getChildAt(1) as TextView).text = a.label
                return row
            }
        }
        lv.adapter = ad
        lv.setOnItemClickListener { _, _, i, _ ->
            Store.addDetoxApp(this, shown[i].pkg)
            dlg?.dismiss()
            render()
            updateLeft()
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString() ?: ""
                shown = if (q.isBlank()) base else base.filter { it.label.contains(q, ignoreCase = true) }
                ad.notifyDataSetChanged()
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
        col.addView(lv, LinearLayout.LayoutParams(MATCH, dp(360)))
        dlg = AlertDialog.Builder(this)
            .setTitle("Add app to Detox")
            .setView(col)
            .setNegativeButton("Cancel", null)
            .create()
        dlg?.show()
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
        Ambient.stop()
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

    override fun onDestroy() {
        Ambient.stop()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        if (!accEnabled()) Notif.showAccessibilityNeeded(this)
        ui.removeCallbacks(tick)
    }
}
