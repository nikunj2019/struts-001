package com.youtubeauto.app.ui

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.youtubeauto.app.data.VideoItem
import com.youtubeauto.app.data.YouTubeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HomeScreen(carContext: CarContext) : Screen(carContext) {

    private val repository = YouTubeRepository()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var videos: List<VideoItem> = emptyList()
    private var isLoading = true
    private var errorMessage: String? = null

    init {
        loadTrending()
    }

    override fun onGetTemplate(): Template {
        if (isLoading) {
            return GridTemplate.Builder()
                .setTitle(carContext.getString(com.youtubeauto.app.R.string.home_title))
                .setLoading(true)
                .setHeaderAction(Action.APP_ICON)
                .setActionStrip(buildActionStrip())
                .build()
        }

        val listBuilder = ItemList.Builder()
        videos.take(MAX_GRID_ITEMS).forEach { video ->
            listBuilder.addItem(
                GridItem.Builder()
                    .setTitle(video.title)
                    .setText("${video.channelName} • ${video.duration}")
                    .setOnClickListener { onVideoSelected(video) }
                    .build()
            )
        }

        return GridTemplate.Builder()
            .setTitle(carContext.getString(com.youtubeauto.app.R.string.trending_title))
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(buildActionStrip())
            .setSingleList(listBuilder.build())
            .build()
    }

    private fun buildActionStrip(): ActionStrip {
        return ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Search")
                    .setOnClickListener { screenManager.push(SearchScreen(carContext)) }
                    .build()
            )
            .build()
    }

    private fun onVideoSelected(video: VideoItem) {
        screenManager.push(PlayerScreen(carContext, video))
    }

    private fun loadTrending() {
        scope.launch {
            try {
                isLoading = true
                invalidate()
                videos = repository.getTrending()
                isLoading = false
                invalidate()
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message
                invalidate()
            }
        }
    }

    companion object {
        private const val MAX_GRID_ITEMS = 6
    }
}
