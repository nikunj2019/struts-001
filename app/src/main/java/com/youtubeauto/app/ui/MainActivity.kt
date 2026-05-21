package com.youtubeauto.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.youtubeauto.app.R
import com.youtubeauto.app.service.PlaybackService

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_start_service).setOnClickListener {
            startForegroundService(Intent(this, PlaybackService::class.java))
        }
    }
}
