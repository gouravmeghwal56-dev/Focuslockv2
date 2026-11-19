package com.focuslock.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
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

private val C_ORANGE = Color.parseColor("#F55414")
private val C_GRAY = Color.parseColor("#ABABAB")
private val C_TILE = Color.parseColor("#DCDCDC")
private val C_DARK = Color.parseColor("#4D4848")
private val C_BG = Color.parseColor("#E0E0E0")
private val C_TXT = Color.parseColor("#4A4A4A")

// ---------- Custom views ----------

class PixelClockView(ctx: Context) : View(ctx) {
    private var text = "0000"
    private var color = C_ORANGE
    private val paint = Paint()
    private val masks = arrayOf("abcdef", "bc", "abdeg", "abcdg", "bcfg", "acdfg", "acdefg", "abc", "abcdefg", "abcdfg")

    fun show(totalMin: Int, c: Int) {
        val h = (totalMin / 60).coerceIn(0, 99)
        val m = totalMin % 60
        text = String.format("%02d%02d", h, m)
        color = c
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = color
        val w = width.toFloat()
        val h = height.toFloat()
        val dw = minOf(w / 4.83f, h / 1.7f)
        val dh = dw * 1.7f
        val t = dw * 0.22f
        val gap = dw * 0.12f
        val colonW = dw * 0.35f
        val total = dw * 4f + colonW + gap * 4f
        var x = (w - total) / 2f
        val y = (h - dh) / 2f
        for (i in 0 until 4) {
            if (i == 2) {
                canvas.drawRect(x + (colonW - t) / 2f, y + dh * 0.28f, x + (colonW + t) / 2f, y + dh * 0.28f + t, paint)
                canvas.drawRect(x + (colonW - t) / 2f, y + dh * 0.64f, x + (colonW + t) / 2f, y + dh * 0.64f + t, paint)
                x += colonW + gap
            }
            drawDigit(canvas, text[i] - '0', x, y, dw, dh, t)
            x += dw + gap
        }
    }

    private fun drawDigit(c: Canvas, d: Int, x: Float, y: Float, dw: Float, dh: Float, t: Float) {
        val m = masks[d]
        if ('a' in m) c.drawRect(x, y, x + dw, y + t, paint)
        if ('b' in m) c.drawRect(x + dw - t, y, x + dw, y + dh / 2f, paint)
        if ('c' in m) c.drawRect(x + dw - t, y + dh / 2f, x + dw, y + dh, paint)
        if ('d' in m) c.drawRect(x, y + dh - t, x + dw, y + dh, paint)
        if ('e' in m) c.drawRect(x, y + dh / 2f, x + t, y + dh, paint)
        if ('f' in m) c.drawRect(x, y, x + t, y + dh / 2f, paint)
        if ('g' in m) c.drawRect(x, y + (dh - t) / 2f, x + dw, y + (dh + t) / 2f, paint)
    }
}

class DotsView(ctx: Context) : View(ctx) {
    var total = 40
    var filled = 0
    var marks: BooleanArray? = null
    private val paint = Paint()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cell = w / total
        val s = minOf(cell * 0.62f, h)
        val y = (h - s) / 2f
        val mk = marks
        for (i in 0 until total) {
            val on = if (mk != null) (i < mk.size && mk[i]) else i < filled
            paint.color = if (on) C_ORANGE else C_TILE
            val x = i * cell + (cell - s) / 2f
            canvas.drawRect(x, y, x + s, y + s, paint)
        }
    }
}

class BagsView(ctx: Context) : View(ctx) {
    var active = 0
    private val paint = Paint()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cw = w / 3f
        for (i in 0..2) {
            paint.color = if (i == active) C_ORANGE else Color.parseColor("#7A706D")
            val bw = cw * 0.8f
            val u = bw / 3f
            val x0 = i * cw + cw * 0.1f
            val base = h * 0.6f
            canvas.drawRect(x0, base - u * 1.7f, x0 + u * 0.95f, base, paint)
            canvas.drawRect(x0 + u, base - u * 2.3f, x0 + u * 1.95f, base, paint)
            canvas.drawRect(x0 + u * 2f, base - u * 1.7f, x0 + u * 2.95f, base, paint)
            paint.color = Color.WHITE
            canvas.drawRect(x0 + u * 1.2f, base - u * 2.0f, x0 + u * 1.75f, base - u * 1.8f, paint)
            paint.color = if (i == active) C_ORANGE else Color.parseColor("#7A706D")
            val sq = u * 0.55f
            for (k in 0..2) {
                val sx = x0 + k * u * 1.05f + (u * 0.95f - sq) / 2f
                canvas.drawRect(sx, base + u * 0.35f, sx + sq, base + u * 0.35f + sq, paint)
            }
        }
    }
}

