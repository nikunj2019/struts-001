package com.youtubeauto.app.ui

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Template
import com.youtubeauto.app.data.VideoItem
import com.youtubeauto.app.data.YouTubeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchScreen(carContext: CarContext) : Screen(carContext) {

    private val repository = YouTubeRepository()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var results: List<VideoItem> = emptyList()
    private var isLoading = false
    private var currentQuery = ""
    private var searchJob: Job? = null

    override fun onGetTemplate(): Template {
        return SearchTemplate.Builder(SearchListener())
            .setHeaderAction(Action.BACK)
            .setShowKeyboardByDefault(true)
            .setSearchHint(carContext.getString(com.youtubeauto.app.R.string.search_hint))
            .setItemList(buildResultList())
            .setLoading(isLoading)
            .build()
    }

    private fun buildResultList(): ItemList {
        val builder = ItemList.Builder()
        results.take(MAX_RESULTS).forEach { video ->
            builder.addItem(
                GridItem.Builder()
                    .setTitle(video.title)
                    .setText("${video.channelName} • ${video.duration}")
                    .setOnClickListener { screenManager.push(PlayerScreen(carContext, video)) }
                    .build()
            )
        }
        return builder.build()
    }

    private fun search(query: String) {
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(400)
            try {
                isLoading = true
                invalidate()
                results = repository.search(query)
                isLoading = false
                invalidate()
            } catch (e: Exception) {
                isLoading = false
                invalidate()
            }
        }
    }

    private inner class SearchListener : SearchTemplate.SearchCallback {
        override fun onSearchTextChanged(searchText: String) {
            currentQuery = searchText
            if (searchText.length >= 2) search(searchText)
        }

        override fun onSearchSubmitted(searchText: String) {
            if (searchText.isNotBlank()) search(searchText)
        }
    }

    companion object {
        private const val MAX_RESULTS = 8
    }
}
