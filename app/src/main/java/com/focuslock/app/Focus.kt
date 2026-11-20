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
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
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

// ---------- Rain animation tile (tap = on/off) ----------

class RainView(ctx: Context) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var t = 0f
    private var on = true
    private var anim: ValueAnimator? = null
    private val dens = ctx.resources.displayMetrics.density
    private val xs = FloatArray(30) { (it * 37 % 30) / 30f }
    private val sp = FloatArray(30) { 0.6f + (it * 13 % 7) / 7f * 0.8f }
    private val ln = FloatArray(30) { 0.05f + (it * 5 % 4) * 0.03f }

    init {
        setOnClickListener {
            on = !on
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 1400L
        a.repeatCount = ValueAnimator.INFINITE
        a.interpolator = LinearInterpolator()
        a.addUpdateListener {
            t = it.animatedValue as Float
            invalidate()
        }
        a.start()
        anim = a
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!on) return
        val w = width.toFloat()
        val h = height.toFloat()
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = dens * 1.6f
        paint.color = Color.parseColor("#8A8A8A")
        for (i in xs.indices) {
            val prog = (t * sp[i] + i * 0.137f) % 1f
            val y = prog * (h * 1.2f) - h * 0.1f
            val x = xs[i] * w
            val l = ln[i] * h
            canvas.drawLine(x, y, x - l * 0.2f, y + l, paint)
        }
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
        root.addView(title, LinearLayout.LayoutParams(MATCH, dp(52)))

        // left: big countdown
        val left = FrameLayout(act)
        left.background = cardBg(28)
        left.setPadding(dp(16), dp(16), dp(16), dp(16))
        clock = PixelClockView(act)
        left.addView(clock, FrameLayout.LayoutParams(MATCH, MATCH))

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
        rainCard.addView(RainView(act), FrameLayout.LayoutParams(MATCH, MATCH))
        val rainLabel = smallLabel("RAIN")
        rainCard.addView(rainLabel, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM))

        val slide = ExitSlideView(act) { confirmExit() }

        val row1 = LinearLayout(act)
        row1.orientation = LinearLayout.HORIZONTAL
        row1.addView(secCard, lp(0, MATCH, 1f))
        row1.addView(holdCard, lp(0, MATCH, 1f))

        val col2 = LinearLayout(act)
        col2.orientation = LinearLayout.VERTICAL
        col2.addView(endsTv, lp(MATCH, 0, 1f))
        col2.addView(chuteCard, lp(MATCH, 0, 1f))

        val row2 = LinearLayout(act)
        row2.orientation = LinearLayout.HORIZONTAL
        row2.addView(col2, lp(0, MATCH, 1f, 0))
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
            body.addView(grid, lp(0, MATCH, 1.15f, 6))
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
