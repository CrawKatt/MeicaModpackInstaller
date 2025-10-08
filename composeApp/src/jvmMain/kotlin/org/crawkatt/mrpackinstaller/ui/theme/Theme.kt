package org.crawkatt.mrpackinstaller.ui.theme

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppColors {
    val Primary = Color(0xFF1DB954)
    val PrimaryVariant = Color(0xFF1AA34A)
    val Secondary = Color(0xFF03DAC6)
    val SecondaryVariant = Color(0xFF018786)
    val Background = Color(0xFF121212)
    val Surface = Color(0xFF1E1E1E)
    val Error = Color(0xFFCF6679)
    val OnPrimary = Color.White
    val OnSecondary = Color.Black
    val OnBackground = Color(0xFFE1E1E1)
    val OnSurface = Color(0xFFE1E1E1)
    val OnError = Color.Black
    val TextFieldBackground = Color(0xFF2A2A2A)
    val TextFieldBorder = Color(0xFF3A3A3A)
    val StatusBackground = Color(0xFF2A2A2A)
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