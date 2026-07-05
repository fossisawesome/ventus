package com.fossisawesome.ventus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fossisawesome.ventus.ui.components.Text
import com.fossisawesome.ventus.ui.theme.LocalAppColors
import com.fossisawesome.ventus.ui.theme.VentusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VentusTheme {
                val colors = LocalAppColors.current
                Box(
                    modifier = Modifier.fillMaxSize().background(colors.bg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Ventus", color = colors.text)
                }
            }
        }
    }
}
