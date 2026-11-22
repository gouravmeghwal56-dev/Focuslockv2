package com.focuslock.app

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService

/** Sirf media sessions padhne ke liye (Settings me "Notification access" on karna padta hai). */
class MediaListener : NotificationListenerService()

object Media {
    class Info(val title: String, val artist: String, val playing: Boolean, val ctl: MediaController)

    fun hasAccess(c: Context): Boolean {
        val s = Settings.Secure.getString(c.contentResolver, "enabled_notification_listeners") ?: return false
        return s.contains(c.packageName)
    }

    /** Is music app ka abhi ka track + controls (nahi chal raha to null). */
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
            Info(title, artist, st == PlaybackState.STATE_PLAYING || st == PlaybackState.STATE_BUFFERING, ctl)
        } catch (e: Exception) {
            null
        }
    }
}
