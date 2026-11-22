package com.focuslock.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val F_CARD = Color.parseColor("#1D1D1D")
private val F_DIM = Color.parseColor("#7C7C7C")
private val F_OFF = Color.parseColor("#3A3A3A")
private val F_MASKS = arrayOf("abcdef", "bc", "abdeg", "abcdg", "bcfg", "acdfg", "acdefg", "abc", "abcdefg", "abcdfg")

// ---------- Small icons (phone, camera, parachute) ----------

class GlyphView(ctx: Context, private val kind: Int) : View(ctx) {
    var tint: Int = F_CARD
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rf = RectF()

    companion object {
        const val PHONE = 0
        const val CAMERA = 1
        const val CHUTE = 2
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
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.9f * u

        fun x(v: Float) = ox + v * u
        fun y(v: Float) = oy + v * u

        when (kind) {
            PHONE -> {
                canvas.save()
                canvas.rotate(-45f, x(12f), y(12f))
                p.strokeWidth = 3.4f * u
                p.strokeCap = Paint.Cap.BUTT
                rf.set(x(4f), y(5f), x(20f), y(21f))
                canvas.drawArc(rf, 180f, 180f, false, p)
                p.style = Paint.Style.FILL
                rf.set(x(2.2f), y(12.5f), x(7f), y(18f))
                canvas.drawRoundRect(rf, 1.5f * u, 1.5f * u, p)
                rf.set(x(17f), y(12.5f), x(21.8f), y(18f))
                canvas.drawRoundRect(rf, 1.5f * u, 1.5f * u, p)
                canvas.restore()
            }
            CAMERA -> {
                rf.set(x(3f), y(7f), x(21f), y(20f))
                canvas.drawRoundRect(rf, 3f * u, 3f * u, p)
                canvas.drawCircle(x(12f), y(13.5f), 3.8f * u, p)
                path.reset()
                path.moveTo(x(8.5f), y(7f))
                path.lineTo(x(9.5f), y(4.5f))
                path.lineTo(x(14.5f), y(4.5f))
                path.lineTo(x(15.5f), y(7f))
                canvas.drawPath(path, p)
            }
            else -> {
                p.strokeWidth = 1.8f * u
                rf.set(x(3f), y(2.5f), x(21f), y(19.5f))
                canvas.drawArc(rf, 180f, 180f, false, p)
                canvas.drawLine(x(3f), y(11f), x(21f), y(11f), p)
                canvas.drawLine(x(3f), y(11f), x(12f), y(19f), p)
                canvas.drawLine(x(21f), y(11f), x(12f), y(19f), p)
                canvas.drawLine(x(12f), y(11f), x(12f), y(19f), p)
                p.style = Paint.Style.FILL
                rf.set(x(10f), y(19f), x(14f), y(22.5f))
                canvas.drawRoundRect(rf, 1f * u, 1f * u, p)
            }
        }
    }
}

// ---------- Pixel digits (for the SEC tile) ----------

class PixelNumberView(ctx: Context) : View(ctx) {
    private var text = "00"
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    fun setText(s: String) {
        if (s != text) {
            text = s
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = Color.WHITE
        val n = text.length
        val w = width.toFloat()
        val h = height.toFloat()
        val gap = 0.25f
        val dw = minOf(w * 0.8f / (n + gap * (n - 1)), h * 0.8f / 1.6f)
        val dh = dw * 1.6f
        val t = dw * 0.30f
        val total = dw * n + dw * gap * (n - 1)
        var x = (w - total) / 2f
        val y = (h - dh) / 2f
        for (ch in text) {
            val d = ch - '0'
            if (d in 0..9) drawDigit(canvas, d, x, y, dw, dh, t)
            x += dw * (1f + gap)
        }
    }

    private fun box(c: Canvas, x: Float, y: Float, w: Float, h: Float, r: Float) {
        rect.set(x, y, x + w, y + h)
        c.drawRoundRect(rect, r, r, paint)
    }

    private fun drawDigit(c: Canvas, d: Int, x: Float, y: Float, dw: Float, dh: Float, t: Float) {
        val m = F_MASKS[d]
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

// ---------- "+15 HOLD" tile: dabaye rakho, 15 minute badh jate hain ----------

class HoldView(ctx: Context, private val onDone: () -> Unit) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var prog = 0f
    private var anim: ValueAnimator? = null
    private var done = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        paint.color = Color.WHITE
        paint.textSize = h * 0.26f
        canvas.drawText("+15", w / 2f, h * 0.45f, paint)
        paint.color = C_ORANGE
        paint.textSize = h * 0.09f
        canvas.drawText("HOLD", w / 2f, h * 0.58f, paint)

        val s = h * 0.075f
        val gap = s * 0.5f
        val total = s * 4f + gap * 3f
        var x = (w - total) / 2f
        val filled = Math.ceil((prog * 4f).toDouble()).toInt()
        for (i in 0 until 4) {
            paint.color = if (i < filled) Color.WHITE else F_OFF
            rect.set(x, h * 0.68f, x + s, h * 0.68f + s)
            canvas.drawRoundRect(rect, s * 0.2f, s * 0.2f, paint)
            x += s + gap
        }
    }

    private fun begin() {
        anim?.cancel()
        done = false
        prog = 0f
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 1600L
        a.interpolator = LinearInterpolator()
        a.addUpdateListener {
            prog = it.animatedValue as Float
            invalidate()
        }
        a.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (prog >= 1f && !done) {
                    done = true
                    prog = 0f
                    invalidate()
                    onDone()
                }
            }
        })
        a.start()
        anim = a
    }

    private fun reset() {
        anim?.cancel()
        prog = 0f
        invalidate()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                begin()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> reset()
        }
        return true
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        super.onDetachedFromWindow()
    }
}

