package org.crawkatt.mrpackinstaller.ui.theme

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppColors {
    val Primary = Color(0xFF5CB85C)
    val PrimaryVariant = Color(0xFF4A9A4A)
    val Secondary = Color(0xFF5BC0DE)
    val SecondaryVariant = Color(0xFF46B8DA)
    val Background = Color(0xFF1C1C1C)
    val Surface = Color(0xFF2B2B2B)
    val SurfaceVariant = Color(0xFF363636)
    val Error = Color(0xFFD9534F)
    val OnPrimary = Color.White
    val OnSecondary = Color.White
    val OnBackground = Color(0xFFE0E0E0)
    val OnSurface = Color(0xFFE0E0E0)
    val OnError = Color.White
    val TextFieldBackground = Color(0xFF2A2A2A)
    val TextFieldBorder = Color(0xFF3A3A3A)
    val MinecraftGreen = Color(0xFF5CB85C)
    val DarkGray = Color(0xFF3C3C3C)
}

private val DarkColorPalette = darkColors(
    primary = AppColors.Primary,
    primaryVariant = AppColors.PrimaryVariant,
    secondary = AppColors.Secondary,
    secondaryVariant = AppColors.SecondaryVariant,
    background = AppColors.Background,
    surface = AppColors.Surface,
    error = AppColors.Error,
    onPrimary = AppColors.OnPrimary,
    onSecondary = AppColors.OnSecondary,
    onBackground = AppColors.OnBackground,
    onSurface = AppColors.OnSurface,
    onError = AppColors.OnError
)

@Composable
fun MrpackInstallerTheme(
    colors: Colors = DarkColorPalette,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = colors,
        content = content
    )
}