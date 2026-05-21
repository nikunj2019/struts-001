package com.youtubeauto.app.ui

import android.content.ComponentName
import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.youtubeauto.app.data.VideoItem
import com.youtubeauto.app.data.YouTubeRepository
import com.youtubeauto.app.service.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PlayerScreen(
    carContext: CarContext,
    private val video: VideoItem
) : Screen(carContext) {

    private val repository = YouTubeRepository()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isLoading = true
    private var isPlaying = false
    private var controller: MediaController? = null

    init {
        startPlayback()
        connectController()
    }

    override fun onGetTemplate(): Template {
        return NavigationTemplate.Builder()
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setTitle(if (isPlaying) "Pause" else "Play")
                            .setOnClickListener { togglePlayback() }
                            .build()
                    )
                    .addAction(
                        Action.Builder()
                            .setTitle("Stop")
                            .setOnClickListener {
                                controller?.stop()
                                screenManager.pop()
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun startPlayback() {
        scope.launch {
            try {
                isLoading = true
                invalidate()
                repository.getStreamUrl(video.id) ?: return@launch
                val serviceIntent = Intent(carContext, PlaybackService::class.java)
                carContext.startForegroundService(serviceIntent)
                isLoading = false
                isPlaying = true
                invalidate()
            } catch (e: Exception) {
                isLoading = false
                invalidate()
            }
        }
    }

    private fun connectController() {
        val token = SessionToken(
            carContext,
            ComponentName(carContext, PlaybackService::class.java)
        )
        val future = MediaController.Builder(carContext, token).buildAsync()
        future.addListener({
            controller = future.get()
            val item = MediaItem.Builder()
                .setMediaId(video.id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(video.title)
                        .setArtist(video.channelName)
                        .build()
                )
                .build()
            controller?.setMediaItem(item)
            controller?.prepare()
            controller?.play()
        }, MoreExecutors.directExecutor())
    }

    private fun togglePlayback() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) {
            ctrl.pause()
            isPlaying = false
        } else {
            ctrl.play()
            isPlaying = true
        }
        invalidate()
    }
}
