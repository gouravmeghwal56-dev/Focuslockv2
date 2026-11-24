package com.focuslock.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/** Settings aur Stats ke chhote line icons (24x24 grid par, canvas se bane - koi image file nahi). */
class LineIconView(ctx: Context, private val kind: Int) : View(ctx) {
    companion object {
        const val PERSON = 0
        const val PALETTE = 1
        const val BELL = 2
        const val ACCESS = 3
        const val BACKUP = 4
        const val RESTORE = 5
        const val INFO = 6
        const val GEAR = 7
        const val TARGET = 8
        const val BARS = 9
        const val STOPWATCH = 10
        const val LAYERS = 11
        const val CHECK = 12
        const val PENCIL = 13
        const val SLIDERS = 14
    }

    var tint = 0xFF3E3A39.toInt()
        set(v) {
            field = v
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()

    private fun line(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) {
        c.drawLine(x1, y1, x2, y2, paint)
    }

    private fun dot(c: Canvas, x: Float, y: Float, r: Float) {
        paint.style = Paint.Style.FILL
        c.drawCircle(x, y, r, paint)
        paint.style = Paint.Style.STROKE
    }

    private fun cloud(c: Canvas) {
        path.reset()
        path.moveTo(7f, 18f)
        path.cubicTo(4.5f, 18f, 3f, 16.3f, 3f, 14.3f)
        path.cubicTo(3f, 12.5f, 4.4f, 11f, 6.2f, 10.7f)
        path.cubicTo(6.6f, 8f, 8.9f, 6f, 11.7f, 6f)
        path.cubicTo(14.6f, 6f, 16.9f, 8f, 17.3f, 10.6f)
        path.cubicTo(19.4f, 10.8f, 21f, 12.4f, 21f, 14.5f)
        path.cubicTo(21f, 16.5f, 19.4f, 18f, 17.4f, 18f)
        c.drawPath(path, paint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val s = minOf(w, h) / 24f
        if (s <= 0f) return
        canvas.save()
        canvas.translate((w - 24f * s) / 2f, (h - 24f * s) / 2f)
        canvas.scale(s, s)
        paint.color = tint
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.8f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        when (kind) {
            PERSON -> {
                canvas.drawCircle(12f, 8f, 3.6f, paint)
                path.reset()
                path.moveTo(5f, 20f)
                path.cubicTo(5f, 15.5f, 8.5f, 13.8f, 12f, 13.8f)
                path.cubicTo(15.5f, 13.8f, 19f, 15.5f, 19f, 20f)
                canvas.drawPath(path, paint)
            }
            PALETTE -> {
                canvas.drawCircle(12f, 12f, 9f, paint)
                dot(canvas, 8f, 10.5f, 1.2f)
                dot(canvas, 12f, 7.6f, 1.2f)
                dot(canvas, 16f, 10.5f, 1.2f)
                dot(canvas, 8.8f, 15.2f, 1.2f)
            }
            BELL -> {
                path.reset()
                path.moveTo(6f, 17f)
                path.cubicTo(7.5f, 15.5f, 7.5f, 14f, 7.5f, 11f)
                path.cubicTo(7.5f, 8.2f, 9.5f, 6.5f, 12f, 6.5f)
                path.cubicTo(14.5f, 6.5f, 16.5f, 8.2f, 16.5f, 11f)
                path.cubicTo(16.5f, 14f, 16.5f, 15.5f, 18f, 17f)
                path.close()
                canvas.drawPath(path, paint)
                line(canvas, 12f, 4.3f, 12f, 6.5f)
                path.reset()
                path.moveTo(10.2f, 19.6f)
                path.cubicTo(10.6f, 20.7f, 13.4f, 20.7f, 13.8f, 19.6f)
                canvas.drawPath(path, paint)
            }
            ACCESS -> {
                canvas.drawCircle(12f, 12f, 9f, paint)
                dot(canvas, 12f, 7.6f, 1.2f)
                line(canvas, 8f, 10.6f, 16f, 10.6f)
                line(canvas, 12f, 10.6f, 12f, 14f)
                line(canvas, 12f, 14f, 9.6f, 17.6f)
                line(canvas, 12f, 14f, 14.4f, 17.6f)
            }
            BACKUP -> {
                cloud(canvas)
                line(canvas, 12f, 16f, 12f, 10.5f)
                path.reset()
                path.moveTo(9.6f, 12.8f)
                path.lineTo(12f, 10.4f)
                path.lineTo(14.4f, 12.8f)
                canvas.drawPath(path, paint)
            }
            RESTORE -> {
                cloud(canvas)
                line(canvas, 12f, 10.5f, 12f, 16f)
                path.reset()
                path.moveTo(9.6f, 13.7f)
                path.lineTo(12f, 16.1f)
                path.lineTo(14.4f, 13.7f)
                canvas.drawPath(path, paint)
            }
            INFO -> {
                canvas.drawCircle(12f, 12f, 9f, paint)
                dot(canvas, 12f, 8f, 1.1f)
                line(canvas, 12f, 11.2f, 12f, 16.6f)
            }
            GEAR -> {
                canvas.drawCircle(12f, 12f, 3.2f, paint)
                canvas.drawCircle(12f, 12f, 7f, paint)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val cs = Math.cos(a).toFloat()
                    val sn = Math.sin(a).toFloat()
                    line(canvas, 12f + cs * 7f, 12f + sn * 7f, 12f + cs * 9.6f, 12f + sn * 9.6f)
                }
            }
            TARGET -> {
                canvas.drawCircle(12f, 12f, 9f, paint)
                canvas.drawCircle(12f, 12f, 5.2f, paint)
                dot(canvas, 12f, 12f, 1.7f)
            }
            BARS -> {
                paint.strokeWidth = 2.6f
                line(canvas, 6f, 19f, 6f, 14f)
                line(canvas, 12f, 19f, 12f, 9.5f)
                line(canvas, 18f, 19f, 18f, 5f)
            }
            STOPWATCH -> {
                canvas.drawCircle(12f, 13.5f, 7.5f, paint)
                line(canvas, 9.6f, 3.6f, 14.4f, 3.6f)
                line(canvas, 12f, 3.6f, 12f, 6f)
                line(canvas, 12f, 13.5f, 12f, 9.6f)
            }
            LAYERS -> {
                path.reset()
                path.moveTo(12f, 3.5f)
                path.lineTo(21f, 8.3f)
                path.lineTo(12f, 13.1f)
                path.lineTo(3f, 8.3f)
                path.close()
                canvas.drawPath(path, paint)
                path.reset()
                path.moveTo(3f, 12.3f)
                path.lineTo(12f, 17.1f)
                path.lineTo(21f, 12.3f)
                canvas.drawPath(path, paint)
                path.reset()
                path.moveTo(3f, 16.3f)
                path.lineTo(12f, 21.1f)
                path.lineTo(21f, 16.3f)
                canvas.drawPath(path, paint)
            }
            CHECK -> {
                rect.set(4f, 4.5f, 20f, 20.5f)
                canvas.drawRoundRect(rect, 3.5f, 3.5f, paint)
                path.reset()
                path.moveTo(8.4f, 12.6f)
                path.lineTo(11f, 15.2f)
                path.lineTo(15.8f, 9.8f)
                canvas.drawPath(path, paint)
            }
            PENCIL -> {
                path.reset()
                path.moveTo(4f, 20f)
                path.lineTo(5.2f, 15.4f)
                path.lineTo(15.8f, 4.8f)
                path.lineTo(19.2f, 8.2f)
                path.lineTo(8.6f, 18.8f)
                path.close()
                canvas.drawPath(path, paint)
                line(canvas, 13.6f, 7f, 17f, 10.4f)
            }
            SLIDERS -> {
                line(canvas, 4f, 7f, 7f, 7f)
                line(canvas, 11f, 7f, 20f, 7f)
                canvas.drawCircle(9f, 7f, 2f, paint)
                line(canvas, 4f, 12f, 13f, 12f)
                line(canvas, 17f, 12f, 20f, 12f)
                canvas.drawCircle(15f, 12f, 2f, paint)
                line(canvas, 4f, 17f, 7f, 17f)
                line(canvas, 11f, 17f, 20f, 17f)
                canvas.drawCircle(9f, 17f, 2f, paint)
            }
        }
        canvas.restore()
    }
}
