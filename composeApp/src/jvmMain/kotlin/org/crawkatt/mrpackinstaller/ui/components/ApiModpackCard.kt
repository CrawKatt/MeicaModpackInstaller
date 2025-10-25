package org.crawkatt.mrpackinstaller.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.crawkatt.mrpackinstaller.data.ApiResponse

@Composable
fun ApiModpackCard(
    apiResponse: ApiResponse?,
    isLoading: Boolean,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    println("🔍 ApiModpackCard - apiResponse: ${apiResponse != null}, isLoading: $isLoading")
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Modpack del Servidor",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onRefreshClick,
                    enabled = !isLoading
                ) {
                    Text("🔄")
                }
            }

            when {
                isLoading -> LoadingView()
                apiResponse == null -> ErrorView("Error al cargar información del servidor")
                !apiResponse.available -> ErrorView("No hay modpack disponible en el servidor")
                else -> ModpackView(apiResponse)
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.body2,
            color = Color.Gray
        )
    }
}

@Composable
private fun ModpackView(apiResponse: ApiResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colors.primary),
        elevation = 4.dp,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "☁",
                style = MaterialTheme.typography.h4,
                color = MaterialTheme.colors.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = apiResponse.modpackInfo.name,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "v${apiResponse.modpackInfo.versionId} • Minecraft ${apiResponse.modpackInfo.minecraftVersion}",
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray
                )

                Text(
                    text = "${apiResponse.modpackInfo.loader} ${apiResponse.modpackInfo.loaderVersion} • ${apiResponse.modpackInfo.modCount} mods",
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray
                )

                if (apiResponse.modpackInfo.summary.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = apiResponse.modpackInfo.summary,
                        style = MaterialTheme.typography.caption,
                        color = Color.Gray
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatFileSize(apiResponse.fileSize),
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = apiResponse.fileName,
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
