package com.focuslock.app

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService

/** Sirf media sessions + notification ka cover photo padhne ke liye (Notification access chahiye). */
class MediaListener : NotificationListenerService() {
    override fun onListenerConnected() {
        inst = this
    }

    override fun onListenerDisconnected() {
        if (inst === this) inst = null
    }

    companion object {
        @Volatile
        var inst: MediaListener? = null
    }
}

object Media {
    class Info(
        val title: String,
        val artist: String,
        val playing: Boolean,
        val ctl: MediaController,
        val art: Bitmap?
    )

    private var artKey = ""
    private var artBmp: Bitmap? = null

    fun hasAccess(c: Context): Boolean {
        val s = Settings.Secure.getString(c.contentResolver, "enabled_notification_listeners") ?: return false
        return s.contains(c.packageName)
    }

    /** Is music app ka abhi ka track + cover + controls (nahi chal raha to null). */
    fun get(c: Context, pkg: String): Info? {
        if (!hasAccess(c)) return null
        return try {
            val msm = c.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val list = msm.getActiveSessions(ComponentName(c, MediaListener::class.java))
            val ctl = list.firstOrNull { it.packageName == pkg } ?: return null
            val md = ctl.metadata
            val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
            val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""
            val st = ctl.playbackState?.state
            val key = "$pkg|$title|$artist"
            var art: Bitmap? = if (key == artKey) artBmp else null
            if (art == null) {
                art = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                    ?: md?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
                    ?: fromNotification(c, pkg)
                artKey = key
                artBmp = art
            }
            Info(title, artist, st == PlaybackState.STATE_PLAYING || st == PlaybackState.STATE_BUFFERING, ctl, art)
        } catch (e: Exception) {
            null
        }
    }

    // metadata me photo na ho to music notification ki badi photo use karo
    private fun fromNotification(c: Context, pkg: String): Bitmap? {
        val svc = MediaListener.inst ?: return null
        return try {
            val list = svc.activeNotifications ?: return null
            for (sbn in list) {
                if (sbn.packageName != pkg) continue
                val icon = sbn.notification.getLargeIcon() ?: continue
                val d = icon.loadDrawable(c) ?: continue
                val b = toBitmap(d)
                if (b != null) return b
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun toBitmap(d: Drawable): Bitmap? {
        if (d is BitmapDrawable && d.bitmap != null) return d.bitmap
        val w = d.intrinsicWidth.coerceAtLeast(1)
        val h = d.intrinsicHeight.coerceAtLeast(1)
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(b)
        d.setBounds(0, 0, w, h)
        d.draw(cv)
        return b
    }
}