class SlideView(ctx: Context, private val onDone: () -> Unit) : View(ctx) {
    private val paint = Paint()
    private var knobX = 0f
    private var dragging = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val r = h * 0.3f
        paint.color = C_TILE
        canvas.drawRoundRect(RectF(0f, 0f, w, h), r, r, paint)
        paint.color = C_GRAY
        paint.textSize = h * 0.26f
        paint.textAlign = Paint.Align.CENTER
        paint.isAntiAlias = true
        val base = h / 2f - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText("SLIDE TO START  >>", w / 2f + h / 2f, base, paint)
        paint.color = C_DARK
        canvas.drawRoundRect(RectF(knobX, 0f, knobX + h, h), r, r, paint)
        paint.color = Color.WHITE
        canvas.drawText(">", knobX + h / 2f, base, paint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val h = height.toFloat()
        val maxX = width.toFloat() - h
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                dragging = e.x <= knobX + h * 1.3f
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging) {
                    knobX = (e.x - h / 2f).coerceIn(0f, maxX)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                val done = dragging && knobX >= maxX * 0.9f
                knobX = 0f
                dragging = false
                invalidate()
                if (done) onDone()
            }
            MotionEvent.ACTION_CANCEL -> {
                knobX = 0f
                dragging = false
                invalidate()
            }
        }
        return true
    }
}

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
    private lateinit var caption: TextView
    private val railBtns = ArrayList<TextView>()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_LONG).show()
    private fun fmt(min: Int) = String.format("%02d:%02d", min / 60, min % 60)
    private fun locked() = Store.active(this) && Store.strict(this)

    private fun rounded(color: Int, r: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(r).toFloat()
        return g
    }

    private fun tile(text: String, selected: Boolean, onClick: () -> Unit): TextView {
        val t = TextView(this)
        t.text = text
        t.gravity = Gravity.CENTER
        t.textSize = 17f
        t.setTextColor(if (selected) Color.WHITE else C_TXT)
        t.background = rounded(if (selected) C_DARK else C_TILE, 16)
        t.setOnClickListener { onClick() }
        return t
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
        c.setPadding(dp(4), dp(4), dp(4), dp(4))
        return c
    }

    private fun lpV(h: Int = 0): LinearLayout.LayoutParams {
        val lp = if (h == 0) LinearLayout.LayoutParams(MATCH, 0, 1f) else LinearLayout.LayoutParams(MATCH, h)
        lp.setMargins(0, dp(4), 0, dp(4))
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
        root.setPadding(dp(12), dp(30), dp(12), dp(12))

        val title = TextView(this)
        title.text = "focuslock"
        title.typeface = Typeface.MONOSPACE
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
        title.textSize = 22f
        title.letterSpacing = 0.15f
        title.gravity = Gravity.CENTER
        title.setTextColor(C_DARK)
        root.addView(title, LinearLayout.LayoutParams(MATCH, WRAP))

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
            body.addView(host, lp(0, MATCH, 1.15f))
            body.addView(rail, lp(WRAP, MATCH, 0f))
        } else {
            body.addView(left, lp(MATCH, 0, 0.9f))
            body.addView(host, lp(MATCH, 0, 1.4f))
            body.addView(rail, lp(MATCH, dp(68), 0f))
        }
        root.addView(body, LinearLayout.LayoutParams(MATCH, 0, 1f))
        setContentView(root)
    }

    private fun buildLeft(): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        l.background = rounded(Color.WHITE, 24)
        l.setPadding(dp(16), dp(16), dp(16), dp(16))
        pc = PixelClockView(this)
        dots = DotsView(this)
        bagsView = BagsView(this)
        caption = TextView(this)
        caption.gravity = Gravity.CENTER
        caption.textSize = 13f
        caption.letterSpacing = 0.1f
        caption.setTextColor(C_TXT)
        l.addView(pc, LinearLayout.LayoutParams(MATCH, 0, 1f))
        l.addView(bagsView, LinearLayout.LayoutParams(MATCH, 0, 1f))
        l.addView(dots, LinearLayout.LayoutParams(MATCH, dp(16)))
        val cl = LinearLayout.LayoutParams(MATCH, WRAP)
        cl.topMargin = dp(10)
        l.addView(caption, cl)
        return l
    }

    private fun buildRail(wide: Boolean): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = if (wide) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER
        r.background = rounded(Color.parseColor("#F2F2F2"), 30)
        val icons = arrayOf("\u23F1", "\uD83C\uDF92", "\u25A6", "\uD83D\uDCCA")
        railBtns.clear()
        for (i in 0..3) {
            val t = TextView(this)
            t.text = icons[i]
            t.textSize = 22f
            t.gravity = Gravity.CENTER
            t.setOnClickListener {
                tab = i
                render()
                updateLeft()
                styleRail()
            }
            railBtns.add(t)
            val lp = if (wide) LinearLayout.LayoutParams(dp(56), dp(56)) else LinearLayout.LayoutParams(0, dp(52), 1f)
            lp.setMargins(dp(6), dp(6), dp(6), dp(6))
            r.addView(t, lp)
        }
        styleRail()
        return r
    }

    private fun styleRail() {
        for (i in railBtns.indices) {
            railBtns[i].background = if (i == tab) rounded(C_DARK, 24) else null
            railBtns[i].setTextColor(if (i == tab) Color.WHITE else C_TXT)
        }
    }

    private fun render() {
        host.removeAllViews()
        val page = when (tab) {
            0 -> timerPage()
            1 -> bagsPage()
            2 -> routinesPage()
            else -> statsPage()
        }
        host.addView(page, FrameLayout.LayoutParams(MATCH, MATCH))
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
        dots.visibility = if (tab == 0 || tab == 3) View.VISIBLE else View.GONE
        bagsView.visibility = if (tab == 1) View.VISIBLE else View.GONE
        when (tab) {
            0 -> {
                dots.marks = null
                dots.total = 40
                if (Store.sessionOn(this)) {
                    val remMs = Store.until(this) - System.currentTimeMillis()
                    val remMin = ((remMs + 59_999L) / 60_000L).toInt()
                    pc.show(remMin, C_ORANGE)
                    val total = Store.sessionMinutes(this) * 60_000L
                    val frac = if (total > 0L) 1f - remMs.toFloat() / total.toFloat() else 0f
                    dots.filled = (frac * 40f).toInt().coerceIn(0, 40)
                    caption.text = "FOCUS ON  \u00B7  ${remMin / 60}h ${remMin % 60}m left"
                } else if (Store.inSchedule(this)) {
                    pc.show(0, C_ORANGE)
                    dots.filled = 0
                    caption.text = "ROUTINE ON"
                } else {
                    pc.show(durMin, C_ORANGE)
                    dots.filled = 2
                    caption.text = "READY"
                }
                dots.invalidate()
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
    private fun durTile(min: Int, unit: String): TextView {
        val big = if (min < 60) "$min" else "${min / 60}"
        return tile("$big\n$unit", durMin == min) {
            if (locked()) {
                toast("Strict mode ON hai, abhi badal nahi sakte")
            } else {
                durMin = min
                render()
                updateLeft()
            }
        }
    }

    private fun customTile(): TextView {
        val preset = durMin == 15 || durMin == 45 || durMin == 60 || durMin == 180 || durMin == 360
        return tile("Custom\n" + fmt(durMin), !preset) {
            if (locked()) toast("Strict mode ON hai, abhi badal nahi sakte") else customDialog()
        }
    }

    private fun customDialog() {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.HORIZONTAL
        box.setPadding(dp(16), dp(8), dp(16), dp(0))
        val eh = EditText(this)
        eh.hint = "ghante"
        eh.inputType = InputType.TYPE_CLASS_NUMBER
        eh.setText((durMin / 60).toString())
        val em = EditText(this)
        em.hint = "minute"
        em.inputType = InputType.TYPE_CLASS_NUMBER
        em.setText((durMin % 60).toString())
        box.addView(eh, LinearLayout.LayoutParams(0, WRAP, 1f))
        box.addView(em, LinearLayout.LayoutParams(0, WRAP, 1f))
        AlertDialog.Builder(this)
            .setTitle("Custom time (max 24 ghante)")
            .setView(box)
            .setPositiveButton("OK") { _, _ ->
                val t = (eh.text.toString().toLongOrNull() ?: 0L) * 60L + (em.text.toString().toLongOrNull() ?: 0L)
                if (t in 1L..1440L) {
                    durMin = t.toInt()
                    render()
                    updateLeft()
                } else {
                    toast("1 minute se 24 ghante ke beech rakh")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleStrict() {
        if (Store.active(this)) { toast("Session chalte time nahi badal sakte"); return }
        Store.setStrict(this, !Store.strict(this))
        render()
    }

    private fun toggleMode() {
        if (Store.active(this)) { toast("Session chalte time nahi badal sakte"); return }
        Store.setAllowMode(this, !Store.allowMode(this))
        render()
        updateLeft()
    }

    private fun timerPage(): View {
        val col = vbox()
        if (!accEnabled()) {
            val b = tile("Accessibility OFF  \u00B7  tap karke ON kar", false) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            b.textSize = 14f
            b.setTextColor(Color.WHITE)
            b.background = rounded(C_ORANGE, 14)
            col.addView(b, lpV(dp(52)))
        }
        col.addView(hrow(durTile(15, "Minutes"), durTile(45, "Minutes")), lpV())
        col.addView(hrow(durTile(60, "Hour"), durTile(180, "Hours")), lpV())
        col.addView(hrow(durTile(360, "Hours"), customTile()), lpV())
        val strictTile = tile("STRICT\n" + if (Store.strict(this)) "ON" else "OFF", Store.strict(this)) { toggleStrict() }
        val modeTile = tile("MODE\n" + if (Store.allowMode(this)) "BAG (allow only)" else "BLOCK", Store.allowMode(this)) { toggleMode() }
        col.addView(hrow(strictTile, modeTile), lpV(dp(72)))

        if (!Store.active(this)) {
            col.addView(SlideView(this) { startFocus() }, lpV(dp(64)))
        } else if (Store.sessionOn(this) && !Store.strict(this)) {
            col.addView(tile("End session", false) {
                Store.stopSession(this)
                render()
                updateLeft()
            }, lpV(dp(64)))
        } else if (Store.strict(this)) {
            col.addView(tile("STRICT ON  \u00B7  timer khatam hone tak locked", false) {}, lpV(dp(64)))
        } else {
            col.addView(tile("Routine chal raha hai (Routines tab se band kar)", false) {}, lpV(dp(64)))
        }
        return col
    }

    private fun startFocus() {
        if (Store.selected(this).isEmpty()) {
            toast("Pehle Bags tab me apps add kar")
            return
        }
        if (!accEnabled()) {
            toast("Pehle Accessibility ON kar")
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
                .setMessage("${durMin / 60}h ${durMin % 60}m tak End nahi kar paega aur Settings bhi block rahegi. Pakka?")
                .setPositiveButton("Haan") { _, _ -> go() }
                .setNegativeButton("Nahi", null)
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
                toast("Strict mode ON hai, abhi badal nahi sakte")
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
            cell.background = rounded(C_TILE, 16)
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
            tile("Apps add / hatao", true) { showPicker() },
            tile("Bag khali kar", false) {
                if (locked()) {
                    toast("Strict mode ON hai, abhi badal nahi sakte")
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
            toast("Strict mode ON hai, abhi badal nahi sakte")
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
        bAll.text = "Dikh rahe sab select"
        bAll.setOnClickListener {
            shown.forEach { set.add(it.pkg) }
            ad.notifyDataSetChanged()
        }
        val bNone = Button(this)
        bNone.text = "Sab hatao"
        bNone.setOnClickListener {
            set.clear()
            ad.notifyDataSetChanged()
        }
        bar.addView(bAll, LinearLayout.LayoutParams(0, WRAP, 1f))
        bar.addView(bNone, LinearLayout.LayoutParams(0, WRAP, 1f))
        col.addView(bar)
        col.addView(lv, LinearLayout.LayoutParams(MATCH, dp(360)))

        AlertDialog.Builder(this)
            .setTitle("Bag ${bagSel + 1} ke apps")
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
            t.background = rounded(C_DARK, 16)
        }
        return t
    }

    private fun pickTime(start: Boolean) {
        if (locked()) {
            toast("Strict mode ON hai, abhi badal nahi sakte")
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
            t.background = rounded(if (on) C_ORANGE else C_TILE, 14)
            t.setOnClickListener {
                if (locked()) {
                    toast("Strict mode ON hai, abhi badal nahi sakte")
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
                toast("Strict mode ON hai, abhi badal nahi sakte")
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
        col.addView(tile("Minutes timer start karte waqt count hote hain", false) {}, lpV(dp(64)))
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
