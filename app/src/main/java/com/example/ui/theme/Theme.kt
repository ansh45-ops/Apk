package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = ForgeEmerald,
    onPrimary = Color(0xFF032218),
    primaryContainer = ForgeEmeraldDark,
    onPrimaryContainer = ForgeEmeraldBright,
    secondary = ForgeCyan,
    onSecondary = Color(0xFF02232E),
    secondaryContainer = ForgeCyanDark,
    onSecondaryContainer = ForgeCyanBright,
    tertiary = ForgeAmber,
    onTertiary = Color(0xFF281100),
    tertiaryContainer = ForgeAmberDark,
    onTertiaryContainer = Color(0xFFFDE68A),
    background = ForgeObsidian,
    onBackground = ForgeTextPrimary,
    surface = ForgeDeepSlate,
    onSurface = ForgeTextPrimary,
    surfaceVariant = ForgeCardSurface,
    onSurfaceVariant = ForgeTextSecondary,
    outline = ForgeBorderSubtle,
    error = ForgeError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = LightTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFCBD5E1),
    error = ForgeError,
    onError = Color.White
)

val ForgeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ForgeShapes,
        content = content
    )
}
