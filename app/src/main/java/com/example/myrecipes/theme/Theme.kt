package com.example.myrecipes.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    secondary = SecondaryColor,
    background = DarkBg,
    surface = DarkCardBg,
    onBackground = DarkTextMain,
    onSurface = DarkTextMain,
    surfaceVariant = DarkCardBg,
    onSurfaceVariant = DarkTextMuted,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.White,
    secondary = SecondaryColor,
    background = LightBg,
    surface = LightCardBg,
    onBackground = LightTextMain,
    onSurface = LightTextMain,
    surfaceVariant = LightCardBg,
    onSurfaceVariant = LightTextMuted,
    outline = LightBorder
)

@Composable
fun MyRecipesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set default to false so our brand colors are applied by default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
