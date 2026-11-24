package com.focuslock.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View

/**
 * Stats page ka bar chart: har slot ek din / mahina. Bar par tap = wo din/mahina chuno.
 * - label mein "\n" ho to 2 line (upar din ka naam, neeche taareekh).
 * - thin = true: month chart ke liye patli saaf bars (poori height ki track-lines nahi).
 * - selText: chuni hui bar ke upar chhota text (taareekh + minutes).
 */
class BarChartView(ctx: Context) : View(ctx) {
    var values: IntArray = IntArray(0)
    var labels: Array<String> = emptyArray()
    var selected = -1
    var todayIdx = -1
    var futureFrom = Int.MAX_VALUE
    var thin = false
    var selText = ""
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
        val twoLine = labels.any { it.contains('\n') }
        val labelH = dens * (if (twoLine) 36f else 22f)
        val top = dens * (if (selText.isNotEmpty()) 20f else 4f)
        val barsH = h - labelH - top
        if (barsH <= 0f) return
        val slot = w / n
        val bw = if (thin) minOf(slot * 0.55f, dens * 9f) else minOf(slot * 0.62f, dens * 34f)
        val r = minOf(bw / 2f, dens * 8f)
        var mx = 1
        for (v in values) if (v > mx) mx = v

        paint.textAlign = Paint.Align.CENTER
        for (i in 0 until n) {
            val cx = slot * i + slot / 2f
            val future = i >= futureFrom
            val v = values[i]
            paint.style = Paint.Style.FILL
            if (!thin) {
                // track (poori height ka halka bar)
                paint.color = trackColor
                paint.alpha = if (future) (Color_a(trackColor) * 0.5f).toInt() else Color_a(trackColor)
                rect.set(cx - bw / 2f, top, cx + bw / 2f, top + barsH)
                canvas.drawRoundRect(rect, r, r, paint)
            }
            if (v > 0 && !future) {
                var fh = barsH * v / mx
                if (fh < dens * (if (thin) 6f else 8f)) fh = dens * (if (thin) 6f else 8f)
                paint.color = barColor
                paint.alpha = if (selected < 0 || selected == i) 255 else 130
                rect.set(cx - bw / 2f, top + barsH - fh, cx + bw / 2f, top + barsH)
                canvas.drawRoundRect(rect, r, r, paint)
            } else if (thin) {
                // khali din: chhota sa dibba (aage ka din aur halka)
                paint.color = trackColor
                paint.alpha = if (future) (Color_a(trackColor) * 0.5f).toInt() else Color_a(trackColor)
                val sh = dens * 4f
                rect.set(cx - bw / 2f, top + barsH - sh, cx + bw / 2f, top + barsH)
                canvas.drawRoundRect(rect, bw / 2f, bw / 2f, paint)
            }
            // label (1 ya 2 line)
            val lb = labels.getOrNull(i) ?: ""
            if (lb.isNotEmpty()) {
                val isSel = selected == i
                val isToday = todayIdx == i
                val parts = lb.split('\n')
                paint.color = if (isToday) barColor else textColor
                paint.alpha = if (isSel || isToday) 255 else 150
                paint.typeface = if (isSel || isToday) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                val base = h - dens * 5f
                if (parts.size >= 2) {
                    paint.textSize = dens * 10f
                    canvas.drawText(parts[0], cx, base - dens * 15f, paint)
                    paint.textSize = dens * 13f
                    canvas.drawText(parts[1], cx, base, paint)
                } else {
                    paint.textSize = dens * 11f
                    canvas.drawText(lb, cx, base, paint)
                }
            }
        }
        // chuni hui bar ke upar taareekh + minutes
        if (selText.isNotEmpty() && selected in 0 until n) {
            paint.textSize = dens * 12f
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.color = textColor
            paint.alpha = 255
            val tw = paint.measureText(selText)
            val cx = (slot * selected + slot / 2f).coerceIn(tw / 2f + dens * 2f, w - tw / 2f - dens * 2f)
            canvas.drawText(selText, cx, dens * 14f, paint)
        }
        paint.alpha = 255
        paint.typeface = Typeface.DEFAULT
    }

    private fun Color_a(c: Int): Int = (c ushr 24) and 0xFF

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
