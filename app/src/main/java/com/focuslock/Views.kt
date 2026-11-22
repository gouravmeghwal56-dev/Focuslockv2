package com.focuslock.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import java.util.Random
import android.view.View

var C_ORANGE = Color.parseColor("#FF6A0A")
var C_GRAY = Color.parseColor("#B5AEAC")
var C_TILE = Color.parseColor("#EBE6E6")
var C_DARK = Color.parseColor("#403B39")
var C_BG = Color.parseColor("#F6F3F3")
var C_TXT = Color.parseColor("#2E2927")
var C_CARD = Color.parseColor("#FCFAFA")
var C_LINE = Color.parseColor("#C9C2C0")
// theme extras: filled controls (play, selected tile), soft accent shades
var C_PRIMARY = Color.parseColor("#403B39")
var C_SOFT = Color.parseColor("#F2B89C")
var C_L1: Int = Color.parseColor("#F7C4A5")
var C_L2: Int = Color.parseColor("#FA9A5C")
// more theme colours: clock gradient, scene (hills/trees/sun), selected-tile gradient top, tile hills (0 = none)
var C_CLK_A: Int = Color.parseColor("#4D4846")
var C_CLK_Z: Int = Color.parseColor("#F45516")
var C_S1: Int = Color.parseColor("#DAD4D8")
var C_S2: Int = Color.parseColor("#B8B1B8")
var C_S3: Int = Color.parseColor("#8E8890")
var C_S4: Int = Color.parseColor("#6E6870")
var C_SUN: Int = Color.parseColor("#FFB066")
var C_PRIMARY_TOP: Int = Color.parseColor("#403B39")
var C_HILL: Int = 0
var C_THEME_ID: String = "default"

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
        const val DETOX = 12
        const val PAUSE = 13
        const val INFINITY = 14
        const val INFO = 15
        const val PLUS = 16
        const val CHEVRON = 17
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
            INFINITY -> {
                p.strokeWidth = 2.6f * u
                path.reset()
                path.moveTo(x(12f), y(12f))
                path.cubicTo(x(9f), y(6.5f), x(3f), y(6.5f), x(3f), y(12f))
                path.cubicTo(x(3f), y(17.5f), x(9f), y(17.5f), x(12f), y(12f))
                path.cubicTo(x(15f), y(6.5f), x(21f), y(6.5f), x(21f), y(12f))
                path.cubicTo(x(21f), y(17.5f), x(15f), y(17.5f), x(12f), y(12f))
                canvas.drawPath(path, p)
            }
            INFO -> {
                canvas.drawCircle(x(12f), y(12f), 9.5f * u, p)
                canvas.drawLine(x(12f), y(11f), x(12f), y(17f), p)
                p.style = Paint.Style.FILL
                canvas.drawCircle(x(12f), y(7.6f), 1.3f * u, p)
            }
            PLUS -> {
                p.strokeWidth = 2.8f * u
                canvas.drawLine(x(12f), y(5f), x(12f), y(19f), p)
                canvas.drawLine(x(5f), y(12f), x(19f), y(12f), p)
            }
            CHEVRON -> {
                p.strokeWidth = 2.4f * u
                path.reset()
                path.moveTo(x(9f), y(5f))
                path.lineTo(x(16f), y(12f))
                path.lineTo(x(9f), y(19f))
                canvas.drawPath(path, p)
            }
            PAUSE -> {
                p.style = Paint.Style.FILL
                rr(6.5f, 5f, 10.5f, 19f, 1.2f)
                rr(13.5f, 5f, 17.5f, 19f, 1.2f)
            }
            DETOX -> {
                canvas.drawCircle(x(12f), y(12f), 8.5f * u, p)
                canvas.drawLine(x(6f), y(18f), x(18f), y(6f), p)
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


/**
 * Base view: content is drawn into an offscreen bitmap. assemble() then rebuilds it on screen
 * from scattered pixel blocks (blockit style), so bags AND digits "build" themselves.
 */
abstract class AssembleView(ctx: Context) : View(ctx) {
    private var buf: Bitmap? = null
    private var bufCanvas: Canvas? = null
    private var dirty = true
    private var prog = 1f
    private var anim: ValueAnimator? = null
    private val cell = (ctx.resources.displayMetrics.density * 6f).toInt().coerceAtLeast(4)
    private var cols = 0
    private var rows = 0
    private var thr = FloatArray(0)
    private var offX = FloatArray(0)
    private var offY = FloatArray(0)
    private val src = Rect()
    private val dst = RectF()
    private val bp = Paint()

    protected abstract fun drawContent(c: Canvas)

    override fun invalidate() {
        dirty = true
        super.invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        buf?.recycle()
        buf = null
        bufCanvas = null
        dirty = true
        cols = (w + cell - 1) / cell
        rows = (h + cell - 1) / cell
        val n = cols * rows
        val r = Random(7)
        thr = FloatArray(n) { r.nextFloat() * 0.62f }
        offX = FloatArray(n) { (r.nextFloat() - 0.5f) * 2f }
        offY = FloatArray(n) { (r.nextFloat() - 0.5f) * 2f }
    }

    fun assemble(duration: Long = 750L) {
        anim?.cancel()
        prog = 0f
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = duration
        a.addUpdateListener {
            prog = it.animatedValue as Float
            super.invalidate()
        }
        a.start()
        anim = a
        super.invalidate()
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        var b = buf
        if (b == null) {
            b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            buf = b
            bufCanvas = Canvas(b)
            dirty = true
        }
        if (dirty) {
            b.eraseColor(Color.TRANSPARENT)
            drawContent(bufCanvas!!)
            dirty = false
        }
        if (prog >= 1f) {
            canvas.drawBitmap(b, 0f, 0f, null)
            return
        }
        val k = 0.38f
        for (cy in 0 until rows) {
            for (cx in 0 until cols) {
                val i = cy * cols + cx
                val local = ((prog - thr[i]) / k).coerceIn(0f, 1f)
                if (local <= 0f) continue
                val l = cx * cell
                val t = cy * cell
                val r = minOf(l + cell, width)
                val bt = minOf(t + cell, height)
                src.set(l, t, r, bt)
                val half = (r - l) / 2f * local
                val halfH = (bt - t) / 2f * local
                val sh = (1f - local) * cell * 2.2f
                val mx = (l + r) / 2f + offX[i] * sh
                val my = (t + bt) / 2f + offY[i] * sh
                dst.set(mx - half, my - halfH, mx + half, my + halfH)
                canvas.drawBitmap(b, src, dst, bp)
            }
        }
    }
}

class PixelClockView(ctx: Context) : AssembleView(ctx) {
    private var text = "0000"
    private var color = C_ORANGE
    private var pair = false
    private var pairText = "00:00 00:00"
    private var slots: BooleanArray? = null
    private var slotColor = C_ORANGE
    private val font = arrayOf(
        "11111,10001,10001,10001,10001,10001,11111",
        "00100,01100,00100,00100,00100,00100,01110",
        "11111,00001,00001,11111,10000,10000,11111",
        "11111,00001,00001,01111,00001,00001,11111",
        "10001,10001,10001,11111,00001,00001,00001",
        "11111,10000,10000,11111,00001,00001,11111",
        "11111,10000,10000,11111,10001,10001,11111",
        "11111,00001,00001,00010,00100,00100,00100",
        "11111,10001,10001,11111,10001,10001,11111",
        "11111,10001,10001,11111,00001,00001,11111"
    ).map { it.split(",") }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val masks = arrayOf("abcdef", "bc", "abdeg", "abcdg", "bcfg", "acdfg", "acdefg", "abc", "abcdefg", "abcdfg")

    /** Vertical nudge (px) for the digits; used to line them up with a neighbouring tile. */
    var dyShift = 0f
        set(v) {
            if (field != v) {
                field = v
                invalidate()
            }
        }

    /** Dim mode: faint block seams cut through the digits (blockit look). */
    var seams = false
        set(v) {
            if (field != v) {
                field = v
                invalidate()
            }
        }
    private val seamPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        strokeWidth = 2f
    }

    private fun seamLines(c: Canvas, x: Float, y: Float, dw: Float, dh: Float) {
        for (k in 1..2) c.drawLine(x + k * dw / 3f, y, x + k * dw / 3f, y + dh, seamPaint)
        for (k in 1..4) c.drawLine(x, y + k * dh / 5f, x + dw, y + k * dh / 5f, seamPaint)
    }

    fun show(totalMin: Int, c: Int) {
        val h = (totalMin / 60).coerceIn(0, 99)
        val m = totalMin % 60
        text = String.format("%02d%02d", h, m)
        color = c
        pair = false
        invalidate()
    }

    /** Routine style: "23:55  05:30" in pixel font + 48 half-hour timeline squares under it. */
    fun showPair(startMin: Int, endMin: Int, c: Int, slotC: Int) {
        pairText = String.format("%02d:%02d %02d:%02d", startMin / 60, startMin % 60, endMin / 60, endMin % 60)
        color = c
        slotColor = slotC
        fun inR(m: Int) = if (startMin < endMin) m in startMin until endMin else (m >= startMin || m < endMin)
        slots = BooleanArray(48) { i ->
            val a = i * 30
            startMin != endMin && (inR(a) || inR(a + 29) || startMin in a..(a + 29))
        }
        pair = true
        invalidate()
    }

    private fun drawPair(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        // columns: digit=5, gap=1, colon=1, group gap=3
        val cols = 54
        val px = (w * 0.94f) / cols
        val x0 = (w - px * cols) / 2f
        val dh = px * 7f
        val barH = px * 0.9f * 1.0f
        val totalH = dh + px * 3.5f + barH
        val y0 = (h - totalH) / 2f
        paint.color = color
        var cx = 0
        for (ch in pairText) {
            when {
                ch == ' ' -> cx += 2
                ch == ':' -> {
                    canvas.drawRect(x0 + cx * px, y0 + px * 2f, x0 + (cx + 1) * px, y0 + px * 3f, paint)
                    canvas.drawRect(x0 + cx * px, y0 + px * 4f, x0 + (cx + 1) * px, y0 + px * 5f, paint)
                    cx += 2
                }
                else -> {
                    val g = font[ch - '0']
                    for (r in 0 until 7) for (q in 0 until 5) {
                        if (g[r][q] == '1') {
                            canvas.drawRect(x0 + (cx + q) * px, y0 + r * px, x0 + (cx + q + 1) * px, y0 + (r + 1) * px, paint)
                        }
                    }
                    cx += 6
                }
            }
        }
        val sl = slots ?: return
        val pitch = (px * cols) / 48f
        val sq = pitch * 0.78f
        val by = y0 + dh + px * 3.5f
        for (i in 0 until 48) {
            paint.color = if (sl[i]) slotColor else C_TILE
            val sx = x0 + i * pitch
            canvas.drawRect(sx, by, sx + sq, by + sq, paint)
        }
    }

    override fun drawContent(canvas: Canvas) {
        if (pair) {
            drawPair(canvas)
            return
        }
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
        val y = (h - dh) / 2f + dyShift
        for (i in 0 until 4) {
            if (i == 2) {
                val cs = t * 0.9f
                box(canvas, x + (colonW - cs) / 2f, y + dh * 0.28f, cs, cs, cs * 0.25f)
                box(canvas, x + (colonW - cs) / 2f, y + dh * 0.64f, cs, cs, cs * 0.25f)
                x += colonW + gap
            }
            drawDigit(canvas, text[i] - '0', x, y, dw, dh, t)
            if (seams) seamLines(canvas, x, y, dw, dh)
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

    /** Optional intensity per square: 0 = none, 1..3 = light..strong orange. */
    var levels: IntArray? = null
    /** Squares from this index on are in the future (drawn very light). */
    var futureFrom = Int.MAX_VALUE
    /** Selected square (drawn dark), -1 = none. */
    var selected = -1
    var onPick: ((Int) -> Unit)? = null
    private val paint = Paint()

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val cb = onPick ?: return super.onTouchEvent(e)
        if (e.action == MotionEvent.ACTION_DOWN) return true
        if (e.action == MotionEvent.ACTION_UP && total > 0 && width > 0) {
            val i = (e.x / (width.toFloat() / total)).toInt().coerceIn(0, total - 1)
            if (i < futureFrom) cb(i)
            return true
        }
        return super.onTouchEvent(e)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cell = w / total
        val s = minOf(cell * 0.62f, h)
        val y = (h - s) / 2f
        val mk = marks
        val lv = levels
        for (i in 0 until total) {
            paint.color = when {
                i == selected -> C_PRIMARY
                lv != null && i >= futureFrom -> Color.parseColor("#F1EDED")
                lv != null -> when (lv.getOrElse(i) { 0 }) {
                    0 -> C_TILE
                    1 -> C_L1
                    2 -> C_L2
                    else -> C_ORANGE
                }
                (if (mk != null) (i < mk.size && mk[i]) else i < filled) -> C_ORANGE
                else -> C_TILE
            }
            val x = i * cell + (cell - s) / 2f
            canvas.drawRect(x, y, x + s, y + s, paint)
        }
    }
}

class BagsView(ctx: Context) : AssembleView(ctx) {
    var active = 0
        set(v) {
            field = v
            invalidate()
        }
    private val paint = Paint()

    override fun drawContent(canvas: Canvas) {
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
    private val cFar: Int get() = C_S1
    private val cMid: Int get() = C_S2
    private val cNear: Int get() = C_S3
    private val cTree: Int get() = C_S4
    private val cSunTop: Int get() = C_SUN

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
        if (Art.get(context, "bg") != null) return

        val art = Art.get(context, "scene")
        if (art != null) {
            val sc = w / art.width.toFloat()
            val dh = art.height * sc
            val src = Rect(0, if (dh > h) ((dh - h) / sc).toInt() else 0, art.width, art.height)
            val dst = RectF(0f, if (dh > h) 0f else h - dh, w, h)
            p.isFilterBitmap = true
            canvas.drawBitmap(art, src, dst, p)
            return
        }

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
    private var limitHit = false
    private var shakeAnim: ValueAnimator? = null

    fun setMinutes(m: Int) {
        value = m.toFloat().coerceAtLeast(1f)
        reported = m
        invalidate()
    }

    private var pinDx = 0f

    /** Only the orange pin wobbles left-right, plus a real vibration, when the user hits 1 min / 24 h. */
    fun shake() {
        shakeAnim?.cancel()
        val amp = dens * 7f
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 320L
        a.addUpdateListener {
            val t = it.animatedValue as Float
            pinDx = (Math.sin(t * Math.PI * 8.0) * (1f - t) * amp).toFloat()
            invalidate()
        }
        a.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) { pinDx = 0f; invalidate() }
            override fun onAnimationCancel(animation: android.animation.Animator) { pinDx = 0f; invalidate() }
        })
        a.start()
        shakeAnim = a
        try {
            val vib = if (android.os.Build.VERSION.SDK_INT >= 31) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            }
            if (vib.hasVibrator()) {
                vib.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 25, 35, 25, 35, 25), -1))
            }
        } catch (e: Exception) {
            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val sp = h / 110f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = dens * 1.5f
        paint.color = C_LINE
        rect.set(dens, dens, w - dens, h - dens)
        canvas.drawRoundRect(rect, w * 0.3f, w * 0.3f, paint)

        val k = value
        val cy = h / 2f
        val first = Math.floor((k - 60f).toDouble()).toInt()
        val last = Math.ceil((k + 60f).toDouble()).toInt()
        for (i in first..last) {
            if (i < 1 || i > 1440) continue
            if (i % 5 != 0) continue // sirf har 5 min pe line, beech ki lines hata di
            val y = cy + (i - k) * sp
            if (y < h * 0.07f || y > h * 0.93f) continue
            paint.strokeWidth = dens * (if (i % 15 == 0) 1.6f else 1.0f)
            val half = w * (if (i % 60 == 0) 0.30f else if (i % 15 == 0) 0.22f else 0.12f)
            canvas.drawLine(w / 2f - half, y, w / 2f + half, y, paint)
        }
        paint.color = C_ORANGE
        paint.strokeWidth = dens * 4f
        canvas.drawLine(w * 0.27f + pinDx, cy, w * 0.73f + pinDx, cy, paint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                lastY = e.y
                limitHit = false
                ignore = onBlocked?.invoke() == true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!ignore) {
                    val dy = e.y - lastY
                    lastY = e.y
                    val raw = value - dy / (height / 110f)
                    if (raw < 1f || raw > 1440f) {
                        if (!limitHit) { limitHit = true; shake() }
                    } else {
                        limitHit = false
                    }
                    value = raw.coerceIn(1f, 1440f)
                    invalidate()
                    val snapped = Math.round(value).coerceIn(1, 1440)
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
        paint.color = if (Art.get(context, "bg") != null) (C_TILE and 0x00FFFFFF) or (0xC8 shl 24) else C_TILE
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, r, r, paint)

        paint.color = C_TXT
        paint.alpha = (255f * (1f - knobX / maxX)).toInt().coerceIn(30, 255)
        paint.textSize = h * 0.30f
        paint.textAlign = Paint.Align.CENTER
        val base = h / 2f - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText("Slide to Start", (w + kw) / 2f, base, paint)
        paint.alpha = 255

        paint.color = C_PRIMARY
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

/** Vertical on/off slider for a weekday (thumb on top = ON, bottom = OFF). */
class DayToggleView(ctx: Context, on: Boolean, private val onToggle: (Boolean) -> Boolean) : View(ctx) {
    private var pos = if (on) 0f else 1f
    private var isOn = on
    private var anim: ValueAnimator? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
    private var downY = 0f
    private var moved = false

    private fun go(to: Float) {
        anim?.cancel()
        val a = ValueAnimator.ofFloat(pos, to)
        a.duration = 260
        a.interpolator = DecelerateInterpolator()
        a.addUpdateListener {
            pos = it.animatedValue as Float
            invalidate()
        }
        a.start()
        anim = a
    }

    private fun setOn(v: Boolean) {
        if (v == isOn) {
            go(if (v) 0f else 1f)
            return
        }
        if (onToggle(v)) {
            isOn = v
            go(if (v) 0f else 1f)
        } else {
            go(if (isOn) 0f else 1f)
        }
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val rad = w * 0.28f
        paint.style = Paint.Style.FILL
        paint.color = C_TILE
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, rad, rad, paint)
        val pad = w * 0.08f
        val th = (h - pad * 2f) * 0.46f
        val ty = pad + (h - pad * 2f - th) * pos
        paint.color = blend(C_ORANGE, Color.parseColor("#8E8886"), pos)
        rect.set(pad, ty, w - pad, ty + th)
        canvas.drawRoundRect(rect, rad * 0.8f, rad * 0.8f, paint)
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt()
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt()
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt()
        return Color.rgb(r, g, bl)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                downY = e.y
                moved = false
                anim?.cancel()
            }
            MotionEvent.ACTION_MOVE -> {
                if (Math.abs(e.y - downY) > slop) moved = true
                if (moved) {
                    pos = (e.y / height).coerceIn(0f, 1f)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (moved) setOn(pos < 0.5f) else setOn(!isOn)
            }
            MotionEvent.ACTION_CANCEL -> go(if (isOn) 0f else 1f)
        }
        return true
    }
}

