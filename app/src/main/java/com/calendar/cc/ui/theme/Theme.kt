package com.calendar.cc.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    primaryContainer = CoralLight,
    onPrimaryContainer = CoralDark,
    secondary = Amber,
    onSecondary = Color.White,
    secondaryContainer = AmberLight,
    onSecondaryContainer = AmberDark,
    tertiary = Sage,
    onTertiary = Color.White,
    tertiaryContainer = SageLight,
    onTertiaryContainer = SageDark,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = CardLight,
    onSurfaceVariant = TextSecondaryLight,
    error = FestivalRed,
    onError = Color.White,
    outline = DividerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = CoralLight,
    onPrimary = Color(0xFF4A1515),
    primaryContainer = CoralDark,
    onPrimaryContainer = CoralLight,
    secondary = AmberLight,
    onSecondary = Color(0xFF3D2600),
    secondaryContainer = AmberDark,
    onSecondaryContainer = AmberLight,
    tertiary = SageLight,
    onTertiary = Color(0xFF1A3A00),
    tertiaryContainer = SageDark,
    onTertiaryContainer = SageLight,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondaryDark,
    error = FestivalRedDark,
    onError = Color(0xFF4A1515),
    outline = DividerDark
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            if (darkTheme) androidx.compose.material3.dynamicDarkColorScheme(context)
            else androidx.compose.material3.dynamicLightColorScheme(context)
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