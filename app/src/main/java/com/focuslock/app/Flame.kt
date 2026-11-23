package com.focuslock.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sin

/** Bag ke 4 slots ka order yaad rakhta hai (Store sirf set rakhta hai). */
object BagSlots {
    private fun p(c: Context) = c.getSharedPreferences("focus", Context.MODE_PRIVATE)

    fun get(c: Context): List<String> {
        val max = Store.MAX_BAG_APPS
        val sel = Store.bag(c, 0)
        val saved = p(c).getString("bag_slots", null)?.split(",") ?: emptyList()
        val out = MutableList(max) { i -> saved.getOrElse(i) { "" } }
        for (i in out.indices) {
            if (out[i] !in sel) out[i] = ""
        }
        for (pkg in sel.toList().sorted()) {
            if (pkg !in out) {
                val k = out.indexOf("")
                if (k >= 0) out[k] = pkg
            }
        }
        return out
    }

    fun save(c: Context, slots: List<String>) {
        val s = slots.take(Store.MAX_BAG_APPS)
        p(c).edit().putString("bag_slots", s.joinToString(",")).apply()
        Store.setBag(c, 0, s.filter { it.isNotBlank() }.toSet())
    }
}

/** Streak tile ki jalti hui aag. live = false to grey (streak 0). */
class FlameView(ctx: Context, private val live: Boolean) : View(ctx) {
    private var t = 0f
    private val path = Path()
    private val outer = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mid = Paint(Paint.ANTI_ALIAS_FLAG)
    private val inner = Paint(Paint.ANTI_ALIAS_FLAG)
    private val spark = Paint(Paint.ANTI_ALIAS_FLAG)
    private val twoPi = (2.0 * Math.PI).toFloat()

    private val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1500L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            t = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        if (live) {
            outer.color = Color.parseColor("#FF5A00")
            mid.color = Color.parseColor("#FF9A1F")
            inner.color = Color.parseColor("#FFE066")
            spark.color = Color.parseColor("#FFB020")
        } else {
            outer.color = Color.parseColor("#C9C2C0")
            mid.color = Color.parseColor("#D8D2D0")
            inner.color = Color.parseColor("#E8E3E2")
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (live) anim.start()
    }

    override fun onDetachedFromWindow() {
        anim.cancel()
        super.onDetachedFromWindow()
    }

    private fun flame(c: Canvas, cx: Float, bottom: Float, fw: Float, fh: Float, tip: Float, p: Paint) {
        val top = bottom - fh
        path.reset()
        path.moveTo(cx, bottom)
        path.cubicTo(cx - fw * 0.62f, bottom, cx - fw * 0.55f, bottom - fh * 0.45f, cx + tip, top)
        path.cubicTo(cx + fw * 0.5f, bottom - fh * 0.5f, cx + fw * 0.62f, bottom, cx, bottom)
        path.close()
        c.drawPath(path, p)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val ph = t * twoPi
        val cx = w / 2f
        val bottom = h * 0.97f
        val fl = if (live) 1f + 0.05f * sin(ph * 3f) + 0.03f * sin(ph * 5f + 1f) else 1f
        val sway = if (live) sin(ph) * w * 0.05f else 0f
        flame(c, cx, bottom, w * 0.82f, h * 0.80f * fl, sway, outer)
        flame(c, cx, bottom, w * 0.56f, h * 0.56f * (2f - fl), -sway * 0.6f, mid)
        flame(c, cx, bottom, w * 0.30f, h * 0.34f * fl, sway * 0.4f, inner)
        if (live) {
            for (k in 0..2) {
                val q = (t + k / 3f) % 1f
                val sx = cx + sin((q + k) * 6f) * w * 0.22f
                val sy = h * 0.65f - q * h * 0.6f
                spark.alpha = (255 * (1f - q)).toInt()
                c.drawCircle(sx, sy, w * 0.045f * (1f - q) + 1f, spark)
            }
        }
    }
}
