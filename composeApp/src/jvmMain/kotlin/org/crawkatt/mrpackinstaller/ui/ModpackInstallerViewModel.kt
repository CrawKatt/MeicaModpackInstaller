package org.crawkatt.mrpackinstaller.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.crawkatt.mrpackinstaller.data.ModpackInfo
import org.crawkatt.mrpackinstaller.services.*
import org.crawkatt.mrpackinstaller.utils.FileUtils
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

class ModpackInstallerViewModel {
    
    private val fileService = FileService()
    private val modpackService = ModpackService()
    private val downloadService = DownloadService()
    private val loaderInstallerService = LoaderInstallerService()

    var selectedMrpackFile by mutableStateOf<File?>(null)
    var minecraftPath by mutableStateOf<Path?>(null)
    var modpackIndex by mutableStateOf<JsonNode?>(null)
    var modpackInfo by mutableStateOf<ModpackInfo?>(null)
    var statusMessage by mutableStateOf("")
    var isInstalling by mutableStateOf(false)
    var installProgress by mutableStateOf(0f)
    
    init {
        detectMinecraftPath()
    }

    private fun detectMinecraftPath() {
        FileUtils.detectMinecraftPath()?.let {
            minecraftPath = it
        } ?: run {
            statusMessage = "⚠️ No se pudo detectar la carpeta .minecraft automáticamente"
        }
    }

    fun selectMrpackFile() {
        fileService.selectMrpackFile()?.let { file ->
            selectedMrpackFile = file
            loadModpackInfo()
        }
    }

    fun selectMinecraftPath() {
        fileService.selectMinecraftDirectory(minecraftPath)?.let { path ->
            minecraftPath = path
        }
    }

    private fun loadModpackInfo() {
        val file = selectedMrpackFile ?: return
        
        val result = modpackService.loadModpackInfo(file)
        result.fold(
            onSuccess = { (info, index) ->
                modpackInfo = info
                modpackIndex = index
                statusMessage = "✅ Modpack cargado correctamente"
            },
            onFailure = { e ->
                statusMessage = "❌ Error cargando modpack: ${e.message}"
                modpackInfo = null
                modpackIndex = null
            }
        )
    }

    suspend fun installModpack(): Result<Unit> = withContext(Dispatchers.IO) {
        val mcPath = minecraftPath ?: return@withContext Result.failure(IllegalStateException("No minecraft path selected"))
        val mrpackFile = selectedMrpackFile ?: return@withContext Result.failure(IllegalStateException("No mrpack file selected"))
        val info = modpackInfo ?: return@withContext Result.failure(IllegalStateException("No modpack info loaded"))
        val index = modpackIndex ?: return@withContext Result.failure(IllegalStateException("No modpack index loaded"))
        
        isInstalling = true
        installProgress = 0f
        statusMessage = "🔄 Preparando instalación..."
        
        return@withContext runCatching {
            installLoader(info, mcPath)
            fileService.ensureMinecraftDirectoryStructure(mcPath).getOrThrow()
            cleanModsFolder(mcPath)
            extractIncludedFiles(mrpackFile, mcPath)
            downloadAndInstallMods(index, mcPath)
        }.onSuccess {
            statusMessage = "✅ ¡Modpack instalado correctamente!"
        }.onFailure { e ->
            statusMessage = "❌ Error durante la instalación: ${e.message}"
            e.printStackTrace()
        }.also {
            isInstalling = false
        }
    }

    private fun installLoader(info: ModpackInfo, mcPath: Path) {
        if (info.loaderType.name == "NONE") {
            statusMessage = "⚠️ No se requiere loader para este modpack"
            return
        }
        
        statusMessage = "🔄 Instalando ${info.loaderType.name}..."
        
        val result = loaderInstallerService.installLoader(
            info.loaderType,
            info.minecraftVersion,
            info.loaderVersion,
            mcPath
        )
        
        result.fold(
            onSuccess = { statusMessage = "✅ ${info.loaderType.name} instalado correctamente" },
            onFailure = { e -> throw e }
        )
    }

    private fun cleanModsFolder(mcPath: Path) {
        statusMessage = "🔄 Limpiando carpeta de mods..."
        fileService.cleanModsFolder(mcPath).getOrThrow()
    }

    private fun extractIncludedFiles(mrpackFile: File, mcPath: Path) {
        statusMessage = "🔄 Extrayendo archivos de configuración..."
        modpackService.extractIncludedFiles(mrpackFile, mcPath).getOrThrow()
    }

    private fun downloadAndInstallMods(index: JsonNode, mcPath: Path) {
        val modFiles = modpackService.parseModFiles(index)
        
        if (modFiles.isEmpty()) {
            statusMessage = "⚠️ No se encontraron mods para descargar"
            return
        }
        
        val totalFiles = modFiles.size
        modFiles.forEachIndexed { index, modFile ->
            val fileNum = index + 1
            installProgress = fileNum.toFloat() / totalFiles
            statusMessage = "🔄 Descargando mod $fileNum/$totalFiles: ${Paths.get(modFile.path).fileName}"
            
            downloadService.downloadAndVerifyModFile(modFile, mcPath).getOrThrow()
        }
    }

    fun clear() {
        selectedMrpackFile = null
        modpackIndex = null
        modpackInfo = null
        statusMessage = ""
        installProgress = 0f
    }

    fun formatFileSize(bytes: Long): String {
        return FileUtils.formatFileSize(bytes)
    }

    fun canInstall(): Boolean {
        return selectedMrpackFile != null &&
                minecraftPath != null &&
                minecraftPath!!.exists() &&
                !isInstalling
    }

    fun canClear(): Boolean {
        return !isInstalling
    }
}