/** Horizontal ruler for a time of day (0..1439, 5 min steps, wraps around). */
class HRulerView(ctx: Context, startValue: Int, private val onChange: (Int) -> Unit) : View(ctx) {
    var onBlocked: (() -> Boolean)? = null
    private val dens = ctx.resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var value = startValue.toFloat()
    private var reported = startValue
    private var lastX = 0f
    private var ignore = false
    private var vt: VelocityTracker? = null
    private var fling: ValueAnimator? = null

    private fun pxPerMin() = width / 90f

    fun setMinutes(m: Int) {
        fling?.cancel()
        value = m.toFloat()
        reported = m
        invalidate()
    }

    private fun report() {
        val snapped = (Math.round(value / 5f) * 5).mod(1440)
        if (snapped != reported) {
            reported = snapped
            onChange(snapped)
        }
    }

    override fun onDetachedFromWindow() {
        fling?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = dens * 1.5f
        paint.color = C_LINE
        rect.set(dens, dens, w - dens, h - dens)
        canvas.drawRoundRect(rect, h * 0.3f, h * 0.3f, paint)

        val sp = pxPerMin() * 5f
        val k = value / 5f
        val cx = w / 2f
        val n = (w / sp / 2f).toInt() + 2
        val first = Math.floor((k - n).toDouble()).toInt()
        val last = Math.ceil((k + n).toDouble()).toInt()
        paint.strokeWidth = dens * 1.4f
        for (i in first..last) {
            val x = cx + (i - k) * sp
            if (x < h * 0.25f || x > w - h * 0.25f) continue
            val hr = (i.mod(288))
            val half = h * (if (hr % 12 == 0) 0.26f else 0.17f)
            canvas.drawLine(x, h / 2f - half, x, h / 2f + half, paint)
        }
        paint.color = C_ORANGE
        paint.strokeWidth = dens * 3f
        canvas.drawLine(cx, h * 0.04f, cx, h * 0.26f, paint)
        canvas.drawLine(cx, h * 0.74f, cx, h * 0.96f, paint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                fling?.cancel()
                lastX = e.x
                ignore = onBlocked?.invoke() == true
                vt?.recycle()
                vt = VelocityTracker.obtain()
                vt?.addMovement(e)
            }
            MotionEvent.ACTION_MOVE -> {
                vt?.addMovement(e)
                if (!ignore) {
                    val dx = e.x - lastX
                    lastX = e.x
                    value = (value - dx / pxPerMin()).mod(1440f)
                    invalidate()
                    report()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val t = vt
                if (t != null && !ignore && e.action == MotionEvent.ACTION_UP) {
                    t.computeCurrentVelocity(1000)
                    val dMin = -t.xVelocity / pxPerMin() * 0.35f
                    val from = value
                    val a = ValueAnimator.ofFloat(0f, dMin)
                    a.duration = 600
                    a.interpolator = DecelerateInterpolator(2f)
                    a.addUpdateListener {
                        value = (from + (it.animatedValue as Float)).mod(1440f)
                        invalidate()
                        report()
                    }
                    a.addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            value = (Math.round(value / 5f) * 5f).mod(1440f)
                            invalidate()
                            report()
                        }
                    })
                    a.start()
                    fling = a
                }
                t?.recycle()
                vt = null
            }
        }
        return true
    }
}

