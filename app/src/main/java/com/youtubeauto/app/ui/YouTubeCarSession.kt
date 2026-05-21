package com.youtubeauto.app.ui

import androidx.car.app.Screen
import androidx.car.app.Session

class YouTubeCarSession : Session() {
    override fun onCreateScreen(intent: android.content.Intent): Screen {
        return HomeScreen(carContext)
    }
}
