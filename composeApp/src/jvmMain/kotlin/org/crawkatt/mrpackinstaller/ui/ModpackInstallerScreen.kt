package org.crawkatt.mrpackinstaller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.crawkatt.mrpackinstaller.data.InstallMode
import org.crawkatt.mrpackinstaller.ui.components.*
import org.crawkatt.mrpackinstaller.ui.theme.MrpackInstallerTheme

@Composable
fun ModpackInstallerScreen() {
    val viewModel = remember { ModpackInstallerViewModel() }
    val scope = rememberCoroutineScope()

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
                    onInstallClick = {
                        scope.launch {
                            viewModel.installModpack()
                        }
                    },
                    clearEnabled = viewModel.canClear(),
                    installEnabled = viewModel.canInstall()
                )
            }
        }
    }
}