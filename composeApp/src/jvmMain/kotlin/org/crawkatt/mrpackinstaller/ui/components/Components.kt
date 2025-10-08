package org.crawkatt.mrpackinstaller.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.crawkatt.mrpackinstaller.ui.theme.AppColors

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(180.dp)
        )
        Text(text = value)
    }
}

@Composable
fun FileSelectionCard(
    title: String,
    filePath: String,
    placeholder: String,
    onBrowseClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = filePath,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text(placeholder) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = MaterialTheme.colors.onSurface,
                        backgroundColor = AppColors.TextFieldBackground,
                        cursorColor = MaterialTheme.colors.primary,
                        focusedBorderColor = MaterialTheme.colors.primary,
                        unfocusedBorderColor = AppColors.TextFieldBorder
                    )
                )
                Button(
                    onClick = onBrowseClick,
                    enabled = enabled
                ) {
                    Text("Examinar")
                }
            }
        }
    }
}

@Composable
fun ModpackInfoCard(
    name: String,
    version: String,
    summary: String,
    minecraftVersion: String,
    loader: String,
    modCount: String,
    totalSize: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Información del Modpack", fontWeight = FontWeight.Bold)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            InfoRow("Nombre:", name)
            InfoRow("Versión:", version)
            InfoRow("Resumen:", summary)
            InfoRow("Versión de Minecraft:", minecraftVersion)
            InfoRow("Loader:", loader)
            InfoRow("Número de mods:", modCount)
            totalSize?.let { 
                InfoRow("Tamaño total aprox:", it)
            }
        }
    }
}

@Composable
fun StatusCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = AppColors.StatusBackground,
        elevation = 2.dp
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colors.onSurface
        )
    }
}

@Composable
fun ProgressCard(progress: Float) {
    LinearProgressIndicator(
        progress = progress,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colors.primary
    )
}

@Composable
fun ActionButtons(
    onClearClick: () -> Unit,
    onInstallClick: () -> Unit,
    clearEnabled: Boolean,
    installEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onClearClick,
            enabled = clearEnabled,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colors.primary
            )
        ) {
            Text("Limpiar")
        }

        Button(
            onClick = onInstallClick,
            enabled = installEnabled,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.primary
            )
        ) {
            Text("Instalar", color = Color.White)
        }
    }
}

@Composable
fun AppHeader() {
    Column {
        Text(
            text = "Instalador de Modpacks .mrpack",
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold
        )
        Divider()
    }
}