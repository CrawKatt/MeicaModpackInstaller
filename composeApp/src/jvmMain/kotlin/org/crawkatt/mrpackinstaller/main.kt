package org.crawkatt.mrpackinstaller

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.formdev.flatlaf.FlatDarkLaf
import org.crawkatt.mrpackinstaller.ui.ModpackInstallerScreen

fun main() = application {
    FlatDarkLaf.setup()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Mrpack Installer",
        state = WindowState(size = DpSize(1200.dp, 800.dp))
    ) {
        ModpackInstallerScreen()
    }
}