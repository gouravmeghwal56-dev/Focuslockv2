package com.focuslock.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

private val D_CARD = Color.parseColor("#1D1D1D")
private val D_DIM = Color.parseColor("#7C7C7C")
private val D_OFF = Color.parseColor("#3A3634")

/** Big pixel word (DONE) that assembles itself from scattered blocks. */
class PixelWordView(ctx: Context, private val word: String) : AssembleView(ctx) {
    private val font = mapOf(
        'D' to "110,101,101,101,110",
        'O' to "111,101,101,101,111",
        'N' to "101,111,111,101,101",
        'E' to "111,100,110,100,111"
    )
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val seam = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        strokeWidth = 2f
    }

    override fun drawContent(canvas: Canvas) {
        val n = word.length
        val cols = n * 3 + (n - 1)
        val cell = minOf(width * 0.94f / cols, height * 0.9f / 5f)
        val x0 = (width - cell * cols) / 2f
        val y0 = (height - cell * 5f) / 2f
        paint.color = Color.WHITE
        for ((i, ch) in word.withIndex()) {
            val g = (font[ch] ?: "111,111,111,111,111").split(",")
            val lx = x0 + i * 4 * cell
            for (r in 0 until 5) for (q in 0 until 3) {
                if (g[r][q] == '1') {
                    val l = lx + q * cell
                    val t = y0 + r * cell
                    canvas.drawRect(l, t, l + cell, t + cell, paint)
                }
            }
            for (k in 1..2) canvas.drawLine(lx + k * cell, y0, lx + k * cell, y0 + 5 * cell, seam)
            for (k in 1..4) canvas.drawLine(lx, y0 + k * cell, lx + 3 * cell, y0 + k * cell, seam)
        }
    }
}

/** M T W T F S S squares (orange = focused that day), today's letter highlighted. */
class WeekView(ctx: Context, private val mins: IntArray, private val todayIdx: Int) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val letters = "MTWTFSS"
    private val dens = ctx.resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val pitch = w / 7f
        val s = minOf(pitch * 0.72f, dens * 34f)
        val textSize = dens * 14f
        val totalH = s + dens * 12f + textSize
        val y = (h - totalH) / 2f
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = textSize
        for (i in 0 until 7) {
            val cx = pitch * i + pitch / 2f
            paint.style = Paint.Style.FILL
            paint.color = if (mins[i] > 0) C_ORANGE else D_OFF
            rect.set(cx - s / 2f, y, cx + s / 2f, y + s)
            canvas.drawRoundRect(rect, s * 0.08f, s * 0.08f, paint)
            val today = i == todayIdx
            paint.color = if (today) Color.WHITE else D_DIM
            paint.typeface = Typeface.create("sans-serif", if (today) Typeface.BOLD else Typeface.NORMAL)
            canvas.drawText(letters[i].toString(), cx, y + s + dens * 12f + textSize * 0.8f, paint)
        }
    }
}

/** Round share glyph (3 dots + 2 lines). */
class ShareGlyph(ctx: Context) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val u = minOf(w, h) / 24f
        val cx = w / 2f
        val cy = h / 2f
        val ax = cx - 5f * u
        val ay = cy
        val bx = cx + 5f * u
        val by1 = cy - 5.5f * u
        val by2 = cy + 5.5f * u
        paint.color = C_DARK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.6f * u
        canvas.drawLine(ax, ay, bx, by1, paint)
        canvas.drawLine(ax, ay, bx, by2, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(ax, ay, 2.6f * u, paint)
        canvas.drawCircle(bx, by1, 2.6f * u, paint)
        canvas.drawCircle(bx, by2, 2.6f * u, paint)
    }
}

/**
 * Session complete screen (blockit style): pixel DONE, session / today minutes,
 * day streak with the week row, and a check button to close.
 */
