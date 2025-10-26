package org.crawkatt.mrpackinstaller.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.crawkatt.mrpackinstaller.data.InstallMode

@Composable
fun InstallModeSelector(
    selectedMode: InstallMode,
    onModeChange: (InstallMode) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Modo de Instalación",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InstallModeOption(
                    selected = selectedMode == InstallMode.API_DOWNLOAD,
                    onClick = { onModeChange(InstallMode.API_DOWNLOAD) },
                    enabled = enabled,
                    title = "Descargar desde Servidor",
                    description = "Conectar a la API para descargar modpacks",
                    icon = "☁",
                    modifier = Modifier.weight(1f)
                )

                InstallModeOption(
                    selected = selectedMode == InstallMode.LOCAL_FILE,
                    onClick = { onModeChange(InstallMode.LOCAL_FILE) },
                    enabled = enabled,
                    title = "Archivo Local",
                    description = "Seleccionar archivo .mrpack local",
                    icon = "📁",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun InstallModeOption(
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    title: String,
    description: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled
            ),
        backgroundColor = if (selected) {
            MaterialTheme.colors.primary.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colors.surface
        },
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                2.dp, 
                MaterialTheme.colors.primary
            )
        } else null,
        elevation = if (selected) 8.dp else 2.dp,
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.h4,
                color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
            )

            Text(
                text = title,
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Medium,
                color = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
            )

            Text(
                text = description,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
