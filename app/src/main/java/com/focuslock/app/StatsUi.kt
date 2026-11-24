package com.focuslock.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View

/** Stats page ka bar chart: har slot ek din / mahina. Bar par tap = wo din/mahina chuno. */
class BarChartView(ctx: Context) : View(ctx) {
    var values: IntArray = IntArray(0)
    var labels: Array<String> = emptyArray()
    var selected = -1
    var futureFrom = Int.MAX_VALUE
    var barColor = 0xFFFF6A0D.toInt()
    var trackColor = 0x14000000
    var textColor = 0xFF3E3A39.toInt()
    var onPick: ((Int) -> Unit)? = null

    private val dens = ctx.resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val n = values.size
        if (n == 0) return
        val w = width.toFloat()
        val h = height.toFloat()
        val labelH = dens * 22f
        val top = dens * 4f
        val barsH = h - labelH - top
        if (barsH <= 0f) return
        val slot = w / n
        val bw = minOf(slot * 0.62f, dens * 38f)
        val r = minOf(bw / 2f, dens * 8f)
        var mx = 1
        for (v in values) if (v > mx) mx = v

        paint.textAlign = Paint.Align.CENTER
        paint.textSize = dens * 11f
        for (i in 0 until n) {
            val cx = slot * i + slot / 2f
            val future = i >= futureFrom
            // track (poori height ka halka bar)
            paint.style = Paint.Style.FILL
            paint.color = trackColor
            if (future) paint.alpha = (paint.alpha * 0.5f).toInt()
            rect.set(cx - bw / 2f, top, cx + bw / 2f, top + barsH)
            canvas.drawRoundRect(rect, r, r, paint)
            // fill
            val v = values[i]
            if (v > 0 && !future) {
                var fh = barsH * v / mx
                if (fh < dens * 8f) fh = dens * 8f
                paint.color = barColor
                paint.alpha = if (selected < 0 || selected == i) 255 else 120
                rect.set(cx - bw / 2f, top + barsH - fh, cx + bw / 2f, top + barsH)
                canvas.drawRoundRect(rect, r, r, paint)
            }
            // label
            val lb = labels.getOrNull(i) ?: ""
            if (lb.isNotEmpty()) {
                paint.color = textColor
                paint.alpha = if (selected == i) 255 else 150
                paint.typeface = if (selected == i) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                canvas.drawText(lb, cx, h - dens * 5f, paint)
            }
        }
        paint.alpha = 255
        paint.typeface = Typeface.DEFAULT
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val n = values.size
        if (n == 0 || onPick == null) return false
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                val i = (e.x / (width.toFloat() / n)).toInt().coerceIn(0, n - 1)
                if (i < futureFrom) onPick?.invoke(i)
                return true
            }
        }
        return true
    }
}

/** Patli progress bar (Daily Focus Goal). */
class ProgressBarView(ctx: Context) : View(ctx) {
    var frac = 0f
    var fillColor = 0xFFFF6A0D.toInt()
    var trackColor = 0x14000000

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val r = h / 2f
        paint.style = Paint.Style.FILL
        paint.color = trackColor
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, r, r, paint)
        val f = frac.coerceIn(0f, 1f)
        if (f > 0f) {
            paint.color = fillColor
            rect.set(0f, 0f, maxOf(h, w * f), h)
            canvas.drawRoundRect(rect, r, r, paint)
        }
    }
}

/** Goal Completion: chhote chhote dibbe. 1 = goal poora, 0 = nahi hua, 2 = aage ka din. */
class GoalDotsView(ctx: Context) : View(ctx) {
    var states: IntArray = IntArray(0)
    var fillColor = 0xFFFF6A0D.toInt()
    var trackColor = 0x14000000

    private val dens = ctx.resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val n = states.size
        if (n == 0) return
        val w = width.toFloat()
        val h = height.toFloat()
        val gap = dens * 4f
        var s = (w - gap * (n - 1)) / n
        s = minOf(s, dens * 16f, h)
        if (s <= 0f) return
        val total = s * n + gap * (n - 1)
        var x = (w - total) / 2f
        val y = (h - s) / 2f
        paint.style = Paint.Style.FILL
        for (i in 0 until n) {
            when (states[i]) {
                1 -> { paint.color = fillColor; paint.alpha = 255 }
                2 -> { paint.color = trackColor; paint.alpha = (Color_a(trackColor) * 0.5f).toInt() }
                else -> { paint.color = trackColor; paint.alpha = Color_a(trackColor) }
            }
            rect.set(x, y, x + s, y + s)
            canvas.drawRoundRect(rect, s * 0.32f, s * 0.32f, paint)
            x += s + gap
        }
        paint.alpha = 255
    }

    private fun Color_a(c: Int): Int = (c ushr 24) and 0xFF
}
