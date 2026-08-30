package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MineDramaColorScheme = darkColorScheme(
    primary = DramaCrimsonBright,
    onPrimary = Color.White,
    primaryContainer = DramaCrimson,
    onPrimaryContainer = Color.White,
    secondary = DramaGold,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceHighlight,
    onSecondaryContainer = DramaGoldBright,
    tertiary = AccentPurple,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkSurfaceHighlight,
    outlineVariant = Color(0x33FFFFFF)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = DarkBackground.toArgb()
                it.navigationBarColor = DarkBackground.toArgb()
                val insetsController = WindowCompat.getInsetsController(it, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = MineDramaColorScheme,
        typography = Typography,
        content = content
    )
}
