package com.example.roomservice.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ThemeRed,
    onPrimary = Color.White,
    secondary = ThemeOrange,
    onSecondary = Color.White,
    tertiary = ThemeYellow,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextOnDark,
    surface = DarkSurface,
    onSurface = TextOnDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextOnDarkMuted
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC5A059),
    onPrimary = Color.White,
    secondary = Color(0xFF9E7E45),
    onSecondary = Color.White,
    tertiary = Color(0xFFFFC107),
    onTertiary = Color.Black,
    background = BackgroundLight,
    onBackground = TextBlack,
    surface = SurfaceWhite,
    onSurface = TextBlack,
    surfaceVariant = SurfaceWhite,
    onSurfaceVariant = TextGray,
    surfaceTint = Color.White,
    outline = BorderLight
)

@Composable
fun RoomServiceTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme // Force light theme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BackgroundLight.toArgb()
            window.navigationBarColor = BackgroundLight.toArgb()
            
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
