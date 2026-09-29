package com.asa.note

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asa.note.repo.AppSettings
import com.asa.note.ui.nav.AppNav
import com.asa.note.ui.theme.NoteTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 手势区那层系统对比度蒙版会盖住底栏自己的底色，让底部出现一条中性灰的断层。
        window.isNavigationBarContrastEnforced = false

        val container = (application as NoteApp).container

        setContent {
            val settings by container.settings.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            NoteTheme(settings) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNav(container, settings)
                }
            }
        }
    }
}