// ---------- Sound tile: tap = Rain -> Waves -> City -> Birds -> Off ----------

class RainView(
    ctx: Context,
    private val onLabel: (String) -> Unit,
    private val onLaunch: (String) -> Unit,
    private val onMenu: () -> Unit,
    private val onAccess: () -> Unit
) : View(ctx) {
    private class Item(val mode: Int, val pkg: String?, val name: String)

    private var items: List<Item> = emptyList()
    private var idx = 0
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var t = 0f
    private var anim: ValueAnimator? = null
    private var swipeAnim: ValueAnimator? = null
    private var swipeP = 1f
    private var swipeDir = 1
    private val dens = ctx.resources.displayMetrics.density
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
    private val xs = FloatArray(30) { (it * 37 % 30) / 30f }
    private val sp = FloatArray(30) { 0.6f + (it * 13 % 7) / 7f * 0.8f }
    private val ln = FloatArray(30) { 0.05f + (it * 5 % 4) * 0.03f }
    private val tp = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var media: Media.Info? = null
    private var hasAccess = false
    private val poll = object : Runnable {
        override fun run() {
            refreshMedia()
            postDelayed(this, 1000L)
        }
    }
    private var downX = 0f
    private var downY = 0f
    private var moved = false
    private var longFired = false
    private val longRun = Runnable {
        longFired = true
        onMenu()
    }

    init {
        load()
    }

    private fun load() {
        val pm = context.packageManager
        val list = ArrayList<Item>()
        for (m in 1..Ambient.COUNT) list.add(Item(m, null, Ambient.NAMES[m]))
        for (pkg in Store.soundApps(context)) {
            try {
                val label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                list.add(Item(0, pkg, label.uppercase()))
            } catch (e: Exception) {
            }
        }
        items = list
        val cur = items.indexOfFirst { it.pkg == null && it.mode == Ambient.mode }
        idx = if (cur >= 0) cur else Store.sndIdx(context).coerceIn(0, items.size - 1)
    }

    /** Music app add / remove ke baad list dobara banao. */
    fun reload() {
        load()
        updateLabel()
        invalidate()
    }

    private fun playing(): Boolean {
        val it = items.getOrNull(idx) ?: return false
        return it.pkg == null && Ambient.mode == it.mode
    }

    private fun updateLabel() {
        val it = items.getOrNull(idx) ?: return
        val hint = when {
            it.pkg != null && !hasAccess -> "TAP TO ALLOW ACCESS"
            it.pkg != null -> "TAP TITLE TO OPEN"
            playing() -> "TAP TO STOP"
            else -> "TAP TO PLAY"
        }
        onLabel(it.name + "  \u00B7  " + hint)
    }

    private fun refreshMedia() {
        val pkg = items.getOrNull(idx)?.pkg
        if (pkg == null) {
            media = null
            return
        }
        val acc = Media.hasAccess(context)
        val m = if (acc) Media.get(context, pkg) else null
        val old = media
        val changed = acc != hasAccess || m?.title != old?.title || m?.artist != old?.artist || m?.playing != old?.playing
        hasAccess = acc
        media = m
        if (changed) {
            updateLabel()
            invalidate()
        }
    }

    private fun tap(x: Float, y: Float) {
        val it = items.getOrNull(idx) ?: return
        if (it.pkg != null) {
            if (!hasAccess) {
                onAccess()
                return
            }
            val w = width.toFloat()
            val cy = height * 0.72f
            val r = dens * 34f
            val dx = dens * 74f
            val m = media
            if (Math.abs(y - cy) < r) {
                when {
                    Math.abs(x - w / 2f) < r -> {
                        if (m == null) onLaunch(it.pkg)
                        else if (m.playing) m.ctl.transportControls.pause()
                        else m.ctl.transportControls.play()
                    }
                    Math.abs(x - (w / 2f - dx)) < r -> m?.ctl?.transportControls?.skipToPrevious()
                    Math.abs(x - (w / 2f + dx)) < r -> m?.ctl?.transportControls?.skipToNext()
                    else -> onLaunch(it.pkg)
                }
                postDelayed({ refreshMedia() }, 300L)
            } else {
                onLaunch(it.pkg)
            }
            return
        }
        if (Ambient.mode == it.mode) {
            Ambient.fadeOut()
        } else {
            Ambient.set(it.mode)
        }
        updateLabel()
        invalidate()
    }

    private fun swipe(dir: Int) {
        if (items.size < 2) return
        idx = (idx + dir + items.size) % items.size
        Store.setSndIdx(context, idx)
        val it = items[idx]
        if (Ambient.mode != Ambient.OFF) {
            if (it.pkg == null) Ambient.set(it.mode) else Ambient.fadeOut()
        }
        media = null
        refreshMedia()
        swipeDir = dir
        swipeAnim?.cancel()
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 260L
        a.addUpdateListener {
            swipeP = it.animatedValue as Float
            invalidate()
        }
        a.start()
        swipeAnim = a
        updateLabel()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                downY = e.y
                moved = false
                longFired = false
                parent?.requestDisallowInterceptTouchEvent(true)
                postDelayed(longRun, 650L)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!moved && (Math.abs(e.x - downX) > slop || Math.abs(e.y - downY) > slop)) {
                    moved = true
                    removeCallbacks(longRun)
                }
            }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(longRun)
                if (!longFired) {
                    val dx = e.x - downX
                    val dy = e.y - downY
                    if (Math.abs(dx) > 48f * dens && Math.abs(dx) > Math.abs(dy) * 1.2f) {
                        swipe(if (dx < 0f) 1 else -1)
                    } else if (!moved) {
                        tap(e.x, e.y)
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> removeCallbacks(longRun)
        }
        return true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateLabel()
        removeCallbacks(poll)
        post(poll)
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 2800L
        a.repeatCount = ValueAnimator.INFINITE
        a.interpolator = LinearInterpolator()
        a.addUpdateListener {
            t = it.animatedValue as Float
            if (playing()) invalidate()
        }
        a.start()
        anim = a
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        swipeAnim?.cancel()
        removeCallbacks(longRun)
        removeCallbacks(poll)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val it = items.getOrNull(idx) ?: return
        val live = playing()
        val alpha = ((if (live || it.pkg != null) 255 else 120) * swipeP).toInt().coerceIn(0, 255)
        val off = (1f - swipeP) * swipeDir * w * 0.35f
        val sc = canvas.saveLayerAlpha(0f, 0f, w, h, alpha)
        canvas.translate(off, 0f)
        val tt = if (live) t else 0.25f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = dens * 1.6f
        paint.color = Color.parseColor("#8A8A8A")
        if (it.pkg != null) {
            drawApp(canvas, it.pkg, w, h)
        } else {
            when (it.mode) {
                Ambient.RAIN -> {
                    val p2 = tt * 2f % 1f
                    for (i in xs.indices) {
                        val prog = (p2 * sp[i] * 2f + i * 0.137f) % 1f
                        val y = prog * (h * 1.2f) - h * 0.1f
                        val x = xs[i] * w
                        val l = ln[i] * h
                        canvas.drawLine(x, y, x - l * 0.2f, y + l, paint)
                    }
                }
                Ambient.WAVES -> {
                    for (k in 0..2) {
                        val base = h * (0.42f + 0.14f * k)
                        val dir = if (k % 2 == 0) 1f else -1f
                        val amp = h * 0.045f
                        var px = 0f
                        var py = base + amp * Math.sin((tt * 2f * dir * Math.PI * 1.0)).toFloat()
                        var x = 14f
                        while (x <= w + 14f) {
                            val y = base + amp * Math.sin(x / w * (2.0 * Math.PI) * (1.3 + k * 0.4) + tt * 2.0 * Math.PI * dir).toFloat()
                            canvas.drawLine(px, py, x, y, paint)
                            px = x
                            py = y
                            x += 14f
                        }
                    }
                }
                Ambient.WIND -> {
                    for (k in 0..3) {
                        val y0 = h * (0.25f + 0.17f * k)
                        val shift = ((tt * (0.8f + k * 0.15f) + k * 0.31f) % 1f) * w * 0.5f
                        var px = 0f
                        var py = y0
                        var x = 12f
                        while (x <= w) {
                            val y = y0 + Math.sin((x - shift) / w * 2.0 * Math.PI * 1.5).toFloat() * h * 0.04f
                            if (x > w * 0.12f + shift * 0.2f && x < w * 0.88f) canvas.drawLine(px, py, x, y, paint)
                            px = x
                            py = y
                            x += 12f
                        }
                    }
                }
                Ambient.BIRDS -> {
                    for (i in 0..3) {
                        val x = (((tt * (0.6f + i * 0.12f)) + i * 0.27f) % 1.3f - 0.15f) * w
                        val y = h * (0.22f + 0.15f * i) + Math.sin(tt * 2.0 * Math.PI + i).toFloat() * h * 0.03f
                        val flap = Math.sin(tt * 2.0 * Math.PI * 4.0 + i).toFloat() * 0.5f
                        val s = dens * 11f
                        canvas.drawLine(x - s, y - s * flap, x, y, paint)
                        canvas.drawLine(x, y, x + s, y - s * flap, paint)
                    }
                }
                Ambient.FIRE -> {
                    paint.style = Paint.Style.FILL
                    val base = h * 0.80f
                    for (k in 0..2) {
                        val cx = w * (0.38f + 0.12f * k)
                        val sway = Math.sin((tt * 2.0 + k * 0.33) * 2.0 * Math.PI).toFloat()
                        val fh = h * (if (k == 1) 0.46f else 0.34f) * (1f + 0.12f * sway)
                        val fw = w * (if (k == 1) 0.085f else 0.065f)
                        path.reset()
                        path.moveTo(cx - fw, base)
                        path.quadTo(cx - fw * 1.1f, base - fh * 0.5f, cx + sway * fw * 0.5f, base - fh)
                        path.quadTo(cx + fw * 1.1f, base - fh * 0.5f, cx + fw, base)
                        path.close()
                        paint.color = if (k == 1) C_ORANGE else Color.parseColor("#B04A08")
                        canvas.drawPath(path, paint)
                    }
                    paint.color = Color.parseColor("#8A8A8A")
                    for (i in 0..4) {
                        val prog = (tt * (0.7f + i * 0.1f) + i * 0.21f) % 1f
                        val x = w * (0.40f + 0.05f * i) + Math.sin(prog * 6.0 + i).toFloat() * w * 0.02f
                        canvas.drawCircle(x, base - prog * h * 0.6f, dens * 1.6f, paint)
                    }
                }
                Ambient.NIGHT -> {
                    paint.style = Paint.Style.FILL
                    paint.color = Color.parseColor("#B8B8B8")
                    canvas.drawCircle(w * 0.72f, h * 0.34f, h * 0.15f, paint)
                    paint.color = Color.parseColor("#1D1D1D")
                    canvas.drawCircle(w * 0.72f + h * 0.07f, h * 0.34f - h * 0.03f, h * 0.14f, paint)
                    paint.color = Color.parseColor("#8A8A8A")
                    for (i in 0..9) {
                        val sx = w * ((i * 37 % 10) / 10f + 0.05f)
                        val sy = h * (0.15f + ((i * 53) % 10) / 10f * 0.5f)
                        val tw = 0.5f + 0.5f * Math.sin((tt * 2.0 + i * 0.37) * 2.0 * Math.PI).toFloat()
                        paint.alpha = (60 + 195 * tw).toInt().coerceIn(0, 255)
                        canvas.drawCircle(sx, sy, dens * (1.2f + 0.8f * tw), paint)
                    }
                    paint.alpha = 255
                }
            }
        }
        canvas.restoreToCount(sc)

        // dots: kitni sounds hain aur kaun si chuni hai (swipe ka hint)
        val n = items.size
        if (n > 1) {
            paint.style = Paint.Style.FILL
            val r = dens * 2.6f
            val gap = dens * 11f
            val x0 = (w - gap * (n - 1)) / 2f
            for (i in 0 until n) {
                paint.color = if (i == idx) Color.WHITE else Color.parseColor("#4A4A4A")
                canvas.drawCircle(x0 + gap * i, dens * 14f, r, paint)
            }
        }
    }

    private fun fit(text: String, maxW: Float): String =
        android.text.TextUtils.ellipsize(text, tp, maxW, android.text.TextUtils.TruncateAt.END).toString()

    /** Music app: upar icon, beech me abhi ka track, niche prev / play-pause / next. */
    private fun drawApp(canvas: Canvas, pkg: String, w: Float, h: Float) {
        val cover = if (hasAccess) media?.art else null
        if (cover != null) {
            drawCover(canvas, cover, w, h)
        } else {
            try {
                val d = context.packageManager.getApplicationIcon(pkg)
                val s = (Math.min(w, h) * 0.2f).toInt()
                val cx = (w / 2f).toInt()
                val cy = (h * 0.20f).toInt()
                d.setBounds(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2)
                d.draw(canvas)
            } catch (e: Exception) {
            }
        }
        tp.textAlign = Paint.Align.CENTER
        tp.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        if (!hasAccess) {
            tp.color = Color.WHITE
            tp.textSize = dens * 14f
            canvas.drawText(fit("Allow notification access", w * 0.88f), w / 2f, h * 0.50f, tp)
            tp.color = Color.parseColor("#8A8A8A")
            tp.textSize = dens * 12f
            canvas.drawText(fit("to see track + controls here", w * 0.88f), w / 2f, h * 0.59f, tp)
            return
        }
        val m = media
        val title = if (m == null) "Nothing playing" else m.title.ifBlank { "Playing" }
        val artist = if (m == null) "Tap play to open app" else m.artist
        tp.color = Color.WHITE
        tp.textSize = dens * 15f
        canvas.drawText(fit(title, w * 0.88f), w / 2f, h * 0.44f, tp)
        tp.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        tp.color = Color.parseColor("#8A8A8A")
        tp.textSize = dens * 12f
        canvas.drawText(fit(artist, w * 0.88f), w / 2f, h * 0.52f, tp)

        val y = h * 0.72f
        val dx = dens * 74f
        drawSkip(canvas, w / 2f - dx, y, false)
        drawSkip(canvas, w / 2f + dx, y, true)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawCircle(w / 2f, y, dens * 24f, paint)
        paint.color = Color.parseColor("#1D1D1D")
        val u = dens * 10f
        val cx = w / 2f
        if (m != null && m.playing) {
            canvas.drawRect(cx - u * 0.7f, y - u, cx - u * 0.15f, y + u, paint)
            canvas.drawRect(cx + u * 0.15f, y - u, cx + u * 0.7f, y + u, paint)
        } else {
            path.reset()
            path.moveTo(cx - u * 0.6f, y - u)
            path.lineTo(cx - u * 0.6f, y + u)
            path.lineTo(cx + u, y)
            path.close()
            canvas.drawPath(path, paint)
        }
    }

    private val srcR = android.graphics.Rect()
    private val dstR = RectF()

    /** Gaane ki photo: beech se square crop + gol kone. */
    private fun drawCover(canvas: Canvas, b: android.graphics.Bitmap, w: Float, h: Float) {
        val s = Math.min(w, h) * 0.30f
        val cx = w / 2f
        val cy = h * 0.22f
        dstR.set(cx - s / 2f, cy - s / 2f, cx + s / 2f, cy + s / 2f)
        val side = Math.min(b.width, b.height)
        srcR.set((b.width - side) / 2, (b.height - side) / 2, (b.width + side) / 2, (b.height + side) / 2)
        val sv = canvas.save()
        path.reset()
        path.addRoundRect(dstR, dens * 12f, dens * 12f, Path.Direction.CW)
        canvas.clipPath(path)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.isFilterBitmap = true
        canvas.drawBitmap(b, srcR, dstR, paint)
        canvas.restoreToCount(sv)
    }

    private fun drawSkip(canvas: Canvas, cx: Float, cy: Float, next: Boolean) {
        paint.style = Paint.Style.FILL
        paint.color = if (media != null) Color.WHITE else Color.parseColor("#5A5A5A")
        val u = dens * 10f
        val d = if (next) 1f else -1f
        path.reset()
        path.moveTo(cx - d * u * 0.7f, cy - u)
        path.lineTo(cx - d * u * 0.7f, cy + u)
        path.lineTo(cx + d * u * 0.5f, cy)
        path.close()
        canvas.drawPath(path, paint)
        val a = cx + d * u * 0.65f
        val b = cx + d * u * 0.95f
        canvas.drawRect(Math.min(a, b), cy - u, Math.max(a, b), cy + u, paint)
    }
}