class DoneScreen(
    private val act: Activity,
    private val sessionMin: Int,
    private val todayMin: Int,
    private val streak: Int,
    private val weekMins: IntArray,
    private val todayIdx: Int,
    private val onClose: () -> Unit
) {
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private fun dp(v: Int) = (v * act.resources.displayMetrics.density).toInt()

    private fun bg(r: Int, c: Int = D_CARD): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(c)
        g.cornerRadius = dp(r).toFloat()
        return g
    }

    private fun lp(w: Int, h: Int, wt: Float, m: Int = 5): LinearLayout.LayoutParams {
        val l = LinearLayout.LayoutParams(w, h, wt)
        l.setMargins(dp(m), dp(m), dp(m), dp(m))
        return l
    }

    private fun statTile(num: Int, label: String): View {
        val t = LinearLayout(act)
        t.orientation = LinearLayout.VERTICAL
        t.gravity = Gravity.CENTER
        t.background = bg(26)
        val n = TextView(act)
        n.text = num.toString()
        n.textSize = 40f
        n.setTextColor(Color.WHITE)
        n.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        n.gravity = Gravity.CENTER
        val l = TextView(act)
        l.text = label
        l.textSize = 14f
        l.letterSpacing = 0.18f
        l.setTextColor(D_DIM)
        l.gravity = Gravity.CENTER
        t.addView(n, LinearLayout.LayoutParams(WRAP, WRAP))
        t.addView(l, LinearLayout.LayoutParams(WRAP, WRAP))
        return t
    }

    val view: View = build()

    private fun build(): View {
        val wide = act.resources.displayMetrics.let { it.widthPixels > it.heightPixels }

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

        // left: pixel DONE
        val left = FrameLayout(act)
        left.background = bg(28)
        val word = PixelWordView(act, "DONE")
        left.addView(word, FrameLayout.LayoutParams(MATCH, MATCH))

        // right grid
        val t1 = statTile(sessionMin, "MIN · SESSION")
        val t2 = statTile(todayMin, "MIN · TODAY")
        val row1 = LinearLayout(act)
        row1.orientation = LinearLayout.HORIZONTAL
        row1.addView(t1, lp(0, MATCH, 1f))
        row1.addView(t2, lp(0, MATCH, 1f))

        val streakCard = LinearLayout(act)
        streakCard.orientation = LinearLayout.HORIZONTAL
        streakCard.gravity = Gravity.CENTER_VERTICAL
        streakCard.background = bg(26)
        val sc = LinearLayout(act)
        sc.orientation = LinearLayout.VERTICAL
        sc.gravity = Gravity.CENTER
        val sn = TextView(act)
        sn.text = streak.toString()
        sn.textSize = 64f
        sn.setTextColor(C_ORANGE)
        sn.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        sn.gravity = Gravity.CENTER
        val sl = TextView(act)
        sl.text = "DAY STREAK"
        sl.textSize = 14f
        sl.letterSpacing = 0.08f
        sl.setTextColor(D_DIM)
        sl.gravity = Gravity.CENTER
        sc.addView(sn, LinearLayout.LayoutParams(WRAP, WRAP))
        sc.addView(sl, LinearLayout.LayoutParams(WRAP, WRAP))
        streakCard.addView(sc, LinearLayout.LayoutParams(0, MATCH, 1f))
        val wl = LinearLayout.LayoutParams(0, MATCH, 1.25f)
        wl.setMargins(0, 0, dp(14), 0)
        streakCard.addView(WeekView(act, weekMins, todayIdx), wl)

        val check = TextView(act)
        check.text = "✓"
        check.textSize = 30f
        check.setTextColor(C_DARK)
        check.gravity = Gravity.CENTER
        check.background = bg(34, Color.WHITE)
        check.setOnClickListener { onClose() }

        val grid = LinearLayout(act)
        grid.orientation = LinearLayout.VERTICAL
        grid.addView(row1, lp(MATCH, 0, 1f, 0))
        grid.addView(streakCard, lp(MATCH, 0, 1f, 5))
        grid.addView(check, lp(MATCH, dp(64), 0f))

        // share
        val share = FrameLayout(act)
        share.background = bg(40, Color.WHITE)
        share.addView(ShareGlyph(act), FrameLayout.LayoutParams(MATCH, MATCH))
        share.setOnClickListener {
            try {
                val i = Intent(Intent.ACTION_SEND)
                i.type = "text/plain"
                i.putExtra(Intent.EXTRA_TEXT, "Focused for $sessionMin min today with FocusLock · $streak day streak")
                act.startActivity(Intent.createChooser(i, "Share"))
            } catch (e: Exception) {
            }
        }

        val body = LinearLayout(act)
        body.orientation = if (wide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        if (wide) {
            body.addView(left, lp(0, MATCH, 1f, 6))
            body.addView(grid, lp(0, MATCH, 1.15f, 1))
            val sl2 = LinearLayout.LayoutParams(dp(52), dp(52))
            sl2.gravity = Gravity.CENTER_VERTICAL
            sl2.setMargins(dp(6), dp(6), dp(6), dp(6))
            body.addView(share, sl2)
        } else {
            body.addView(left, lp(MATCH, 0, 0.9f, 6))
            body.addView(grid, lp(MATCH, 0, 1.5f, 6))
        }
        root.addView(body, LinearLayout.LayoutParams(MATCH, 0, 1f))

        // entry animation (same stagger as the focus screen) + DONE assembles from blocks
        val items = listOf<View>(left, t1, t2, streakCard, check, share)
        for (v in items) {
            v.alpha = 0f
            v.translationY = dp(26).toFloat()
        }
        root.post {
            for ((i, v) in items.withIndex()) {
                v.animate().alpha(1f).translationY(0f).setStartDelay(i * 80L).setDuration(420L).start()
            }
            word.assemble(1000L)
        }
        return root
    }
}
