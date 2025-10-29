package org.crawkatt.mrpackinstaller.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.background
import org.crawkatt.mrpackinstaller.data.InstallMode
import org.crawkatt.mrpackinstaller.ui.components.*
import org.crawkatt.mrpackinstaller.ui.theme.AppColors
import org.crawkatt.mrpackinstaller.ui.theme.MrpackInstallerTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun ModpackInstallerScreen() {
    val viewModel = remember { ModpackInstallerViewModel() }
    rememberCoroutineScope()

    MrpackInstallerTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(Res.drawable.background),
                contentDescription = "Minecraft Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .width(400.dp)
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LauncherHeader()
                        InstallModeSelector(
                            selectedMode = viewModel.installMode,
                            onModeChange = { viewModel.switchInstallMode(it) },
                            enabled = !viewModel.isInstalling
                        )

                        when (viewModel.installMode) {
                            InstallMode.API_DOWNLOAD -> {
                                ApiModpackCard(
                                    apiResponse = viewModel.apiResponse,
                                    isLoading = viewModel.isLoadingModpackInfo,
                                    onRefreshClick = { viewModel.loadModpackInfo() }
                                )
                            }
                            InstallMode.LOCAL_FILE -> {
                                CompactFileSelector(
                                    title = "Archivo .mrpack",
                                    filePath = viewModel.selectedMrpackFile?.absolutePath ?: "",
                                    onBrowseClick = { viewModel.selectMrpackFile() },
                                    enabled = !viewModel.isInstalling
                                )
                            }
                        }

                        CompactFileSelector(
                            title = "Carpeta .minecraft",
                            filePath = viewModel.minecraftPath?.toString() ?: "",
                            onBrowseClick = { viewModel.selectMinecraftPath() },
                            enabled = !viewModel.isInstalling
                        )

                        viewModel.modpackInfo?.let { info ->
                            CompactModpackInfo(
                                name = info.name,
                                version = info.version,
                                minecraftVersion = info.minecraftVersion,
                                loader = info.loader,
                                modCount = info.modCount
                            )
                        }

                        if (viewModel.isInstalling) {
                            ProgressCard(progress = viewModel.installProgress)
                        }

                        if (viewModel.statusMessage.isNotEmpty()) {
                            StatusCard(message = viewModel.statusMessage)
                        }

                        Spacer(Modifier.weight(1f))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        viewModel.modpackInfo?.let { info ->
                            MainModpackDisplay(
                                name = info.name,
                                version = info.version,
                                summary = info.summary
                            )
                        }

                        LargePlayButton(
                            onPlayClick = { viewModel.playOrUpdate() },
                            onClearClick = { viewModel.clear() },
                            showClearButton = viewModel.installMode == InstallMode.LOCAL_FILE,
                            clearEnabled = viewModel.canClear(),
                            playEnabled = viewModel.canPlay(),
                            isInstalling = viewModel.isInstalling
                        )
                    }
                }
            }

            if (viewModel.showUpdateDialog) {
                UpdateDialog(
                    message = viewModel.updateDialogMessage,
                    onConfirm = { viewModel.confirmUpdate() },
                    onCancel = { viewModel.cancelUpdate() }
                )
            }
        }
    }
}

@Composable
fun UpdateDialog(
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        backgroundColor = AppColors.SurfaceVariant,
        title = {
            Text(
                text = "Actualización del Modpack",
                style = MaterialTheme.typography.h6,
                color = AppColors.MinecraftGreen
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colors.onSurface
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = AppColors.MinecraftGreen
                )
            ) {
                Text("Aceptar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = AppColors.DarkGray
                )
            ) {
                Text("Cancelar", color = Color.White)
            }
        }
    )
}