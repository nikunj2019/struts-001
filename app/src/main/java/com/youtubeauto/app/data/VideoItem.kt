package com.youtubeauto.app.data

data class VideoItem(
    val id: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val duration: String,
    val viewCount: String
)

data class StreamInfo(
    val videoId: String,
    val streamUrl: String,
    val mimeType: String,
    val quality: String
)
