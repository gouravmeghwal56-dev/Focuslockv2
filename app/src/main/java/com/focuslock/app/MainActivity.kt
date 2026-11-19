package com.focuslock.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
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

private val C_ORANGE = Color.parseColor("#FF6A0A")
private val C_GRAY = Color.parseColor("#B5AEAC")
private val C_TILE = Color.parseColor("#EBE6E6")
private val C_DARK = Color.parseColor("#403B39")
private val C_BG = Color.parseColor("#F6F3F3")
private val C_TXT = Color.parseColor("#2E2927")
private val C_CARD = Color.parseColor("#FCFAFA")
private val C_LINE = Color.parseColor("#C9C2C0")

// ---------- Custom views ----------

/** Line/filled icons drawn on a 24x24 grid, so no drawable files are needed. */
class IconView(ctx: Context, var kind: Int) : View(ctx) {
    var tint: Int = C_DARK
        set(v) {
            field = v
            invalidate()
        }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rf = RectF()

    companion object {
        const val TIMER = 0
        const val BAG = 1
        const val GRID = 2
        const val BARS = 3
        const val MENU = 4
        const val GEAR = 5
        const val LOCK = 6
        const val PLAY = 7
        const val STOP = 8
        const val RESET = 9
        const val UP = 10
        const val DOWN = 11
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val aw = (width - paddingLeft - paddingRight).toFloat()
        val ah = (height - paddingTop - paddingBottom).toFloat()
        val s = minOf(aw, ah)
        val u = s / 24f
        val ox = paddingLeft + (aw - s) / 2f
        val oy = paddingTop + (ah - s) / 2f
        p.color = tint
        p.strokeWidth = 2f * u
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.style = Paint.Style.STROKE

        fun x(v: Float) = ox + v * u
        fun y(v: Float) = oy + v * u
        fun rr(l: Float, t: Float, r: Float, b: Float, rad: Float) {
            rf.set(x(l), y(t), x(r), y(b))
            canvas.drawRoundRect(rf, rad * u, rad * u, p)
        }

        when (kind) {
            TIMER -> {
                canvas.drawCircle(x(12f), y(13.5f), 8f * u, p)
                canvas.drawLine(x(12f), y(13.5f), x(12f), y(9.5f), p)
                canvas.drawLine(x(9.5f), y(2.5f), x(14.5f), y(2.5f), p)
            }
            BAG -> {
                p.style = Paint.Style.FILL
                rr(4f, 9f, 8.5f, 20f, 1f)
                rr(9.5f, 4f, 14.5f, 20f, 1f)
                rr(15.5f, 9f, 20f, 20f, 1f)
            }
            GRID -> {
                p.style = Paint.Style.FILL
                rr(3f, 3f, 11f, 11f, 2f)
                rr(13f, 3f, 21f, 11f, 2f)
                rr(3f, 13f, 11f, 21f, 2f)
                rr(13f, 13f, 21f, 21f, 2f)
            }
            BARS -> {
                p.style = Paint.Style.FILL
                rr(4f, 13f, 8f, 20f, 1.2f)
                rr(10f, 8.5f, 14f, 20f, 1.2f)
                rr(16f, 4f, 20f, 20f, 1.2f)
            }
            MENU -> {
                p.strokeWidth = 1.9f * u
                rr(3.5f, 3.5f, 10f, 10f, 2.5f)
                rr(14f, 3.5f, 20.5f, 10f, 2.5f)
                rr(3.5f, 14f, 10f, 20.5f, 2.5f)
                rr(14f, 14f, 20.5f, 20.5f, 2.5f)
            }
            GEAR -> {
                canvas.drawCircle(x(12f), y(12f), 6.3f * u, p)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val c = Math.cos(a).toFloat()
                    val sn = Math.sin(a).toFloat()
                    canvas.drawLine(
                        x(12f + 8.3f * c), y(12f + 8.3f * sn),
                        x(12f + 10.5f * c), y(12f + 10.5f * sn), p
                    )
                }
                canvas.drawCircle(x(12f), y(12f), 2.4f * u, p)
            }
            LOCK -> {
                p.strokeWidth = 2.6f * u
                path.reset()
                path.moveTo(x(7.5f), y(11f))
                path.lineTo(x(7.5f), y(8f))
                rf.set(x(7.5f), y(2.5f), x(16.5f), y(13.5f))
                path.arcTo(rf, 180f, 180f)
                path.lineTo(x(16.5f), y(11f))
                canvas.drawPath(path, p)
                p.style = Paint.Style.FILL
                rr(4f, 10.5f, 20f, 22f, 3.5f)
                p.color = Color.WHITE
                canvas.drawCircle(x(12f), y(15.3f), 1.7f * u, p)
                rr(11.2f, 15.5f, 12.8f, 19f, 0.8f)
            }
            PLAY -> {
                p.style = Paint.Style.FILL_AND_STROKE
                p.strokeWidth = 1.8f * u
                path.reset()
                path.moveTo(x(9f), y(6f))
                path.lineTo(x(9f), y(18f))
                path.lineTo(x(18f), y(12f))
                path.close()
                canvas.drawPath(path, p)
            }
            STOP -> {
                p.style = Paint.Style.FILL
                rr(6.5f, 6.5f, 17.5f, 17.5f, 2.5f)
            }
            RESET -> {
                rf.set(x(4f), y(4f), x(20f), y(20f))
                canvas.drawArc(rf, -40f, 290f, false, p)
                p.style = Paint.Style.FILL
                path.reset()
                path.moveTo(x(8.6f), y(1.2f))
                path.lineTo(x(8.6f), y(7.8f))
                path.lineTo(x(13.6f), y(4.5f))
                path.close()
                canvas.drawPath(path, p)
            }
            UP -> {
                p.strokeWidth = 2.2f * u
                path.reset()
                path.moveTo(x(6f), y(15f))
                path.lineTo(x(12f), y(9f))
                path.lineTo(x(18f), y(15f))
                canvas.drawPath(path, p)
            }
            DOWN -> {
                p.strokeWidth = 2.2f * u
                path.reset()
                path.moveTo(x(6f), y(9f))
                path.lineTo(x(12f), y(15f))
                path.lineTo(x(18f), y(9f))
                canvas.drawPath(path, p)
            }
        }
    }
}

