package com.example.roomservice.util

import android.content.Context
import android.media.MediaPlayer
import com.example.roomservice.R

object SoundHelper {
    private var mediaPlayer: MediaPlayer? = null

    fun playOrderBell(context: Context) {
        try {
            mediaPlayer?.release()
            // Try to find custom hotel_bell in res/raw, otherwise use system notification
            val resId = context.resources.getIdentifier("hotel_bell", "raw", context.packageName)
            
            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(context, resId)
            } else {
                // Play default system notification sound if hotel_bell is missing
                val notification = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                mediaPlayer = MediaPlayer.create(context, notification)
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
