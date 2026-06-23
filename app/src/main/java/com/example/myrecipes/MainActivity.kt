package com.example.myrecipes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.myrecipes.data.SettingsStore
import com.example.myrecipes.theme.MyRecipesTheme

class MainActivity : ComponentActivity() {
  private lateinit var settingsStore: SettingsStore
  private val themeValState = mutableStateOf("system")
  private val sharedTextState = mutableStateOf<String?>(null)

  private val preferenceChangeListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
    if (key == "theme") {
      themeValState.value = settingsStore.getTheme()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    settingsStore = SettingsStore(this)
    themeValState.value = settingsStore.getTheme()
    sharedTextState.value = getSharedText(intent)
    settingsStore.registerListener(preferenceChangeListener)

    enableEdgeToEdge()
    setContent {
      val themeVal = themeValState.value
      val systemDark = isSystemInDarkTheme()
      val darkTheme = when (themeVal) {
        "dark" -> true
        "light" -> false
        else -> systemDark
      }

      MyRecipesTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(
            sharedText = sharedTextState.value,
            onSharedTextConsumed = { sharedTextState.value = null }
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    sharedTextState.value = getSharedText(intent)
  }

  private fun getSharedText(intent: android.content.Intent?): String? {
    return if (intent?.action == android.content.Intent.ACTION_SEND && intent.type == "text/plain") {
      intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
    } else {
      null
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    settingsStore.unregisterListener(preferenceChangeListener)
  }
}