// ---------- "SLIDE TO EXIT" (parachute left ki taraf kheencho) ----------

class ExitSlideView(ctx: Context, private val onDone: () -> Unit) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val icon = GlyphView(ctx, GlyphView.CHUTE)
    private var prog = 0f
    private var dragging = false
    private var back: ValueAnimator? = null

    private fun snapBack() {
        back?.cancel()
        val a = ValueAnimator.ofFloat(prog, 0f)
        a.duration = 220L
        a.addUpdateListener {
            prog = it.animatedValue as Float
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
        val knobLeft = (1f - prog) * maxX

        paint.style = Paint.Style.FILL
        paint.color = F_CARD
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, r, r, paint)

        paint.color = F_DIM
        paint.alpha = (255f * (1f - prog)).toInt().coerceIn(40, 255)
        paint.textSize = h * 0.24f
        paint.textAlign = Paint.Align.CENTER
        paint.letterSpacing = 0.15f
        val base = h / 2f - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText("SLIDE TO EXIT", (w - kw) / 2f, base, paint)
        paint.alpha = 255
        paint.letterSpacing = 0f

        paint.color = Color.parseColor("#F2F2F2")
        rect.set(knobLeft, 0f, knobLeft + kw, h)
        canvas.drawRoundRect(rect, r, r, paint)

        val s = (h * 0.5f).toInt()
        icon.layout(0, 0, s, s)
        canvas.save()
        canvas.translate(knobLeft + (kw - s) / 2f, (h - s) / 2f)
        icon.draw(canvas)
        canvas.restore()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val h = height.toFloat()
        val kw = h * 1.25f
        val maxX = (width.toFloat() - kw).coerceAtLeast(1f)
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                back?.cancel()
                dragging = e.x >= (1f - prog) * maxX - kw * 0.3f
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging) {
                    val kl = (e.x - kw / 2f).coerceIn(0f, maxX)
                    prog = 1f - kl / maxX
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                val ok = dragging && prog >= 0.9f
                dragging = false
                snapBack()
                if (ok) onDone()
            }
            MotionEvent.ACTION_CANCEL -> {
                dragging = false
                snapBack()
            }
        }
        return true
    }
}

