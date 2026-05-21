package com.youtubeauto.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.youtubeauto.app.data.YouTubeRepository
import com.youtubeauto.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val repository = YouTubeRepository()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MediaSessionCallback())
            .build()
        startForegroundNotification()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    fun playVideo(videoId: String, title: String, channel: String) {
        scope.launch {
            try {
                val stream = repository.getStreamUrl(videoId) ?: return@launch
                val item = MediaItem.Builder()
                    .setUri(stream.streamUrl)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(title)
                            .setArtist(channel)
                            .build()
                    )
                    .build()
                player.setMediaItem(item)
                player.prepare()
                player.play()
            } catch (e: Exception) {
                Log.e(TAG, "Playback error", e)
            }
        }
    }

    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            // Items accepted as-is; URL resolution happens in playVideo()
            return Futures.immediateFuture(mediaItems)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "YouTube Auto Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification() {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("YouTube Auto")
            .setContentText("Ready to play")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "PlaybackService"
        private const val CHANNEL_ID = "youtube_auto_playback"
        private const val NOTIFICATION_ID = 1001
    }
}
