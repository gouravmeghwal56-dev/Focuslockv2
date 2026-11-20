package com.focuslock.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View

val C_ORANGE = Color.parseColor("#FF6A0A")
val C_GRAY = Color.parseColor("#B5AEAC")
val C_TILE = Color.parseColor("#EBE6E6")
val C_DARK = Color.parseColor("#403B39")
val C_BG = Color.parseColor("#F6F3F3")
val C_TXT = Color.parseColor("#2E2927")
val C_CARD = Color.parseColor("#FCFAFA")
val C_LINE = Color.parseColor("#C9C2C0")

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