// ---------- The black focus screen ----------

class FocusScreen(private val act: Activity, private val onExit: () -> Unit) {

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private lateinit var clock: PixelClockView
    private lateinit var secView: PixelNumberView
    private lateinit var endsTv: TextView
    private lateinit var timerTv: TextView
    private lateinit var swBtn: IconView
    private lateinit var lapTv: TextView
    private lateinit var lapBtn: TextView
    private var lastSwRunning = false

    private fun swText(): CharSequence {
        val e = Store.swElapsed(act)
        val s = e / 1000L
        val sb = SpannableStringBuilder(String.format("%d:%02d:%02d", s / 3600L, (s % 3600L) / 60L, s % 60L))
        val st = sb.length
        sb.append(String.format(".%02d", (e % 1000L) / 10L))
        sb.setSpan(RelativeSizeSpan(0.6f), st, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(ForegroundColorSpan(C_ORANGE), st, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
    }

    private val swLaps = ArrayList<Long>()

    private fun fmtShort(ms: Long): String {
        val s = ms / 1000L
        val cs = (ms % 1000L) / 10L
        return if (s >= 3600L) String.format("%d:%02d:%02d.%02d", s / 3600L, (s % 3600L) / 60L, s % 60L, cs)
        else String.format("%d:%02d.%02d", s / 60L, s % 60L, cs)
    }

    private fun updateLaps() {
        val n = swLaps.size
        if (n == 0) {
            lapTv.text = ""
            return
        }
        val sb = StringBuilder()
        for (i in n - 1 downTo maxOf(0, n - 2)) {
            val split = swLaps[i] - (if (i > 0) swLaps[i - 1] else 0L)
            if (sb.isNotEmpty()) sb.append("\n")
            sb.append("LAP ").append(i + 1).append("   ").append(fmtShort(split)).append("   \u00B7   ").append(fmtShort(swLaps[i]))
        }
        lapTv.text = sb
    }

    // stopwatch chalte time milliseconds smooth dikhane ke liye ~30 fps refresh
    private val swHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val swTick = object : Runnable {
        override fun run() {
            if (!view.isAttachedToWindow) return
            timerTv.text = swText()
            if (Store.swRunning(act)) swHandler.postDelayed(this, 33L)
        }
    }
    private lateinit var chuteTv: TextView
    private var lastSec = -1
    private var lastUntil = 0L
    private var lastChutes = -1

    val view: View = build()

    private fun dp(v: Int) = (v * act.resources.displayMetrics.density).toInt()

    private fun cardBg(r: Int = 26): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(F_CARD)
        g.cornerRadius = dp(r).toFloat()
        return g
    }

    private fun lp(w: Int, h: Int, wt: Float, m: Int = 5): LinearLayout.LayoutParams {
        val l = LinearLayout.LayoutParams(w, h, wt)
        l.setMargins(dp(m), dp(m), dp(m), dp(m))
        return l
    }

    private fun smallLabel(t: String): TextView {
        val tv = TextView(act)
        tv.text = t
        tv.textSize = 13f
        tv.letterSpacing = 0.2f
        tv.setTextColor(F_DIM)
        tv.gravity = Gravity.CENTER
        tv.setPadding(0, 0, 0, dp(16))
        return tv
    }

    private fun circle(glyph: Int, onClick: () -> Unit): View {
        val g = GlyphView(act, glyph)
        g.tint = F_CARD
        g.setPadding(dp(14), dp(14), dp(14), dp(14))
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.OVAL
        bg.setColor(Color.WHITE)
        g.background = bg
        g.setOnClickListener { onClick() }
        return g
    }

    // Sound tile pe long-press: music app add / remove
    private fun soundMenu(reload: () -> Unit) {
        val have = Store.soundApps(act)
        val opts = ArrayList<String>()
        opts.add("Add music app (Spotify, YT Music...)")
        if (have.isNotEmpty()) opts.add("Remove app")
        AlertDialog.Builder(act)
            .setTitle("Sound tile")
            .setItems(opts.toTypedArray()) { _, i ->
                if (i == 0) pickSoundApp(reload) else removeSoundApp(reload)
            }
            .show()
    }

    private fun pickSoundApp(reload: () -> Unit) {
        val pm = act.packageManager
        val have = Store.soundApps(act)
        val known = setOf(
            "com.spotify.music", "com.google.android.apps.youtube.music", "com.jio.media.jiobeats",
            "com.bsbportal.music", "com.gaana", "com.amazon.mp3", "com.apple.android.music", "com.soundcloud.android"
        )
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(q, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { it.first != act.packageName && it.first !in have }
            .distinctBy { it.first }
            .sortedWith(compareBy({ it.first !in known }, { it.second.lowercase() }))
        if (apps.isEmpty()) {
            Toast.makeText(act, "Koi app nahi mila", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(act)
            .setTitle("Add music app")
            .setItems(apps.map { it.second }.toTypedArray()) { _, w ->
                Store.addSoundApp(act, apps[w].first)
                reload()
            }
            .show()
    }

    private fun removeSoundApp(reload: () -> Unit) {
        val pm = act.packageManager
        val have = Store.soundApps(act)
        val names = have.map {
            try { pm.getApplicationLabel(pm.getApplicationInfo(it, 0)).toString() } catch (e: Exception) { it }
        }
        AlertDialog.Builder(act)
            .setTitle("Remove app")
            .setItems(names.toTypedArray()) { _, w ->
                Store.removeSoundApp(act, have[w])
                reload()
            }
            .show()
    }

    // Music ka track + controls dikhane ke liye Notification access chahiye (session se pehle on karo)
    private fun openAccess() {
        Toast.makeText(act, "FocusLock ko Notification access do, phir wapas aao", Toast.LENGTH_LONG).show()
        try {
            act.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } catch (e: Exception) {
        }
    }

    private fun launch(pkg: String) {
        try {
            val i = act.packageManager.getLaunchIntentForPackage(pkg)
            if (i != null) act.startActivity(i)
        } catch (e: Exception) {
        }
    }

    private fun build(): View {
        val dm = act.resources.displayMetrics
        val wide = dm.widthPixels > dm.heightPixels

        val root = LinearLayout(act)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.BLACK)
        root.setPadding(dp(14), dp(26), dp(14), dp(14))

        val title = TextView(act)
        val tsp = SpannableStringBuilder("FocusLock")
        tsp.setSpan(ForegroundColorSpan(C_ORANGE), 5, 9, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        title.text = tsp
        title.textSize = 24f
        title.setTextColor(Color.WHITE)
        title.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        title.gravity = Gravity.CENTER
        val header = FrameLayout(act)
        header.addView(title, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(header, LinearLayout.LayoutParams(MATCH, dp(52)))

        // left: big countdown (upar) + manual stopwatch (neeche)
        val left = LinearLayout(act)
        left.orientation = LinearLayout.VERTICAL
        left.background = cardBg(28)
        left.setPadding(dp(16), dp(16), dp(16), dp(16))
        clock = PixelClockView(act)
        left.addView(clock, LinearLayout.LayoutParams(MATCH, 0, 1f))

        val swBox = LinearLayout(act)
        swBox.orientation = LinearLayout.VERTICAL
        swBox.gravity = Gravity.CENTER
        swBox.addView(smallLabel("STOPWATCH"), LinearLayout.LayoutParams(MATCH, WRAP))
        timerTv = TextView(act)
        timerTv.textSize = 52f
        timerTv.setTextColor(Color.WHITE)
        timerTv.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        timerTv.gravity = Gravity.CENTER
        timerTv.maxLines = 1
        timerTv.setAutoSizeTextTypeUniformWithConfiguration(18, 52, 2, android.util.TypedValue.COMPLEX_UNIT_SP)
        timerTv.text = swText()
        swBox.addView(timerTv, LinearLayout.LayoutParams(MATCH, WRAP))

        // lap list (last 2 laps): lap time + total
        lapTv = TextView(act)
        lapTv.textSize = 12f
        lapTv.setTextColor(F_DIM)
        lapTv.typeface = Typeface.MONOSPACE
        lapTv.gravity = Gravity.CENTER
        lapTv.minLines = 2
        lapTv.maxLines = 2
        val lapLp = LinearLayout.LayoutParams(MATCH, WRAP)
        lapLp.topMargin = dp(4)
        swBox.addView(lapTv, lapLp)

        val swRow = LinearLayout(act)
        swRow.orientation = LinearLayout.HORIZONTAL
        swRow.gravity = Gravity.CENTER
        val swReset = IconView(act, IconView.RESET)
        swReset.tint = Color.WHITE
        swReset.setPadding(dp(14), dp(14), dp(14), dp(14))
        swReset.background = cardBg(40)
        swReset.setOnClickListener {
            Store.swReset(act)
            swLaps.clear()
            updateLaps()
            update()
        }
        swBtn = IconView(act, if (Store.swRunning(act)) IconView.PAUSE else IconView.PLAY)
        swBtn.tint = Color.WHITE
        swBtn.setPadding(dp(14), dp(10), dp(14), dp(10))
        val pill = GradientDrawable()
        pill.setColor(C_ORANGE)
        pill.cornerRadius = dp(40).toFloat()
        swBtn.background = pill
        swBtn.setOnClickListener {
            Store.swToggle(act)
            update()
        }
        lapBtn = TextView(act)
        lapBtn.text = "LAP"
        lapBtn.textSize = 12f
        lapBtn.letterSpacing = 0.1f
        lapBtn.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        lapBtn.setTextColor(Color.WHITE)
        lapBtn.gravity = Gravity.CENTER
        lapBtn.background = cardBg(40)
        lapBtn.alpha = if (Store.swRunning(act)) 1f else 0.4f
        lapBtn.setOnClickListener {
            if (Store.swRunning(act)) {
                swLaps.add(Store.swElapsed(act))
                updateLaps()
            }
        }
        swRow.addView(swReset, LinearLayout.LayoutParams(dp(56), dp(56)))
        val pl = LinearLayout.LayoutParams(dp(110), dp(56))
        pl.leftMargin = dp(12)
        pl.rightMargin = dp(12)
        swRow.addView(swBtn, pl)
        swRow.addView(lapBtn, LinearLayout.LayoutParams(dp(56), dp(56)))
        val rl = LinearLayout.LayoutParams(WRAP, WRAP)
        rl.topMargin = dp(6)
        swBox.addView(swRow, rl)
        left.addView(swBox, LinearLayout.LayoutParams(MATCH, 0, 1f))

        // SEC tile
        val secCard = LinearLayout(act)
        secCard.orientation = LinearLayout.VERTICAL
        secCard.background = cardBg()
        secCard.gravity = Gravity.CENTER
        secView = PixelNumberView(act)
        secCard.addView(secView, LinearLayout.LayoutParams(MATCH, 0, 1f))
        secCard.addView(smallLabel("SEC"), LinearLayout.LayoutParams(MATCH, WRAP))

        // +15 HOLD tile
        val holdCard = FrameLayout(act)
        holdCard.background = cardBg()
        val hold = HoldView(act) {
            Store.extendSession(act, 15L)
            Toast.makeText(act, "+15 minutes", Toast.LENGTH_SHORT).show()
            update()
        }
        holdCard.addView(hold, FrameLayout.LayoutParams(MATCH, MATCH))

        // ENDS tile
        endsTv = TextView(act)
        endsTv.gravity = Gravity.CENTER
        endsTv.background = cardBg(22)

        // parachute tile
        val chuteCard = LinearLayout(act)
        chuteCard.orientation = LinearLayout.HORIZONTAL
        chuteCard.gravity = Gravity.CENTER
        chuteCard.background = cardBg(22)
        val chuteIcon = circle(GlyphView.CHUTE) {}
        chuteIcon.setPadding(dp(10), dp(10), dp(10), dp(10))
        chuteTv = TextView(act)
        chuteTv.textSize = 26f
        chuteTv.setTextColor(Color.WHITE)
        chuteTv.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        chuteTv.setPadding(dp(12), 0, 0, 0)
        chuteCard.addView(chuteIcon, LinearLayout.LayoutParams(dp(44), dp(44)))
        chuteCard.addView(chuteTv, LinearLayout.LayoutParams(WRAP, WRAP))

        // rain tile
        val rainCard = FrameLayout(act)
        rainCard.background = cardBg()
        rainCard.clipToOutline = true
        val rainLabel = smallLabel("TAP TO PLAY")
        lateinit var soundView: RainView
        soundView = RainView(act, { rainLabel.text = it }, { launch(it) }, { soundMenu { soundView.reload() } }, { openAccess() })
        rainCard.addView(soundView, FrameLayout.LayoutParams(MATCH, MATCH))
        rainCard.addView(rainLabel, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM))

        val slide = ExitSlideView(act) { confirmExit() }

        val row1 = LinearLayout(act)
        row1.orientation = LinearLayout.HORIZONTAL
        row1.addView(secCard, lp(0, MATCH, 1f))
        row1.addView(holdCard, lp(0, MATCH, 1f))

        // col2 ke tiles ke kone row1 / rain tile ke saath barabar rahein (pehle 7px ka farq tha)
        val col2 = LinearLayout(act)
        col2.orientation = LinearLayout.VERTICAL
        val endsLp = LinearLayout.LayoutParams(MATCH, 0, 1f)
        endsLp.setMargins(0, dp(5), 0, dp(5))
        col2.addView(endsTv, endsLp)
        val chuteLp = LinearLayout.LayoutParams(MATCH, 0, 1f)
        chuteLp.setMargins(0, dp(5), 0, dp(5))
        col2.addView(chuteCard, chuteLp)

        val row2 = LinearLayout(act)
        row2.orientation = LinearLayout.HORIZONTAL
        val col2Lp = LinearLayout.LayoutParams(0, MATCH, 1f)
        col2Lp.setMargins(dp(5), 0, dp(5), 0)
        row2.addView(col2, col2Lp)
        row2.addView(rainCard, lp(0, MATCH, 1f))

        val grid = LinearLayout(act)
        grid.orientation = LinearLayout.VERTICAL
        grid.addView(row1, lp(MATCH, 0, 1.5f, 0))
        grid.addView(row2, lp(MATCH, 0, 1.35f, 0))
        grid.addView(slide, lp(MATCH, dp(64), 0f))

        val rail = buildRail(wide)

        val body = LinearLayout(act)
        body.orientation = if (wide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        if (wide) {
            body.addView(left, lp(0, MATCH, 1f, 6))
            body.addView(grid, lp(0, MATCH, 1.15f, 1))
            val rl = LinearLayout.LayoutParams(WRAP, WRAP)
            rl.gravity = Gravity.CENTER_VERTICAL
            rl.setMargins(dp(6), dp(6), dp(6), dp(6))
            body.addView(rail, rl)
        } else {
            body.addView(left, lp(MATCH, 0, 0.9f, 6))
            body.addView(grid, lp(MATCH, 0, 1.5f, 6))
            body.addView(rail, lp(MATCH, WRAP, 0f, 6))
        }
        root.addView(body, LinearLayout.LayoutParams(MATCH, 0, 1f))

        // entry animation
        val items = listOf<View>(left, secCard, holdCard, endsTv, chuteCard, rainCard, slide, rail)
        for ((i, v) in items.withIndex()) {
            v.alpha = 0f
            v.translationY = dp(26).toFloat()
        }
        root.post {
            for ((i, v) in items.withIndex()) {
                v.animate().alpha(1f).translationY(0f).setStartDelay(i * 70L).setDuration(420L).start()
            }
        }
        return root
    }

    // right side: sirf bag ke 3-4 apps + phone + camera (hamesha chalenge)
    private fun buildRail(wide: Boolean): View {
        val orient = if (wide) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        val r = LinearLayout(act)
        r.orientation = orient
        r.gravity = Gravity.CENTER

        val pkgs: List<String> = if (Store.allowMode(act)) Store.selected(act).toList().sorted().take(4) else emptyList()
        if (pkgs.isNotEmpty()) {
            val apps = LinearLayout(act)
            apps.orientation = orient
            apps.gravity = Gravity.CENTER
            apps.background = cardBg(30)
            apps.setPadding(dp(6), dp(6), dp(6), dp(6))
            for (p in pkgs) {
                val iv = ImageView(act)
                try {
                    iv.setImageDrawable(act.packageManager.getApplicationIcon(p))
                } catch (e: Exception) {
                }
                iv.setOnClickListener { launch(p) }
                apps.addView(iv, lp(dp(44), dp(44), 0f, 6))
            }
            r.addView(apps, LinearLayout.LayoutParams(WRAP, WRAP))
        }

        val phone = circle(GlyphView.PHONE) {
            try {
                act.startActivity(Intent(Intent.ACTION_DIAL))
            } catch (e: Exception) {
            }
        }
        val camera = circle(GlyphView.CAMERA) {
            try {
                act.startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            } catch (e: Exception) {
            }
        }
        r.addView(phone, lp(dp(52), dp(52), 0f, 6))
        r.addView(camera, lp(dp(52), dp(52), 0f, 6))
        return r
    }

    private fun endsText(until: Long): CharSequence {
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(until)).lowercase()
        val sb = SpannableStringBuilder("ENDS  ")
        sb.setSpan(ForegroundColorSpan(F_DIM), 0, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(AbsoluteSizeSpan(13, true), 0, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val st = sb.length
        sb.append(time)
        sb.setSpan(ForegroundColorSpan(Color.WHITE), st, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(AbsoluteSizeSpan(26, true), st, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
    }

    fun update() {
        val now = System.currentTimeMillis()
        val until = Store.until(act)
        val remSec = ((until - now) / 1000L).coerceAtLeast(0L)
        clock.show((remSec / 60L).toInt(), Color.WHITE)
        timerTv.text = swText()
        val swOn = Store.swRunning(act)
        swHandler.removeCallbacks(swTick)
        if (swOn) swHandler.post(swTick)
        if (swOn != lastSwRunning) {
            lastSwRunning = swOn
            lapBtn.alpha = if (swOn) 1f else 0.4f
            swBtn.kind = if (swOn) IconView.PAUSE else IconView.PLAY
            swBtn.invalidate()
        }

        val sec = (remSec % 60L).toInt()
        if (sec != lastSec) {
            lastSec = sec
            secView.setText(String.format("%02d", sec))
            secView.scaleX = 0.9f
            secView.scaleY = 0.9f
            secView.animate().scaleX(1f).scaleY(1f).setDuration(220L).start()
        }
        if (until != lastUntil) {
            lastUntil = until
            endsTv.text = endsText(until)
        }
        val c = Store.chutes(act)
        if (c != lastChutes) {
            lastChutes = c
            chuteTv.text = c.toString()
        }
    }

    private fun confirmExit() {
        if (!Store.strict(act)) {
            Store.stopSession(act)
            onExit()
            return
        }
        if (Store.chutes(act) <= 0) {
            Toast.makeText(act, "Strict mode: parachute nahi bacha, timer khatam hone tak ruk", Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(act)
            .setTitle("Parachute use karna hai?")
            .setMessage("Strict session abhi khatam ho jayega. Aaj ka parachute sirf 1 hai, kal naya milega.")
            .setPositiveButton("Haan, exit") { _, _ ->
                if (Store.useChute(act)) {
                    Store.stopSession(act)
                    onExit()
                }
            }
            .setNegativeButton("Nahi", null)
            .show()
    }
}
