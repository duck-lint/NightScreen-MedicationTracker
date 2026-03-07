package com.redpillz.pixelutility

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.redpillz.pixelutility.ui.RedPillzRoot
import com.redpillz.pixelutility.ui.theme.RedPillzTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as RedPillzApp).appContainer
        setContent {
            RedPillzTheme {
                RedPillzRoot(appContainer = appContainer)
            }
        }
    }
}

