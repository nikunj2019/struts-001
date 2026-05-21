package com.youtubeauto.app.service

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.youtubeauto.app.ui.YouTubeCarSession

class YouTubeCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = YouTubeCarSession()
}
