package com.focuslock.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private class App(val label: String, val pkg: String)

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private var all: List<App> = emptyList()
    private var shown: List<App> = emptyList()
    private val sel = HashSet<String>()
    private val ui = Handler(Looper.getMainLooper())
    private val lockables = ArrayList<View>()

    private lateinit var pageTimer: View
    private lateinit var pageApps: View
    private lateinit var status: TextView
    private lateinit var accStatus: TextView
    private lateinit var accBtn: Button
    private lateinit var etH: EditText
    private lateinit var etM: EditText
    private lateinit var cbStrict: CheckBox
    private lateinit var btnStart: Button
    private lateinit var btnEnd: Button
    private lateinit var btnS: Button
    private lateinit var btnE: Button
    private lateinit var count: TextView
    private lateinit var adapter: BaseAdapter

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_LONG).show()
    private fun fmt(min: Int) = String.format("%02d:%02d", min / 60, min % 60)

    private fun label(t: String) = TextView(this).apply {
        text = t
        textSize = 16f
        setPadding(0, dp(14), 0, dp(4))
    }

    private fun smallBtn(t: String, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = t
        b.setOnClickListener { onClick() }
        return b
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sel.addAll(Store.selected(this))
        loadApps()

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(12), dp(36), dp(12), dp(8))

        val tabs = LinearLayout(this)
        tabs.orientation = LinearLayout.HORIZONTAL
        val tTimer = smallBtn("Timer") { showPage(true) }
        val tApps = smallBtn("Apps") { showPage(false) }
        tabs.addView(tTimer, LinearLayout.LayoutParams(0, WRAP, 1f))
        tabs.addView(tApps, LinearLayout.LayoutParams(0, WRAP, 1f))
        root.addView(tabs)

        pageTimer = buildTimerPage()
        pageApps = buildAppsPage()
        root.addView(pageTimer, LinearLayout.LayoutParams(MATCH, 0, 1f))
        root.addView(pageApps, LinearLayout.LayoutParams(MATCH, 0, 1f))
        pageApps.visibility = View.GONE
        setContentView(root)
    }

    private fun showPage(timer: Boolean) {
        pageTimer.visibility = if (timer) View.VISIBLE else View.GONE
        pageApps.visibility = if (timer) View.GONE else View.VISIBLE
    }

    private fun loadApps() {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        all = packageManager.queryIntentActivities(i, 0)
            .filter { it.activityInfo.packageName != packageName }
            .map { App(it.loadLabel(packageManager).toString(), it.activityInfo.packageName) }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
        shown = all
    }

    // ---------------- Timer page ----------------
    private fun buildTimerPage(): View {
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(4), dp(8), dp(4), dp(24))

        status = TextView(this)
        status.textSize = 30f
        status.setPadding(0, dp(10), 0, dp(6))
        col.addView(status)

        accStatus = TextView(this)
        col.addView(accStatus)
        accBtn = smallBtn("Accessibility ON kar") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        col.addView(accBtn)

        col.addView(label("Mode"))
        val rg = RadioGroup(this)
        val rbBlock = RadioButton(this)
        rbBlock.id = View.generateViewId()
        rbBlock.text = "Selected apps BLOCK karo"
        val rbAllow = RadioButton(this)
        rbAllow.id = View.generateViewId()
        rbAllow.text = "Sirf selected apps ALLOW karo (baaki sab block)"
        rg.addView(rbBlock)
        rg.addView(rbAllow)
        if (Store.allowMode(this)) rbAllow.isChecked = true else rbBlock.isChecked = true
        rg.setOnCheckedChangeListener { _, id -> Store.setAllowMode(this, id == rbAllow.id) }
        col.addView(rg)
        lockables.add(rbBlock)
        lockables.add(rbAllow)

        col.addView(label("Kitni der? (max 24 ghante)"))
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        etH = EditText(this)
        etH.hint = "ghante"
        etH.inputType = InputType.TYPE_CLASS_NUMBER
        etH.setText("4")
        etM = EditText(this)
        etM.hint = "minute"
        etM.inputType = InputType.TYPE_CLASS_NUMBER
        etM.setText("0")
        row.addView(etH, LinearLayout.LayoutParams(0, WRAP, 1f))
        row.addView(etM, LinearLayout.LayoutParams(0, WRAP, 1f))
        col.addView(row)
        lockables.add(etH)
        lockables.add(etM)

        val quick = LinearLayout(this)
        quick.orientation = LinearLayout.HORIZONTAL
        for (h in intArrayOf(1, 3, 6, 12)) {
            val b = smallBtn("${h}h") {
                etH.setText(h.toString())
                etM.setText("0")
            }
            quick.addView(b, LinearLayout.LayoutParams(0, WRAP, 1f))
            lockables.add(b)
        }
        col.addView(quick)

        cbStrict = CheckBox(this)
        cbStrict.text = "Strict mode (End button band + Settings block)"
        cbStrict.isChecked = Store.strict(this)
        cbStrict.setOnCheckedChangeListener { _, c -> Store.setStrict(this, c) }
        col.addView(cbStrict)

        btnStart = smallBtn("START FOCUS") { onStart() }
        col.addView(btnStart)
        lockables.add(btnStart)
        btnEnd = smallBtn("End session") {
            Store.stopSession(this)
            refreshState()
        }
        col.addView(btnEnd)

        col.addView(label("Daily schedule (roz repeat)"))
        val cbSched = CheckBox(this)
        cbSched.text = "Schedule ON"
        cbSched.isChecked = Store.schedOn(this)
        cbSched.setOnCheckedChangeListener { _, c -> Store.setSchedOn(this, c) }
        col.addView(cbSched)
        lockables.add(cbSched)
        btnS = smallBtn("") { pickTime(true) }
        btnE = smallBtn("") { pickTime(false) }
        col.addView(btnS)
        col.addView(btnE)
        lockables.add(btnS)
        lockables.add(btnE)
        updateSchedLabels()

        val hint = TextView(this)
        hint.textSize = 12f
        hint.setPadding(0, dp(16), 0, 0)
        hint.text = "Dhyan: Strict mode bhi 100% tamper-proof nahi hai (reboot, uninstall, phone ka time badalna). " +
            "Xiaomi/Vivo/Oppo me battery optimization off rakh, warna service band ho sakti hai."
        col.addView(hint)

        val sv = ScrollView(this)
        sv.addView(col)
        return sv
    }

    private fun updateSchedLabels() {
        btnS.text = "Start time: " + fmt(Store.schedStart(this))
        btnE.text = "End time: " + fmt(Store.schedEnd(this))
    }

    private fun pickTime(start: Boolean) {
        val cur = if (start) Store.schedStart(this) else Store.schedEnd(this)
        TimePickerDialog(this, { _, h, m ->
            if (start) Store.setSchedStart(this, h * 60 + m) else Store.setSchedEnd(this, h * 60 + m)
            updateSchedLabels()
        }, cur / 60, cur % 60, true).show()
    }

    private fun onStart() {
        val h = etH.text.toString().toLongOrNull() ?: 0L
        val m = etM.text.toString().toLongOrNull() ?: 0L
        val total = h * 60L + m
        if (total < 1L || total > 1440L) {
            toast("Time 1 minute se 24 ghante ke beech rakh")
            return
        }
        if (sel.isEmpty()) {
            toast("Pehle Apps tab me apps select kar")
            return
        }
        if (!accEnabled()) {
            toast("Pehle Accessibility ON kar")
            return
        }
        val go = {
            Store.startSession(this, total)
            refreshState()
        }
        if (cbStrict.isChecked) {
            AlertDialog.Builder(this)
                .setTitle("Strict mode")
                .setMessage("${total / 60}h ${total % 60}m tak End nahi kar paega aur Settings bhi block rahegi. Pakka?")
                .setPositiveButton("Haan") { _, _ -> go() }
                .setNegativeButton("Nahi", null)
                .show()
        } else {
            go()
        }
    }

    // ---------------- Apps page ----------------
    private fun buildAppsPage(): View {
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL

        val search = EditText(this)
        search.hint = "Search app"
        search.isSingleLine = true
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
        col.addView(search)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val bAll = smallBtn("Sab select (jo dikh rahe)") {
            shown.forEach { sel.add(it.pkg) }
            saveSel()
        }
        val bNone = smallBtn("Sab hatao") {
            sel.clear()
            saveSel()
        }
        row.addView(bAll, LinearLayout.LayoutParams(0, WRAP, 1f))
        row.addView(bNone, LinearLayout.LayoutParams(0, WRAP, 1f))
        col.addView(row)
        lockables.add(bAll)
        lockables.add(bNone)

        count = TextView(this)
        count.text = "${sel.size} selected"
        col.addView(count)

        val listView = ListView(this)
        adapter = object : BaseAdapter() {
            override fun getCount() = shown.size
            override fun getItem(i: Int): Any = shown[i]
            override fun getItemId(i: Int) = i.toLong()
            override fun getView(i: Int, v: View?, parent: ViewGroup?): View {
                val cb = (v as? CheckBox) ?: CheckBox(this@MainActivity).apply {
                    isClickable = false
                    isFocusable = false
                    textSize = 16f
                    setPadding(dp(8), dp(10), dp(8), dp(10))
                }
                val a = shown[i]
                cb.text = a.label
                cb.isChecked = a.pkg in sel
                return cb
            }
        }
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, i, _ ->
            if (!(Store.active(this) && Store.strict(this))) {
                val p = shown[i].pkg
                if (!sel.remove(p)) sel.add(p)
                saveSel()
            }
        }
        col.addView(listView, LinearLayout.LayoutParams(MATCH, 0, 1f))
        return col
    }

    private fun applyFilter(q: String) {
        shown = if (q.isBlank()) all else all.filter { it.label.contains(q, ignoreCase = true) }
        adapter.notifyDataSetChanged()
    }

    private fun saveSel() {
        Store.setSelected(this, sel)
        count.text = "${sel.size} selected"
        adapter.notifyDataSetChanged()
    }

    // ---------------- State refresh ----------------
    private fun accEnabled(): Boolean {
        val am = getSystemService(AccessibilityManager::class.java)
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    private fun refreshState() {
        val now = System.currentTimeMillis()
        val until = Store.until(this)
        val sessionOn = now < until
        val locked = Store.active(this) && Store.strict(this)

        status.text = when {
            sessionOn -> {
                val s = (until - now) / 1000
                String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
            }
            Store.inSchedule(this) -> "Schedule ON"
            else -> "Ready"
        } + if (locked) "\nSTRICT" else ""

        val acc = accEnabled()
        accStatus.text = if (acc) "Accessibility: ON" else "Accessibility: OFF (blocking nahi chalegi)"
        accBtn.visibility = if (acc) View.GONE else View.VISIBLE

        lockables.forEach { it.isEnabled = !locked }
        cbStrict.isEnabled = !Store.active(this)
        btnStart.isEnabled = !locked && !sessionOn
        btnEnd.visibility = if (sessionOn && !Store.strict(this)) View.VISIBLE else View.GONE
    }

    private val tick = object : Runnable {
        override fun run() {
            refreshState()
            ui.postDelayed(this, 1000)
        }
    }

    override fun onResume() {
        super.onResume()
        tick.run()
    }

    override fun onPause() {
        super.onPause()
        ui.removeCallbacks(tick)
    }
}
