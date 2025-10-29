package org.crawkatt.mrpackinstaller.ui.components

import androidx.compose.foundation.BorderStroke
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
import org.crawkatt.mrpackinstaller.ui.theme.AppColors
import org.crawkatt.mrpackinstaller.utils.FileUtils

@Composable
fun ApiModpackCard(
    apiResponse: ApiResponse?,
    isLoading: Boolean,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    println("🔍 ApiModpackCard - apiResponse: ${apiResponse != null}, isLoading: $isLoading")
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Modpack del Servidor",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                color = AppColors.MinecraftGreen
            )

            IconButton(
                onClick = onRefreshClick,
                enabled = !isLoading,
                modifier = Modifier.size(32.dp)
            ) {
                Text("🔄", style = MaterialTheme.typography.body2)
            }
        }

        when {
            isLoading -> CompactLoadingView()
            apiResponse == null -> CompactErrorView("Error al cargar")
            !apiResponse.available -> CompactErrorView("No disponible")
            else -> CompactModpackView(apiResponse)
        }
    }
}

@Composable
private fun CompactLoadingView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = AppColors.MinecraftGreen,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun CompactErrorView(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = AppColors.DarkGray,
        elevation = 1.dp
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.caption,
            color = Color(0xFFAAAAAA),
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun CompactModpackView(apiResponse: ApiResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = AppColors.MinecraftGreen.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, AppColors.MinecraftGreen),
        elevation = 1.dp,
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "☁",
                    style = MaterialTheme.typography.h6,
                    color = AppColors.MinecraftGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = apiResponse.modpackInfo.name,
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.MinecraftGreen
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "v${apiResponse.modpackInfo.versionId} • MC ${apiResponse.modpackInfo.minecraftVersion}",
                style = MaterialTheme.typography.caption,
                color = Color(0xFFAAAAAA)
            )

            Text(
                text = "${apiResponse.modpackInfo.loader} • ${apiResponse.modpackInfo.modCount} mods • ${FileUtils.formatFileSize(apiResponse.fileSize)}",
                style = MaterialTheme.typography.caption,
                color = Color(0xFF888888)
            )
        }
    }
}