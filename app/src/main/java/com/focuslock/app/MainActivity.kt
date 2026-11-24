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
    private var gearBtn: IconView? = null
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
        // Art themes use light glass tiles, so the selected state keeps dark text (white would vanish).
        t.setTextColor(if (selected && Art.get(this, "bg") == null) Color.WHITE else C_TXT)
        t.background = tileBg(selected)
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

    private fun tileBg(sel: Boolean, custom: Boolean = false, kind: String? = null): android.graphics.drawable.Drawable {
        val uw = C_THEME_ID in GLASS_THEMES
        val r = dp(if (uw) 28 else 20).toFloat()
        val cardArt = Art.get(this, "card")
        // Per-tile art: t15 / t45 / hours, else a shared "tile" image if the theme has one.
        val tileArt = if (kind != null) (Art.get(this, kind) ?: Art.get(this, "tile")) else null
        if (tileArt != null) return TileBg(C_TILE, C_TILE, 0, r, tileArt, false, 0x01FFFFFF, if (sel) C_PRIMARY else 0)
        val cardVeil = if (C_THEME_ID in LIGHT_CARD_THEMES) 0x14FFFFFF else 0
        if (custom && cardArt != null) return TileBg(C_PRIMARY, C_PRIMARY_TOP, 0, r, cardArt, false, cardVeil, if (sel) C_PRIMARY else 0)
        if (Art.get(this, "bg") != null) {
            val soft = (C_TILE and 0x00FFFFFF) or (0xC8 shl 24)
            if (uw) return TileBg(if (C_THEME_ID != "underwater") (C_TILE and 0x00FFFFFF) or (0xEB shl 24) else 0xEBE3F1EE.toInt(), 0xF2FFFFFF.toInt(), C_HILL, r, null, true, 0, if (sel) C_PRIMARY else 0)
            return TileBg(soft, soft, C_HILL, r, null, true, 0, if (sel) C_PRIMARY else 0)
        }
        return if (sel) TileBg(C_PRIMARY, C_PRIMARY_TOP, 0, r) else TileBg(C_TILE, C_TILE, C_HILL, r)
    }

    private fun styleTile(t: TextView?, sel: Boolean) {
        if (t == null) return
        val artTheme = Art.get(this, "bg") != null
        val cardArt = t === tCustom && Art.get(this, "card") != null
        t.setTextColor(if (cardArt && C_THEME_ID in LIGHT_CARD_THEMES) C_TXT else if (cardArt || (sel && !artTheme)) Color.WHITE else C_TXT)
        t.background = tileBg(sel, t === tCustom, if (t === t15) "t15" else if (t === t45) "t45" else if (t === t180) "hours" else null)
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
        Themes.load(this)
        loadApps()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 7)
        }
        bagSel = Store.activeBag(this)
        val wide = resources.displayMetrics.widthPixels > resources.displayMetrics.heightPixels
        isWide = wide
        buildUi(false)
        origStatus = window.statusBarColor
        origNav = window.navigationBarColor
        origUi = window.decorView.systemUiVisibility
    }

    /** Status/navigation bar take the theme colour (no system blue); art themes use the art's own edge colour. */
    private fun applyBars(bgArt: android.graphics.Bitmap?) {
        val top = (if (bgArt != null) bgArt.getPixel(bgArt.width / 2, 2) else C_BG) or (0xFF shl 24)
        val bot = (if (bgArt != null) bgArt.getPixel(bgArt.width / 2, bgArt.height - 3) else C_BG) or (0xFF shl 24)
        window.statusBarColor = top
        window.navigationBarColor = bot
        var ui = window.decorView.systemUiVisibility
        ui = if (Color.luminance(top) > 0.5f) ui or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
             else ui and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        ui = if (Color.luminance(bot) > 0.5f) ui or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
             else ui and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        window.decorView.systemUiVisibility = ui
        origStatus = top
        origNav = bot
        origUi = ui
    }

    /** Builds the whole screen from the current C_* colours. Called again for a live theme change. */
    private fun buildUi(fade: Boolean) {
        val wide = isWide
        railBtns.clear()
        clockColor = C_ORANGE
        clockTarget = C_ORANGE

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        val bgArt = Art.get(this, "bg")
        if (bgArt != null) root.background = ArtBg(C_BG, bgArt) else root.setBackgroundColor(C_BG)
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
        applyBars(bgArt)
        setContentView(root)
        if (fade) {
            root.alpha = 0f
            root.animate().alpha(1f).setDuration(220).start()
            root.post {
                render()
                updateLeft()
                styleRail()
            }
        }
    }

    // ---------- Theme picker (live preview) ----------
    private fun showThemePicker() {
        val savedId = Themes.savedId(this)
        var curId = savedId
        var confirmed = false
        val boxBg = Color.parseColor("#F6F2F1")

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(18), dp(16), dp(18), dp(14))
        root.background = rounded(boxBg, 28)

        val title = TextView(this)
        title.text = "Theme"
        title.textSize = 16f
        title.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        title.setTextColor(Color.parseColor("#403B39"))
        title.gravity = Gravity.CENTER
        root.addView(title, LinearLayout.LayoutParams(MATCH, WRAP))

        val grid = LinearLayout(this)
        grid.orientation = LinearLayout.VERTICAL
        val cards = ArrayList<Pair<AppTheme, LinearLayout>>()

        fun styleCards() {
            for ((t, c) in cards) {
                val g = rounded(t.bg, 16)
                g.setStroke(if (t.id == curId) dp(3) else dp(1), if (t.id == curId) t.accent else t.line)
                c.background = g
            }
        }
        fun pick(t: AppTheme) {
            if (t.id == curId) return
            curId = t.id
            Themes.apply(t)
            styleCards()
            buildUi(true)
        }
        fun card(t: AppTheme): LinearLayout {
            val c = LinearLayout(this)
            c.orientation = LinearLayout.VERTICAL
            c.gravity = Gravity.CENTER_HORIZONTAL
            c.setPadding(dp(8), dp(10), dp(8), dp(10))
            val n = TextView(this)
            n.text = t.name
            n.textSize = 14f
            n.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            n.setTextColor(t.ink)
            n.gravity = Gravity.CENTER
            c.addView(n, LinearLayout.LayoutParams(WRAP, WRAP))
            val dots = LinearLayout(this)
            dots.orientation = LinearLayout.HORIZONTAL
            dots.gravity = Gravity.CENTER
            for (col in intArrayOf(t.primary, t.accent, t.ink, t.tile)) {
                val d = View(this)
                val g = oval(col)
                g.setStroke(dp(1), t.line)
                d.background = g
                val dl = LinearLayout.LayoutParams(dp(18), dp(18))
                dl.setMargins(dp(3), dp(7), dp(3), 0)
                dots.addView(d, dl)
            }
            c.addView(dots, LinearLayout.LayoutParams(WRAP, WRAP))
            c.setOnClickListener { pick(t) }
            return c
        }

        val list = Themes.all
        var i = 0
        while (i < list.size) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            for (j in 0 until 2) {
                if (i + j < list.size) {
                    val t = list[i + j]
                    val c = card(t)
                    cards.add(t to c)
                    val l = LinearLayout.LayoutParams(0, WRAP, 1f)
                    l.setMargins(dp(4), dp(4), dp(4), dp(4))
                    row.addView(c, l)
                } else {
                    row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
                }
            }
            grid.addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
            i += 2
        }
        styleCards()

        val sv = android.widget.ScrollView(this)
        sv.addView(grid)
        val svLp = LinearLayout.LayoutParams(MATCH, dp(300))
        svLp.topMargin = dp(8)
        root.addView(sv, svLp)

        val dlg = AlertDialog.Builder(this).setView(root).create()

        val btns = LinearLayout(this)
        btns.orientation = LinearLayout.HORIZONTAL
        btns.gravity = Gravity.CENTER
        val cancel = TextView(this)
        cancel.text = "Cancel"
        cancel.textSize = 15f
        cancel.setTextColor(Color.parseColor("#403B39"))
        cancel.gravity = Gravity.CENTER
        cancel.background = rounded(Color.parseColor("#EBE6E6"), 24)
        cancel.setOnClickListener { dlg.dismiss() }
        val ok = TextView(this)
        ok.text = "Apply"
        ok.textSize = 15f
        ok.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        ok.setTextColor(Color.WHITE)
        ok.gravity = Gravity.CENTER
        ok.background = rounded(Color.parseColor("#403B39"), 24)
        ok.setOnClickListener {
            confirmed = true
            Themes.save(this, curId)
            dlg.dismiss()
        }
        val b1 = LinearLayout.LayoutParams(dp(110), dp(46))
        b1.rightMargin = dp(8)
        btns.addView(cancel, b1)
        btns.addView(ok, LinearLayout.LayoutParams(dp(130), dp(46)))
        val bLp = LinearLayout.LayoutParams(WRAP, WRAP)
        bLp.topMargin = dp(10)
        bLp.gravity = Gravity.CENTER_HORIZONTAL
        root.addView(btns, bLp)

        // cancel / tap outside => go back to the saved theme
        dlg.setOnDismissListener {
            if (!confirmed && curId != savedId) {
                Themes.apply(Themes.byId(savedId))
                buildUi(true)
            }
        }

        dlg.window?.let { w ->
            w.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            w.setDimAmount(0.10f)
            val land = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            if (land) {
                w.setGravity(Gravity.END or Gravity.CENTER_VERTICAL)
                val lp = w.attributes
                lp.x = dp(80)
                w.attributes = lp
            } else {
                w.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                val lp = w.attributes
                lp.y = dp(24)
                w.attributes = lp
            }
            w.setLayout(dp(300), WRAP)
        }
        dlg.show()
        // dobara kholne pe list upar nahi, chune hue theme ke paas khule
        sv.post {
            val sel = cards.firstOrNull { it.first.id == curId }?.second
            if (sel != null) {
                val row = sel.parent as View
                sv.scrollTo(0, (row.top - dp(40)).coerceAtLeast(0))
            }
        }
    }

    private fun circleBtn(kind: Int, onClick: () -> Unit): IconView {
        val v = IconView(this, kind)
        v.setPadding(dp(12), dp(12), dp(12), dp(12))
        v.background = oval(C_CARD)
        v.setOnClickListener { onClick() }
        return v
    }

    // ---------- Settings: upar right corner se khulta popup ----------
    private val REQ_BACKUP = 41
    private val REQ_RESTORE = 42

    private fun showSettings() {
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

    private fun showAbout() {
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

    private fun showBackupMenu() {
        AlertDialog.Builder(this)
            .setTitle("Data & Backup")
            .setItems(arrayOf("Backup Data", "Restore Data")) { _, i ->
                if (i == 0) startBackup() else startRestore()
            }
            .show()
    }

    private fun startBackup() {
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

    private fun startRestore() {
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
    private fun backupJson(): String {
        val prefs = JSONObject()
        val dir = java.io.File(applicationInfo.dataDir, "shared_prefs")
        val files = dir.listFiles() ?: emptyArray()
        for (f in files) {
            if (!f.name.endsWith(".xml")) continue
            val pname = f.name.removeSuffix(".xml")
            val sp = getSharedPreferences(pname, MODE_PRIVATE)
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

    private fun restoreJson(text: String): Boolean {
        return try {
            val root = JSONObject(text)
            if (root.optString("app") != "FocusLock") return false
            val prefs = root.getJSONObject("prefs")
            val names = prefs.keys()
            while (names.hasNext()) {
                val pname = names.next()
                val o = prefs.getJSONObject(pname)
                val ed = getSharedPreferences(pname, MODE_PRIVATE).edit()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri: Uri = data?.data ?: return
        if (resultCode != RESULT_OK) return
        if (requestCode == REQ_BACKUP) {
            try {
                contentResolver.openOutputStream(uri)?.use { it.write(backupJson().toByteArray(Charsets.UTF_8)) }
                toast("Backup save ho gaya")
            } catch (e: Exception) {
                toast("Backup fail ho gaya")
            }
        } else if (requestCode == REQ_RESTORE) {
            val text = try {
                contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) }
            } catch (e: Exception) {
                null
            }
            if (text != null && restoreJson(text)) {
                toast("Restore ho gaya, app dobara khul rahi hai")
                ui.postDelayed({
                    val li = packageManager.getLaunchIntentForPackage(packageName)
                    if (li != null) {
                        li.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(li)
                    }
                    Runtime.getRuntime().exit(0)
                }, 700L)
            } else {
                toast("Ye FocusLock ki backup file nahi lag rahi")
            }
        }
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
        gearBtn = gear
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
        if (Art.get(this, "bg") != null) {
            card.background = null
        } else {
            card.background = CardBg(C_CARD, dp(28).toFloat(), Art.get(this, "branch"))
            card.clipToOutline = true
        }

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
        play.background = rounded(C_PRIMARY, 28)
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
        card.addView(scene, LinearLayout.LayoutParams(MATCH, if (Art.get(this, "scene") != null || Art.get(this, "bg") != null) dp(170) else dp(110)))
        return card
    }

    private fun buildRail(wide: Boolean): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = if (wide) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER
        r.background = rounded(if (Art.get(this, "bg") != null) Color.parseColor("#80FFFFFF") else Color.parseColor("#FFFFFF"), 32)
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
            railBtns[i].background = if (i == tab) rounded(C_PRIMARY, 26) else null
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
        val a = C_CLK_A
        val z = C_CLK_Z
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
        leftCard.visibility = if (tab == 3) View.GONE else View.VISIBLE
        pc.visibility = if (tab == 1 || tab == 4) View.GONE else View.VISIBLE
        detoxBox.visibility = if (tab == 4) View.VISIBLE else View.GONE
        leftCard.background = if (tab != 4 && Art.get(this, "bg") != null) null else rounded(if (tab == 4) C_DXLEFT else C_CARD, 28)
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
                    if (on) C_ORANGE else C_SOFT
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
        val n = (durMin + d).coerceIn(1, 1440)
        if (n == durMin) {
            ruler?.shake()
            return
        }
        durMin = n
        refreshDur(false)
        updateLeft()
    }

    private fun customDialog() {
        val origDur = durMin
        var confirmed = false

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.setPadding(dp(22), dp(18), dp(22), dp(16))
        root.background = rounded(C_BG, 28)

        val title = TextView(this)
        title.text = "Custom time"
        title.textSize = 16f
        title.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        title.setTextColor(C_DARK)
        root.addView(title, LinearLayout.LayoutParams(WRAP, WRAP))

        // scroll wheels: HRS | MIN  (infinite: 24 -> 00 and 59 -> 00)
        val wheels = LinearLayout(this)
        wheels.orientation = LinearLayout.HORIZONTAL
        wheels.gravity = Gravity.CENTER
        fun picker(max: Int, cur: Int): android.widget.NumberPicker {
            val np = android.widget.NumberPicker(this)
            np.minValue = 0
            np.maxValue = max
            np.value = cur
            np.wrapSelectorWheel = true
            np.setFormatter { String.format("%02d", it) }
            np.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            return np
        }
        val ph = picker(24, (durMin / 60).coerceIn(0, 24))
        val pm = picker(59, if (durMin / 60 >= 24) 0 else durMin % 60)
        fun label(t: String): TextView {
            val tv = TextView(this)
            tv.text = t
            tv.textSize = 11f
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
        sep.textSize = 26f
        sep.setTextColor(C_DARK)
        sep.gravity = Gravity.CENTER
        wheels.addView(col("HRS", ph), LinearLayout.LayoutParams(WRAP, WRAP))
        wheels.addView(sep, LinearLayout.LayoutParams(dp(28), WRAP))
        wheels.addView(col("MIN", pm), LinearLayout.LayoutParams(WRAP, WRAP))
        val wLp = LinearLayout.LayoutParams(WRAP, WRAP)
        wLp.topMargin = dp(6)
        root.addView(wheels, wLp)

        val ok = TextView(this)
        fun total() = ph.value * 60 + pm.value

        fun refresh() {
            // 24h = max, so minutes must be 00 there
            if (ph.value == 24 && pm.value != 0) pm.value = 0
            val t = total()
            ok.alpha = if (t in 1..1440) 1f else 0.4f
            // live update on the main page behind the dialog
            if (t in 1..1440 && t != durMin) {
                durMin = t
                refreshDur(false)
                updateLeft()
            }
        }
        ph.setOnValueChangedListener { _, _, nv ->
            if (nv == 24) pm.value = 0
            refresh()
        }
        pm.setOnValueChangedListener { _, _, _ ->
            if (ph.value == 24) pm.value = 0
            refresh()
        }

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
                val t = (total() + add).coerceAtMost(1440)
                ph.value = t / 60
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
        hLp.topMargin = dp(8)
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
        ok.text = "Set"
        ok.textSize = 15f
        ok.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        ok.setTextColor(Color.WHITE)
        ok.gravity = Gravity.CENTER
        ok.background = rounded(C_PRIMARY, 24)
        ok.setOnClickListener {
            val t = total()
            if (t in 1..1440) {
                confirmed = true
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

        // cancel / tap outside => put the old duration back
        dlg.setOnDismissListener {
            if (!confirmed && durMin != origDur) {
                durMin = origDur
                refreshDur(false)
                updateLeft()
            }
        }

        refresh()
        dlg.window?.let { w ->
            w.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            // keep the page behind visible so the live update can be seen
            w.setDimAmount(0.12f)
            val land = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            if (land) {
                w.setGravity(Gravity.END or Gravity.CENTER_VERTICAL)
                val lp = w.attributes
                lp.x = dp(80)
                w.attributes = lp
            } else {
                w.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                val lp = w.attributes
                lp.y = dp(24)
                w.attributes = lp
            }
        }
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
        styleTile(d, !preset)
        styleTile(a, durMin == 15)
        styleTile(b, durMin == 45)
        styleTile(c, durMin == 180)

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
        // Music app add hai par Notification access off hai: session se PEHLE allow karne ka popup
        val ui = getSharedPreferences("focuslock_ui", MODE_PRIVATE)
        if (Store.soundApps(this).isNotEmpty() && !Media.hasAccess(this) && !ui.getBoolean("noAccessAsk", false)) {
            AlertDialog.Builder(this)
                .setTitle("Music controls")
                .setMessage("Allow Notification access for FocusLock to show the track and play/pause during a session. Once a strict session starts, Settings can't be opened.")
                .setPositiveButton("Allow") { _, _ ->
                    try { startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } catch (e: Exception) { }
                }
                .setNeutralButton("Start anyway") { _, _ ->
                    startSessionConfirmed(go)
                }
                .setNegativeButton("Don't ask again") { _, _ ->
                    ui.edit().putBoolean("noAccessAsk", true).apply()
                    startSessionConfirmed(go)
                }
                .show()
            return
        }
        startSessionConfirmed(go)
    }

    private fun startSessionConfirmed(go: () -> Unit) {
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
        val slotList = BagSlots.get(this)
        val filled = slotList.count { it.isNotBlank() }
        val cells = ArrayList<View>()
        for (i in 0 until 6) {
            val cell = FrameLayout(this)
            cell.background = rounded(C_CELL, 20)
            if (i < Store.MAX_BAG_APPS) {
                val pk = slotList[i]
                if (pk.isNotBlank()) {
                    val iv = ImageView(this)
                    iv.setImageDrawable(icon(pk))
                    val lp = FrameLayout.LayoutParams(dp(44), dp(44))
                    lp.gravity = Gravity.CENTER
                    cell.addView(iv, lp)
                } else {
                    val plus = TextView(this)
                    plus.text = "+"
                    plus.textSize = 34f
                    plus.setTextColor(C_DIMTXT)
                    plus.gravity = Gravity.CENTER
                    cell.addView(plus, FrameLayout.LayoutParams(MATCH, MATCH))
                }
                cell.setOnClickListener { showPicker(i) }
            } else {
                val lk = IconView(this, IconView.LOCK)
                lk.tint = C_SOFT
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
            tile("Add / remove apps  ${filled}/${Store.MAX_BAG_APPS}", true) { showPicker(-1) },
            tile("Clear bag", false) {
                if (locked()) {
                    lockedToast()
                } else {
                    BagSlots.save(this, List(Store.MAX_BAG_APPS) { "" })
                    render()
                    updateLeft()
                }
            }
        ), lpV(dp(80)))
        return col
    }

    private fun showPicker(slot: Int = -1) {
        if (locked()) {
            lockedToast()
            return
        }
        val slots = BagSlots.get(this).toMutableList()
        val target = if (slot in 0 until Store.MAX_BAG_APPS) slot else {
            val e = slots.indexOf("")
            if (e >= 0) e else 0
        }
        var shown: List<App> = allApps
        val dlg = android.app.Dialog(this)
        dlg.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(22), dp(12), dp(22), dp(8))
        val bgd = GradientDrawable()
        bgd.setColor(C_BG)
        val rr = dp(28).toFloat()
        bgd.cornerRadii = floatArrayOf(rr, rr, rr, rr, 0f, 0f, 0f, 0f)
        root.background = bgd

        val handle = View(this)
        handle.background = rounded(C_LINE, 3)
        val hlp = LinearLayout.LayoutParams(dp(40), dp(4))
        hlp.gravity = Gravity.CENTER_HORIZONTAL
        root.addView(handle, hlp)

        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        val title = TextView(this)
        title.text = "Pack your bag"
        title.textSize = 22f
        title.setTextColor(C_TXT)
        head.addView(title, LinearLayout.LayoutParams(0, WRAP, 1f))
        val empty = TextView(this)
        empty.text = "\u2715  Empty slot"
        empty.textSize = 14f
        empty.setTextColor(C_TXT)
        empty.background = rounded(C_TILE, 20)
        empty.setPadding(dp(14), dp(9), dp(14), dp(9))
        empty.setOnClickListener {
            slots[target] = ""
            BagSlots.save(this, slots)
            dlg.dismiss()
            render()
            updateLeft()
        }
        head.addView(empty, LinearLayout.LayoutParams(WRAP, WRAP))
        val hl = LinearLayout.LayoutParams(MATCH, WRAP)
        hl.setMargins(0, dp(14), 0, dp(10))
        root.addView(head, hl)

        val search = EditText(this)
        search.hint = "Search apps"
        search.isSingleLine = true
        search.textSize = 16f
        search.background = rounded(C_TILE, 26)
        search.setPadding(dp(20), dp(12), dp(20), dp(12))
        root.addView(search, LinearLayout.LayoutParams(MATCH, WRAP))

        val lv = ListView(this)
        lv.divider = null
        val ad = object : BaseAdapter() {
            override fun getCount() = shown.size
            override fun getItem(i: Int): Any = shown[i]
            override fun getItemId(i: Int) = i.toLong()
            override fun getView(i: Int, v: View?, parent: ViewGroup?): View {
                val a = shown[i]
                val row = (v as? LinearLayout) ?: LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(8), dp(10), dp(8), dp(10))
                    val iv = ImageView(this@MainActivity)
                    addView(iv, LinearLayout.LayoutParams(dp(44), dp(44)))
                    val tv = TextView(this@MainActivity)
                    tv.textSize = 18f
                    tv.setTextColor(C_TXT)
                    tv.setPadding(dp(16), 0, dp(8), 0)
                    addView(tv, LinearLayout.LayoutParams(0, WRAP, 1f))
                }
                (row.getChildAt(0) as ImageView).setImageDrawable(icon(a.pkg))
                (row.getChildAt(1) as TextView).text = a.label
                return row
            }
        }
        lv.adapter = ad
        lv.setOnItemClickListener { _, _, i, _ ->
            val p = shown[i].pkg
            for (k in slots.indices) {
                if (slots[k] == p) slots[k] = ""
            }
            slots[target] = p
            BagSlots.save(this, slots)
            dlg.dismiss()
            render()
            updateLeft()
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
        root.addView(lv, LinearLayout.LayoutParams(MATCH, 0, 1f))

        dlg.setContentView(root)
        val dm = resources.displayMetrics
        val win = dlg.window
        if (win != null) {
            win.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            win.setGravity(Gravity.BOTTOM)
            win.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            win.setLayout(minOf(dp(560), dm.widthPixels - dp(24)), (dm.heightPixels * 0.82f).toInt())
        }
        dlg.show()
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
        sb.setSpan(ForegroundColorSpan(if (on) C_ORANGE else C_SOFT), st, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
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

    private fun streakTile(top: String, streak: Int): View {
        val t = statTile(top, "STREAK")
        t.setPadding(0, dp(34), 0, 0)
        val box = FrameLayout(this)
        box.addView(t, FrameLayout.LayoutParams(MATCH, MATCH))
        val fv = FlameView(this, streak > 0)
        val lp = FrameLayout.LayoutParams(dp(38), dp(48))
        lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        lp.topMargin = dp(12)
        box.addView(fv, lp)
        return box
    }

    private fun monthPickerDialog() {
        val now = Calendar.getInstance()
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.setPadding(dp(22), dp(20), dp(22), dp(18))
        root.background = rounded(C_BG, 28)
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
        ok.background = rounded(C_PRIMARY, 24)
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

    // ---------- Page 3: Stats (naya design: goal, streak, chart, goal completion) ----------
    private fun stGoal(): Int = getSharedPreferences("fl_ui", MODE_PRIVATE).getInt("goal_min", 180)

    private fun stTrack(): Int = (C_TXT and 0x00FFFFFF) or (0x1A shl 24)

    /** Ab tak ka sabse lamba streak (pichle ~3 saal). */
    private fun stBestStreak(): Int {
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
    private fun stSessions(): Pair<Int, Int> {
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

    private fun stText(s: CharSequence, sp: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = sp
        t.setTextColor(color)
        if (bold) t.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        return t
    }

    private fun stCard(): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        l.background = tileBg(false)
        l.setPadding(dp(16), dp(12), dp(16), dp(12))
        return l
    }

    private fun goalDialog() {
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
                getSharedPreferences("fl_ui", MODE_PRIVATE).edit().putInt("goal_min", total).apply()
                refreshStats()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun statsPage(): View {
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
            val ol = LinearLayout.La
