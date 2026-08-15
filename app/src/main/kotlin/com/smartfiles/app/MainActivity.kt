package com.smartfiles.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.smartfiles.core.designsystem.theme.SmartFilesTheme
import com.smartfiles.feature.navigation.SmartFilesNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SmartFilesTheme {
                SmartFilesNavHost()
            }
        }
    }
}