class PixelClockView(ctx: Context) : View(ctx) {
    private var text = "0000"
    private var color = C_ORANGE
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
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
        val dw = minOf(w / 4.9f, h / 1.6f)
        val dh = dw * 1.6f
        val t = dw * 0.30f
        val gap = dw * 0.10f
        val colonW = dw * 0.32f
        val total = dw * 4f + colonW + gap * 4f
        var x = (w - total) / 2f
        val y = (h - dh) / 2f
        for (i in 0 until 4) {
            if (i == 2) {
                val cs = t * 0.9f
                box(canvas, x + (colonW - cs) / 2f, y + dh * 0.28f, cs, cs, cs * 0.25f)
                box(canvas, x + (colonW - cs) / 2f, y + dh * 0.64f, cs, cs, cs * 0.25f)
                x += colonW + gap
            }
            drawDigit(canvas, text[i] - '0', x, y, dw, dh, t)
            x += dw + gap
        }
    }

    private fun box(c: Canvas, x: Float, y: Float, w: Float, h: Float, r: Float) {
        rect.set(x, y, x + w, y + h)
        c.drawRoundRect(rect, r, r, paint)
    }

    private fun drawDigit(c: Canvas, d: Int, x: Float, y: Float, dw: Float, dh: Float, t: Float) {
        val m = masks[d]
        val r = t * 0.28f
        if ('a' in m) box(c, x, y, dw, t, r)
        if ('b' in m) box(c, x + dw - t, y, t, dh / 2f + t / 2f, r)
        if ('c' in m) box(c, x + dw - t, y + dh / 2f - t / 2f, t, dh / 2f + t / 2f, r)
        if ('d' in m) box(c, x, y + dh - t, dw, t, r)
        if ('e' in m) box(c, x, y + dh / 2f - t / 2f, t, dh / 2f + t / 2f, r)
        if ('f' in m) box(c, x, y, t, dh / 2f + t / 2f, r)
        if ('g' in m) box(c, x, y + (dh - t) / 2f, dw, t, r)
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

/** Sunrise + mountains + trees. progress 0..1 moves the sun up. */
class SceneView(ctx: Context) : View(ctx) {
    var progress = 0.3f
        set(v) {
            field = v.coerceIn(0f, 1f)
            invalidate()
        }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val cFar = Color.parseColor("#DAD4D8")
    private val cMid = Color.parseColor("#B8B1B8")
    private val cNear = Color.parseColor("#8E8890")
    private val cTree = Color.parseColor("#6E6870")
    private val cSunTop = Color.parseColor("#FFB066")

    private fun ridge(c: Canvas, color: Int, w: Float, h: Float, pts: FloatArray) {
        path.reset()
        path.moveTo(0f, h)
        var i = 0
        while (i < pts.size) {
            path.lineTo(pts[i] * w, pts[i + 1] * h)
            i += 2
        }
        path.lineTo(w, h)
        path.close()
        p.color = color
        c.drawPath(path, p)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        p.style = Paint.Style.FILL

        val r = h * 0.30f
        val sx = w * 0.72f
        val sy = h * (1.05f - 0.60f * progress)
        p.shader = LinearGradient(sx, sy - r, sx, sy + r, cSunTop, C_ORANGE, Shader.TileMode.CLAMP)
        canvas.drawCircle(sx, sy, r, p)
        p.shader = null

        ridge(canvas, cFar, w, h, floatArrayOf(0f, .62f, .12f, .45f, .25f, .60f, .38f, .40f, .55f, .62f, .70f, .50f, .85f, .60f, 1f, .48f))
        ridge(canvas, cMid, w, h, floatArrayOf(0f, .78f, .15f, .60f, .30f, .75f, .50f, .55f, .68f, .78f, .85f, .62f, 1f, .74f))
        ridge(canvas, cNear, w, h, floatArrayOf(0f, .88f, .20f, .78f, .40f, .90f, .60f, .80f, .80f, .90f, 1f, .82f))

        p.color = cTree
        for (i in 0 until 16) {
            val tx = w * (i + 0.5f) / 16f
            val th = h * (0.14f + 0.05f * ((i * 7) % 4))
            val tw = th * 0.55f
            val by = h * 0.97f
            path.reset()
            path.moveTo(tx - tw / 2f, by)
            path.lineTo(tx, by - th)
            path.lineTo(tx + tw / 2f, by)
            path.close()
            canvas.drawPath(path, p)
        }
    }
}

class RulerView(ctx: Context, private val onChange: (Int) -> Unit) : View(ctx) {
    var onBlocked: (() -> Boolean)? = null
    private val dens = ctx.resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var value = 180f
    private var reported = 180
    private var lastY = 0f
    private var ignore = false

    fun setMinutes(m: Int) {
        value = m.toFloat().coerceAtLeast(5f)
        reported = m
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val sp = h / 22f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = dens * 1.5f
        paint.color = C_LINE
        rect.set(dens, dens, w - dens, h - dens)
        canvas.drawRoundRect(rect, w * 0.3f, w * 0.3f, paint)

        val k = value / 5f
        val cy = h / 2f
        paint.strokeWidth = dens * 1.6f
        val first = Math.floor((k - 12f).toDouble()).toInt()
        val last = Math.ceil((k + 12f).toDouble()).toInt()
        for (i in first..last) {
            if (i < 0 || i > 288) continue
            val y = cy - (i - k) * sp
            if (y < h * 0.07f || y > h * 0.93f) continue
            val half = w * (if (i % 12 == 0) 0.26f else 0.17f)
            canvas.drawLine(w / 2f - half, y, w / 2f + half, y, paint)
        }
        paint.color = C_ORANGE
        paint.strokeWidth = dens * 4f
        canvas.drawLine(w * 0.27f, cy, w * 0.73f, cy, paint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                lastY = e.y
                ignore = onBlocked?.invoke() == true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!ignore) {
                    val dy = e.y - lastY
                    lastY = e.y
                    value = (value - dy / (height / 22f) * 5f).coerceIn(5f, 1440f)
                    invalidate()
                    val snapped = (Math.round(value / 5f) * 5).coerceIn(5, 1440)
                    if (snapped != reported) {
                        reported = snapped
                        onChange(snapped)
                    }
                }
            }
        }
        return true
    }
}

