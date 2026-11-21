package com.focuslock.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Saari notifications yahin se: session chal raha hai + accessibility/permission chahiye. */
object Notif {
    private const val CH_SESSION = "session"
    private const val CH_SETUP = "setup"
    private const val ID_SESSION = 11
    private const val ID_SETUP = 12
    private var postedUntil = 0L

    private fun nm(c: Context) = c.getSystemService(NotificationManager::class.java)

    private fun channels(c: Context) {
        val m = nm(c)
        if (m.getNotificationChannel(CH_SESSION) == null) {
            val ch = NotificationChannel(CH_SESSION, "Focus session", NotificationManager.IMPORTANCE_LOW)
            ch.description = "Shows the running focus session and time left"
            ch.setShowBadge(false)
            m.createNotificationChannel(ch)
        }
        if (m.getNotificationChannel(CH_SETUP) == null) {
            val ch = NotificationChannel(CH_SETUP, "Setup & permissions", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "Reminders to turn on Accessibility or other permissions"
            m.createNotificationChannel(ch)
        }
    }

    private fun openApp(c: Context): PendingIntent {
        val i = Intent(c, MainActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        return PendingIntent.getActivity(c, 1, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun openAccessibility(c: Context): PendingIntent {
        val i = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(c, 2, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun openNotifSettings(c: Context): PendingIntent {
        val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        i.putExtra(Settings.EXTRA_APP_PACKAGE, c.packageName)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(c, 3, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** Session chal raha ho to ongoing notification (countdown), nahi to hata do. Sasta hai, tick se bulao. */
    fun syncSession(c: Context) {
        if (!Store.sessionOn(c)) {
            if (postedUntil != 0L) {
                postedUntil = 0L
                nm(c).cancel(ID_SESSION)
            }
            return
        }
        val until = Store.until(c)
        if (until == postedUntil) return
        postedUntil = until
        channels(c)
        val ends = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(until)).lowercase()
        val mode = if (Store.allowMode(c)) "Only your Bag apps are allowed" else "Your Bag apps are blocked"
        val n = Notification.Builder(c, CH_SESSION)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setColor(Color.parseColor("#FF6A0A"))
            .setContentTitle("Focus session running")
            .setContentText("Ends at $ends  \u00B7  $mode")
            .setWhen(until)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setContentIntent(openApp(c))
            .setTimeoutAfter((until - System.currentTimeMillis()).coerceAtLeast(1000L))
            .build()
        nm(c).notify(ID_SESSION, n)
    }

    /** Accessibility OFF hai: tap karte hi seedha Accessibility settings khulti hai. */
    fun showAccessibilityNeeded(c: Context) {
        channels(c)
        val n = Notification.Builder(c, CH_SETUP)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setColor(Color.parseColor("#FF6A0A"))
            .setContentTitle("Turn on Accessibility")
            .setContentText("FocusLock needs it to block apps, Shorts and Reels.")
            .setStyle(
                Notification.BigTextStyle().bigText(
                    "FocusLock needs Accessibility access to block apps, YouTube Shorts and Instagram Reels. Tap to open settings and switch it on."
                )
            )
            .setCategory(Notification.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setContentIntent(openAccessibility(c))
            .addAction(Notification.Action.Builder(null, "Turn on", openAccessibility(c)).build())
            .build()
        nm(c).notify(ID_SETUP, n)
    }

    fun cancelSetup(c: Context) {
        nm(c).cancel(ID_SETUP)
    }

    /** Notification permission band hai to ye notification bhi nahi dikh sakti, isliye sirf in-app banner. */
    fun notifSettingsIntent(c: Context): PendingIntent = openNotifSettings(c)
}
