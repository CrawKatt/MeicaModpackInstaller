package org.crawkatt.mrpackinstaller.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.crawkatt.mrpackinstaller.ui.theme.AppColors

@Composable
fun StatusCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = AppColors.SurfaceVariant,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "ℹ",
                style = MaterialTheme.typography.h6,
                color = AppColors.MinecraftGreen,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = message,
                color = MaterialTheme.colors.onSurface
            )
        }
    }
}

@Composable
fun ProgressCard(progress: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = AppColors.SurfaceVariant,
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Instalando...",
                    fontWeight = FontWeight.Bold,
                    color = AppColors.MinecraftGreen
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontWeight = FontWeight.Bold,
                    color = AppColors.MinecraftGreen
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = AppColors.MinecraftGreen,
                backgroundColor = AppColors.DarkGray
            )
        }
    }
}

@Composable
fun LauncherHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "MINECRAFT",
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
            color = AppColors.MinecraftGreen
        )
        Text(
            text = "Modpack Installer",
            style = MaterialTheme.typography.h6,
            color = Color(0xFFAAAAAA)
        )
        Divider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = AppColors.MinecraftGreen.copy(alpha = 0.3f),
            thickness = 2.dp
        )
    }
}

@Composable
fun CompactFileSelector(
    title: String,
    filePath: String,
    onBrowseClick: () -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Bold,
            color = AppColors.MinecraftGreen
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = filePath.takeLastWhile { it != '/' && it != '\\' }.ifEmpty { "..." },
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.caption,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color(0xFFCCCCCC),
                    backgroundColor = Color(0xFF1A1A1A),
                    cursorColor = AppColors.MinecraftGreen,
                    focusedBorderColor = AppColors.MinecraftGreen,
                    unfocusedBorderColor = Color(0xFF404040)
                )
            )
            Button(
                onClick = onBrowseClick,
                enabled = enabled,
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = AppColors.DarkGray,
                    contentColor = Color.White
                )
            ) {
                Text("📁")
            }
        }
    }
}

@Composable
fun CompactModpackInfo(
    name: String,
    version: String,
    minecraftVersion: String,
    loader: String,
    modCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        backgroundColor = AppColors.MinecraftGreen.copy(alpha = 0.1f)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                color = AppColors.MinecraftGreen
            )
            Text(
                text = "v$version • MC $minecraftVersion",
                style = MaterialTheme.typography.caption,
                color = Color(0xFFAAAAAA)
            )
            Text(
                text = "$loader • $modCount mods",
                style = MaterialTheme.typography.caption,
                color = Color(0xFFAAAAAA)
            )
        }
    }
}

@Composable
fun MainModpackDisplay(
    name: String,
    version: String,
    summary: String?
) {
    Card(
        modifier = Modifier.widthIn(max = 600.dp),
        elevation = 4.dp,
        backgroundColor = Color.Black.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.Bold,
                color = AppColors.MinecraftGreen,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "Versión $version",
                style = MaterialTheme.typography.h6,
                color = Color(0xFFCCCCCC)
            )
            if (summary.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = summary ?: "No hay descripción",
                    style = MaterialTheme.typography.body2,
                    color = Color(0xFFAAAAAA),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun LargePlayButton(
    onPlayClick: () -> Unit,
    onClearClick: () -> Unit,
    showClearButton: Boolean,
    clearEnabled: Boolean,
    playEnabled: Boolean,
    isInstalling: Boolean
) {
    Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onPlayClick,
            enabled = playEnabled && !isInstalling,
            modifier = Modifier
                .width(300.dp)
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = AppColors.MinecraftGreen,
                contentColor = Color.White,
                disabledBackgroundColor = AppColors.TextFieldBorder
            )
        ) {
            Text(
                text = if (isInstalling) "INSTALANDO..." else "▶ JUGAR",
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold
            )
        }

        if (showClearButton) {
            Button(
                onClick = onClearClick,
                enabled = clearEnabled,
                modifier = Modifier
                    .width(300.dp)
                    .height(45.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = AppColors.DarkGray,
                    contentColor = Color.White,
                    disabledBackgroundColor = AppColors.TextFieldBackground
                )
            ) {
                Text(
                    text = "Limpiar",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}