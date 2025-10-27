package org.crawkatt.mrpackinstaller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.crawkatt.mrpackinstaller.data.InstallMode
import org.crawkatt.mrpackinstaller.ui.components.*
import org.crawkatt.mrpackinstaller.ui.theme.MrpackInstallerTheme

@Composable
fun ModpackInstallerScreen() {
    val viewModel = remember { ModpackInstallerViewModel() }
    rememberCoroutineScope()

    MrpackInstallerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppHeader()


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
                        FileSelectionCard(
                            title = "Archivo .mrpack",
                            filePath = viewModel.selectedMrpackFile?.absolutePath ?: "",
                            placeholder = "Selecciona un archivo...",
                            onBrowseClick = { viewModel.selectMrpackFile() },
                            enabled = !viewModel.isInstalling
                        )
                    }
                }

                viewModel.modpackInfo?.let { info ->
                    ModpackInfoCard(
                        name = info.name,
                        version = info.version,
                        summary = info.summary,
                        minecraftVersion = info.minecraftVersion,
                        loader = info.loader,
                        modCount = info.modCount.toString(),
                        totalSize = if (info.totalSize > 0) viewModel.formatFileSize(info.totalSize) else null
                    )
                }

                FileSelectionCard(
                    title = "Carpeta .minecraft",
                    filePath = viewModel.minecraftPath?.toString() ?: "",
                    placeholder = "Ruta de .minecraft",
                    onBrowseClick = { viewModel.selectMinecraftPath() },
                    enabled = !viewModel.isInstalling
                )

                if (viewModel.isInstalling) {
                    ProgressCard(progress = viewModel.installProgress)
                }

                if (viewModel.statusMessage.isNotEmpty()) {
                    StatusCard(message = viewModel.statusMessage)
                }

                Spacer(Modifier.weight(1f))
                ActionButtons(
                    onClearClick = { viewModel.clear() },
                    onPlayClick = { viewModel.playOrUpdate() },
                    showClearButton = viewModel.installMode == InstallMode.LOCAL_FILE,
                    clearEnabled = viewModel.canClear(),
                    playEnabled = viewModel.canPlay()
                )
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
        title = {
            Text(
                text = "Actualización del Modpack",
                style = MaterialTheme.typography.h6
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary
                )
            ) {
                Text("Aceptar", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) {
                Text("Cancelar")
            }
        }
    )
}