package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val RunvantaDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = DeepMidnight,
    primaryContainer = CyanContainer,
    onPrimaryContainer = ElectricCyan,
    secondary = HotCoral,
    onSecondary = Color.White,
    secondaryContainer = CoralContainer,
    onSecondaryContainer = HotCoral,
    tertiary = CyberGold,
    onTertiary = DeepMidnight,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = CyberGold,
    background = DeepMidnight,
    onBackground = TextPrimary,
    surface = MidnightSurface,
    onSurface = TextPrimary,
    surfaceVariant = MidnightCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHigh = MidnightElevated,
    outline = OutlineSubtle
)

private val RunvantaLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFF4FF),
    onPrimaryContainer = Color(0xFF002736),
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9DF),
    onSecondaryContainer = Color(0xFF470011),
    tertiary = Color(0xFFB37B00),
    onTertiary = Color.White,
    background = LightBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightCard,
    onSurfaceVariant = Color(0xFF475577),
    outline = Color(0xFFB5C2DF)
)

val RunvantaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RunvantaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) RunvantaDarkColorScheme else RunvantaLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RunvantaTypography,
        shapes = RunvantaShapes,
        content = content
    )
}
