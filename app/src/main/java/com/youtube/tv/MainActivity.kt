package com.youtube.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.youtube.tv.ui.screens.YoutubeWV
import com.youtube.tv.ui.theme.NoTubeTVTheme
import com.youtube.tv.utils.checkSerial
import com.youtube.tv.utils.getCompatiblePrimarySerialNumberFromDeviceHW

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkSerial(getCompatiblePrimarySerialNumberFromDeviceHW())
        enableEdgeToEdge()
        window.setLayout(3840, 2160)
        setContent {
            NoTubeTVTheme {
               Box(modifier = Modifier.fillMaxSize()) { YoutubeWV() }
            }
        }
    }
}
