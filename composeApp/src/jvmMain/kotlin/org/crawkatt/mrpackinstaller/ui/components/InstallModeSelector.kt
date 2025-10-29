package org.crawkatt.mrpackinstaller.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.crawkatt.mrpackinstaller.data.InstallMode
import org.crawkatt.mrpackinstaller.ui.theme.AppColors

@Composable
fun InstallModeSelector(
    selectedMode: InstallMode,
    onModeChange: (InstallMode) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Modo de Instalación",
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Bold,
            color = AppColors.MinecraftGreen
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactModeOption(
                selected = selectedMode == InstallMode.API_DOWNLOAD,
                onClick = { onModeChange(InstallMode.API_DOWNLOAD) },
                enabled = enabled,
                title = "Servidor",
                icon = "☁",
                modifier = Modifier.weight(1f)
            )

            CompactModeOption(
                selected = selectedMode == InstallMode.LOCAL_FILE,
                onClick = { onModeChange(InstallMode.LOCAL_FILE) },
                enabled = enabled,
                title = "Local",
                icon = "📁",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CompactModeOption(
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    title: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled
            )
            .height(70.dp),
        backgroundColor = if (selected) AppColors.MinecraftGreen.copy(alpha = 0.2f) else AppColors.DarkGray,
        border = if (selected) {
            BorderStroke(
                2.dp, 
                AppColors.MinecraftGreen
            )
        } else {
            BorderStroke(1.dp, Color(0xFF505050))
        },
        elevation = if (selected) 3.dp else 1.dp,
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.h5,
                color = if (selected) AppColors.MinecraftGreen else Color(0xFFAAAAAA)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium,
                color = if (selected) AppColors.MinecraftGreen else Color(0xFFCCCCCC)
            )
        }
    }
}