class SlideView(ctx: Context, private val onDone: () -> Unit) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()
    private var knobX = 0f
    private var dragging = false
    private var back: ValueAnimator? = null

    private fun snapBack() {
        back?.cancel()
        val a = ValueAnimator.ofFloat(knobX, 0f)
        a.duration = 220
        a.addUpdateListener {
            knobX = it.animatedValue as Float
            invalidate()
        }
        a.start()
        back = a
    }

    override fun onDetachedFromWindow() {
        back?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val kw = h * 1.25f
        val r = h / 2f
        val maxX = (w - kw).coerceAtLeast(1f)

        paint.style = Paint.Style.FILL
        paint.color = C_TILE
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, r, r, paint)

        paint.color = C_TXT
        paint.alpha = (255f * (1f - knobX / maxX)).toInt().coerceIn(30, 255)
        paint.textSize = h * 0.30f
        paint.textAlign = Paint.Align.CENTER
        val base = h / 2f - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText("Slide to Start", (w + kw) / 2f, base, paint)
        paint.alpha = 255

        paint.color = C_DARK
        rect.set(knobX, 0f, knobX + kw, h)
        canvas.drawRoundRect(rect, r, r, paint)

        val cx = knobX + kw / 2f
        val cy = h / 2f
        val s = h * 0.17f
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = h * 0.07f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        path.reset()
        path.moveTo(cx - s * 0.5f, cy - s)
        path.lineTo(cx + s * 0.5f, cy)
        path.lineTo(cx - s * 0.5f, cy + s)
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val h = height.toFloat()
        val kw = h * 1.25f
        val maxX = width.toFloat() - kw
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                back?.cancel()
                dragging = e.x <= knobX + kw * 1.3f
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging) {
                    knobX = (e.x - kw / 2f).coerceIn(0f, maxX)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                val done = dragging && knobX >= maxX * 0.9f
                dragging = false
                snapBack()
                if (done) onDone()
            }
            MotionEvent.ACTION_CANCEL -> {
                dragging = false
                snapBack()
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

     
