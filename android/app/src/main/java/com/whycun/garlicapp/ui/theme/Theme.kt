package com.whycun.garlicapp.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = CardBlue,
    secondary = Green,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    background = Background,
    surface = Surface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = Red,
)

@Composable
fun GarlicTheme(content: @Composable () -> Unit) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
            bodyMedium = TextStyle(fontSize = 13.sp, color = TextPrimary),
            bodySmall = TextStyle(fontSize = 11.sp, color = TextSecondary),
            labelSmall = TextStyle(fontSize = 10.sp, color = TextMuted),
        ),
        content = content
    )
}