/** Detox illustration: phone with a crossed-out clapperboard (pixel-assemble animation supported). */
class DetoxArtView(ctx: Context) : AssembleView(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val path = Path()

    override fun drawContent(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val ph = minOf(h * 0.82f, w * 0.95f)
        val pw = ph * 0.56f
        val cx = w / 2f
        val top = h * 0.12f
        val cy = top + ph / 2f

        // soft blob + floor shadow
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#EEE9E8")
        rect.set(cx - pw * 1.45f, top + ph * 0.34f, cx + pw * 1.05f, top + ph * 0.96f)
        canvas.drawOval(rect, paint)
        paint.color = Color.parseColor("#E4DEDD")
        rect.set(cx - pw * 0.85f, top + ph * 0.985f, cx + pw * 0.85f, top + ph * 1.045f)
        canvas.drawOval(rect, paint)

        // phone
        val sw = pw * 0.075f
        rect.set(cx - pw / 2f, top, cx + pw / 2f, top + ph)
        paint.color = Color.parseColor("#FCFAFA")
        canvas.drawRoundRect(rect, pw * 0.2f, pw * 0.2f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = sw
        paint.color = Color.parseColor("#171515")
        canvas.drawRoundRect(rect, pw * 0.2f, pw * 0.2f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, top + ph * 0.075f, pw * 0.032f, paint)

        // sparks (top right)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = pw * 0.06f
        paint.color = C_ORANGE
        val ox = cx + pw * 0.62f
        val oy = top + ph * 0.02f
        val angles = floatArrayOf(-78f, -48f, -18f)
        for (a in angles) {
            val r = Math.toRadians(a.toDouble())
            val c0 = Math.cos(r).toFloat()
            val s0 = Math.sin(r).toFloat()
            canvas.drawLine(ox + c0 * pw * 0.20f, oy + s0 * pw * 0.20f, ox + c0 * pw * 0.42f, oy + s0 * pw * 0.42f, paint)
        }

        // clapperboard
        val cw = pw * 0.66f
        val ch = cw * 0.78f
        val l = cx - cw / 2f
        val r = cx + cw / 2f
        val t = cy - ch / 2f
        val b = cy + ch / 2f
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#3A3837")
        rect.set(l, t + ch * 0.26f, r, b)
        canvas.drawRoundRect(rect, cw * 0.1f, cw * 0.1f, paint)
        rect.set(l, t, r, t + ch * 0.27f)
        canvas.drawRoundRect(rect, cw * 0.07f, cw * 0.07f, paint)
        paint.color = Color.parseColor("#F2F2F2")
        val sx = cw * 0.12f
        for (i in 0..2) {
            val x0 = l + cw * 0.10f + i * cw * 0.30f
            path.reset()
            path.moveTo(x0, t + ch * 0.22f)
            path.lineTo(x0 + sx, t + ch * 0.05f)
            path.lineTo(x0 + sx + cw * 0.14f, t + ch * 0.05f)
            path.lineTo(x0 + cw * 0.14f, t + ch * 0.22f)
            path.close()
            canvas.drawPath(path, paint)
        }
        // play triangle
        paint.color = Color.parseColor("#EDEDED")
        val ts = cw * 0.17f
        path.reset()
        path.moveTo(cx - ts * 0.6f, cy + ch * 0.08f - ts)
        path.lineTo(cx + ts, cy + ch * 0.08f)
        path.lineTo(cx - ts * 0.6f, cy + ch * 0.08f + ts)
        path.close()
        canvas.drawPath(path, paint)

        // slash
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        val x1 = l - cw * 0.04f
        val y1 = b + ch * 0.04f
        val x2 = r + cw * 0.02f
        val y2 = t - ch * 0.05f
        paint.strokeWidth = pw * 0.15f
        paint.color = Color.parseColor("#FCFAFA")
        canvas.drawLine(x1, y1, x2, y2, paint)
        paint.strokeWidth = pw * 0.075f
        paint.color = C_ORANGE
        canvas.drawLine(x1, y1, x2, y2, paint)
    }
}

/** Rotating quotes: auto-swipe every 5s, user can swipe too. Dots show the position. */
class QuoteView(ctx: Context) : View(ctx) {
    private val quotes = arrayOf(
        "Own Your Mind," to "Leave Scrolls Behind.",
        "Less Feed," to "More Deed.",
        "Don't Just Scroll," to "Take Control.",
        "Feed Your Mind," to "Not the Grind."
    )
    private val dens = ctx.resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var idx = 0
    private var t = 1f
    private var from = 0
    private var dir = 1
    private var anim: ValueAnimator? = null
    private var size = 40f
    private var downX = 0f
    private val auto = object : Runnable {
        override fun run() {
            if (isShown) go(1)
            postDelayed(this, 5000L)
        }
    }

    init {
        paint.typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
        paint.textAlign = Paint.Align.CENTER
    }

    private fun go(d: Int) {
        val n = quotes.size
        from = idx
        idx = (idx + d + n) % n
        dir = d
        anim?.cancel()
        t = 0f
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 450
        a.interpolator = DecelerateInterpolator(1.8f)
        a.addUpdateListener {
            t = it.animatedValue as Float
            invalidate()
        }
        a.start()
        anim = a
    }

    private fun restart() {
        removeCallbacks(auto)
        postDelayed(auto, 5000L)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postDelayed(auto, 5000L)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(auto)
        anim?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        var sz = minOf(w * 0.11f, h * 0.27f)
        paint.textSize = sz
        var widest = 0f
        for (q in quotes) widest = maxOf(widest, paint.measureText(q.first), paint.measureText(q.second))
        if (widest > w * 0.92f) sz *= (w * 0.92f) / widest
        size = sz
    }

    private fun drawQuote(c: Canvas, i: Int, dx: Float, alpha: Int) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.textSize = size
        val y1 = h * 0.34f
        paint.color = Color.argb(alpha, 17, 17, 17)
        c.drawText(quotes[i].first, w / 2f + dx, y1, paint)
        paint.color = Color.argb(alpha, Color.red(C_ORANGE), Color.green(C_ORANGE), Color.blue(C_ORANGE))
        c.drawText(quotes[i].second, w / 2f + dx, y1 + size * 1.15f, paint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (t < 1f) {
            drawQuote(canvas, from, -dir * t * w * 0.6f, ((1f - t) * 255).toInt())
            drawQuote(canvas, idx, dir * (1f - t) * w * 0.6f, (t * 255).toInt())
        } else {
            drawQuote(canvas, idx, 0f, 255)
        }
        val r = dens * 5f
        val gap = dens * 14f
        val total = (quotes.size - 1) * gap
        for (i in quotes.indices) {
            paint.color = if (i == idx) C_ORANGE else Color.parseColor("#DDD7D6")
            canvas.drawCircle(w / 2f - total / 2f + i * gap, h - dens * 14f, r, paint)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> downX = e.x
            MotionEvent.ACTION_UP -> {
                val dx = e.x - downX
                if (Math.abs(dx) > dens * 40f) {
                    go(if (dx < 0) 1 else -1)
                    restart()
                }
            }
        }
        return true
    }
}

/** ScrollView whose children are exactly half its height, so 2 cards show and more can be swiped. */
class HalfScrollView(ctx: Context) : android.widget.ScrollView(ctx) {
    private val m = (12 * ctx.resources.displayMetrics.density).toInt()

    private var pendingY = -1

    /** Cards ki height abhi ke size ke hisaab se set karo (list dobara bharne ke baad). */
    fun fit() {
        if (height <= 0) return
        val c = getChildAt(0) as? android.widget.LinearLayout ?: return
        for (i in 0 until c.childCount) {
            val v = c.getChildAt(i)
            val lp = v.layoutParams as android.widget.LinearLayout.LayoutParams
            lp.height = (height / 2 - m).coerceAtLeast(40)
            v.layoutParams = lp
        }
    }

    /** render() page dobara banata hai; naye view me purani scroll position wapas laao. */
    fun restoreScroll(y: Int) { pendingY = y }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (pendingY >= 0 && height > 0) {
            val first = (getChildAt(0) as? android.widget.LinearLayout)?.getChildAt(0)
            val want = (height / 2 - m).coerceAtLeast(40)
            // cards ki height set hone ke baad hi scroll karo, warna clamp ho jata hai
            if (first != null && first.height == want) {
                val y = pendingY
                pendingY = -1
                scrollTo(0, y)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        val c = getChildAt(0) as? android.widget.LinearLayout ?: return
        for (i in 0 until c.childCount) {
            val v = c.getChildAt(i)
            val lp = v.layoutParams as android.widget.LinearLayout.LayoutParams
            lp.height = (h / 2 - m).coerceAtLeast(40)
            v.layoutParams = lp
        }
    }
}


/** Tile background: optional vertical gradient (selected tiles) and soft hills at the bottom (themes). */
class TileBg(private val base: Int, private val top: Int, private val hill: Int, private val rPx: Float, private val art: Bitmap? = null, private val ridge: Boolean = false, private val veil: Int = 0, private val ring: Int = 0) :
    android.graphics.drawable.Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val clip = Path()
    private val rf = RectF()

    override fun draw(c: Canvas) {
        val b = bounds
        rf.set(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat())
        val l = rf.left
        val t = rf.top
        val w = rf.width()
        val h = rf.height()
        c.save()
        clip.reset()
        clip.addRoundRect(rf, rPx, rPx, Path.Direction.CW)
        c.clipPath(clip)
        p.style = Paint.Style.FILL
        if (art != null) {
            val sc = maxOf(w / art.width, h / art.height)
            val sw = art.width * sc
            val sh = art.height * sc
            val dst = RectF(l + (w - sw) / 2f, t + (h - sh) / 2f, l + (w + sw) / 2f, t + (h + sh) / 2f)
            p.isFilterBitmap = true
            c.drawBitmap(art, null, dst, p)
            p.color = if (veil != 0) veil else 0x2A3F281B
            c.drawRect(rf, p)
            c.restore()
            drawRing(c)
            return
        }
        p.shader = if (top != base) LinearGradient(0f, t, 0f, t + h, top, base, Shader.TileMode.CLAMP) else null
        p.color = base
        c.drawRect(rf, p)
        p.shader = null
        if (ridge && hill != 0) {
            val far = floatArrayOf(0f, .80f, .14f, .70f, .28f, .79f, .46f, .64f, .64f, .78f, .82f, .68f, 1f, .77f)
            val near = floatArrayOf(0f, .90f, .20f, .82f, .40f, .91f, .62f, .80f, .82f, .90f, 1f, .84f)
            val layers = arrayOf(far to 0x26, near to 0x34)
            for ((pts, a) in layers) {
                p.color = (hill and 0x00FFFFFF) or (a shl 24)
                path.reset()
                path.moveTo(l, t + h)
                var i = 0
                while (i < pts.size) {
                    path.lineTo(l + pts[i] * w, t + pts[i + 1] * h)
                    i += 2
                }
                path.lineTo(l + w, t + h)
                path.close()
                c.drawPath(path, p)
            }
        } else if (hill != 0) {
            p.color = hill
            path.reset()
            path.moveTo(l, t + h * 0.80f)
            path.quadTo(l + w * 0.25f, t + h * 0.66f, l + w * 0.50f, t + h * 0.80f)
            path.quadTo(l + w * 0.75f, t + h * 0.92f, l + w, t + h * 0.74f)
            path.lineTo(l + w, t + h)
            path.lineTo(l, t + h)
            path.close()
            c.drawPath(path, p)
            p.color = (hill and 0x00FFFFFF) or (0x99 shl 24)
            path.reset()
            path.moveTo(l, t + h * 0.90f)
            path.quadTo(l + w * 0.30f, t + h * 0.82f, l + w * 0.55f, t + h * 0.92f)
            path.quadTo(l + w * 0.80f, t + h * 1.00f, l + w, t + h * 0.86f)
            path.lineTo(l + w, t + h)
            path.lineTo(l, t + h)
            path.close()
            c.drawPath(path, p)
        }
        c.restore()
        drawRing(c)
    }

    private fun drawRing(c: Canvas) {
        if (ring == 0) return
        val sw = rPx * 0.14f
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = sw
        p.color = ring
        val r2 = RectF(rf.left + sw / 2f, rf.top + sw / 2f, rf.right - sw / 2f, rf.bottom - sw / 2f)
        c.drawRoundRect(r2, rPx - sw / 2f, rPx - sw / 2f, p)
        p.style = Paint.Style.FILL
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(cf: android.graphics.ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}


/** Left card background: rounded fill + optional corner art (branch) from the theme's PNG. */
class CardBg(private val fill: Int, private val rPx: Float, private val art: Bitmap?) :
    android.graphics.drawable.Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rf = RectF()
    private val dst = RectF()

    override fun draw(c: Canvas) {
        val b = bounds
        rf.set(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat())
        p.style = Paint.Style.FILL
        p.color = fill
        c.drawRoundRect(rf, rPx, rPx, p)
        if (art != null) {
            val w = rf.width() * 0.42f
            val h = w * art.height / art.width.toFloat()
            dst.set(rf.left, rf.top, rf.left + w, rf.top + h)
            p.isFilterBitmap = true
            c.drawBitmap(art, null, dst, p)
        }
    }

    override fun getOutline(outline: android.graphics.Outline) {
        outline.setRoundRect(bounds, rPx)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(cf: android.graphics.ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}

/** Full-screen background: base colour + theme art scaled to cover, anchored bottom-left. */
class ArtBg(private val base: Int, private val art: Bitmap) : android.graphics.drawable.Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dst = RectF()

    override fun draw(c: Canvas) {
        val b = bounds
        val w = b.width().toFloat()
        val h = b.height().toFloat()
        p.color = base
        p.style = Paint.Style.FILL
        c.drawRect(b, p)
        val sc = maxOf(w / art.width, h / art.height)
        val sw = art.width * sc
        val sh = art.height * sc
        dst.set(b.left.toFloat(), b.bottom - sh, b.left + sw, b.bottom.toFloat())
        p.isFilterBitmap = true
        c.drawBitmap(art, null, dst, p)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(cf: android.graphics.ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = android.graphics.PixelFormat.OPAQUE
}

/** Optional per-theme art, dropped in res/drawable-nodpi as theme_<id>_scene / _branch / _bg (full screen) / _card (selected Custom tile). */
object Art {
    private val cache = HashMap<String, Bitmap?>()

    fun get(ctx: Context, kind: String): Bitmap? {
        val id = C_THEME_ID
        if (id == "default") return null
        val key = id + "_" + kind
        if (cache.containsKey(key)) return cache[key]
        var bmp: Bitmap? = null
        try {
            val res = ctx.resources.getIdentifier("theme_" + id + "_" + kind, "drawable", ctx.packageName)
            if (res != 0) bmp = android.graphics.BitmapFactory.decodeResource(ctx.resources, res)
        } catch (e: Exception) {
            bmp = null
        }
        cache[key] = bmp
        return bmp
    }
}
