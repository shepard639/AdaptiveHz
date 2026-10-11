package com.healthcraft.adaptivehz.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = HealthCraftPrimary,
    onPrimary = Color.White,
    primaryContainer = HealthCraftMintContainer,
    onPrimaryContainer = HealthCraftPrimaryDark,
    secondary = HealthCraftPrimary,
    onSecondary = Color.White,
    secondaryContainer = MetricMintBg,
    onSecondaryContainer = HealthCraftPrimaryDark,
    tertiary = MetricAmberText,
    background = HealthCraftBg,
    onBackground = TextPrimary,
    surface = HealthCraftSurface,
    onSurface = TextPrimary,
    surfaceVariant = HealthCraftMintContainer,
    onSurfaceVariant = TextSecondary,
    outline = HealthCraftCardStroke,
    outlineVariant = HealthCraftMintBorder
)

@Composable
fun AdaptiveHzTheme(
    darkTheme: Boolean = false, // Strictly light theme as requested by user in HealthCraft style
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = HealthCraftBg.toArgb()
                it.navigationBarColor = Color.White.toArgb()
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
