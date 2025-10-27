package org.crawkatt.mrpackinstaller.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.crawkatt.mrpackinstaller.data.*
import org.crawkatt.mrpackinstaller.services.*
import org.crawkatt.mrpackinstaller.utils.FileUtils
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

class ModpackInstallerViewModel {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    private val fileService = FileService()
    private val modpackService = ModpackService()
    private val downloadService = DownloadService()
    private val loaderInstallerService = LoaderInstallerService()
    private val apiService = ApiService()
    private val launcherService = MinecraftLauncherService()
    private val versionService = ModpackVersionService()

    var selectedMrpackFile by mutableStateOf<File?>(null)
    var minecraftPath by mutableStateOf<Path?>(null)
    var modpackIndex by mutableStateOf<JsonNode?>(null)
    var modpackInfo by mutableStateOf<ModpackInfo?>(null)
    var statusMessage by mutableStateOf("")
    var isInstalling by mutableStateOf(false)
    var installProgress by mutableStateOf(0f)
    var installMode by mutableStateOf(InstallMode.API_DOWNLOAD)
    var apiResponse by mutableStateOf<ApiResponse?>(null)
    var isLoadingModpackInfo by mutableStateOf(false)
    var showUpdateDialog by mutableStateOf(false)
    var updateDialogMessage by mutableStateOf("")

    init {
        detectMinecraftPath()
        loadModpackInfo()
    }

    private fun detectMinecraftPath() {
        FileUtils.detectMinecraftPath()?.let {
            minecraftPath = it
        } ?: run {
            statusMessage = "⚠️ No se pudo detectar la carpeta .minecraft automáticamente"
        }
    }

    fun switchInstallMode(mode: InstallMode) {
        installMode = mode
        clear()
        if (mode == InstallMode.API_DOWNLOAD) {
            loadModpackInfo()
        }
    }

    fun loadModpackInfo() {
        if (installMode == InstallMode.LOCAL_FILE) return
        if (isLoadingModpackInfo) return

        isLoadingModpackInfo = true
        statusMessage = "Cargando información del modpack..."

        viewModelScope.launch {
            apiService.getModpackInfo()
                .onSuccess { response ->
                    println("✅ API Response recibida: ${response.available}")
                    apiResponse = response
                    if (response.available) {
                        modpackInfo = ModpackInfo(
                            name = response.modpackInfo.name,
                            version = response.modpackInfo.versionId,
                            summary = response.modpackInfo.summary,
                            minecraftVersion = response.modpackInfo.minecraftVersion,
                            loader = "${response.modpackInfo.loader} ${response.modpackInfo.loaderVersion}",
                            loaderVersion = response.modpackInfo.loaderVersion,
                            loaderType = determineLoaderType(response.modpackInfo.loader),
                            modCount = response.modpackInfo.modCount,
                            totalSize = response.fileSize
                        )
                        println("✅ ModpackInfo creado: ${modpackInfo?.name}")
                        statusMessage = "Modpack disponible: ${response.modpackInfo.name}"
                        println("✅ Estado actualizado - modpackInfo: ${modpackInfo?.name}")
                    } else {
                        modpackInfo = null
                        statusMessage = "No hay modpack disponible en el servidor"
                        println("⚠️ No hay modpack disponible")
                    }
                }
                .onFailure { exception ->
                    println("❌ Error en API: ${exception.message}")
                    statusMessage = "Error al conectar con el servidor: ${exception.message}"
                    apiResponse = null
                    modpackInfo = null
                }
                .also {
                    isLoadingModpackInfo = false
                    println("✅ isLoadingModpackInfo = false")
                }
        }
    }

    private fun determineLoaderType(loader: String): LoaderType {
        return when {
            loader.contains("forge", ignoreCase = true) && !loader.contains("neo", ignoreCase = true) -> LoaderType.FORGE
            loader.contains("neoforge", ignoreCase = true) -> LoaderType.NEOFORGE
            loader.contains("fabric", ignoreCase = true) -> LoaderType.FABRIC
            loader.contains("quilt", ignoreCase = true) -> LoaderType.QUILT
            else -> LoaderType.NONE
        }
    }

    fun selectMrpackFile() {
        fileService.selectMrpackFile()?.let { file ->
            selectedMrpackFile = file
            loadLocalModpackInfo()
        }
    }

    fun selectMinecraftPath() {
        fileService.selectMinecraftDirectory(minecraftPath)?.let { path ->
            minecraftPath = path
        }
    }

    private fun loadLocalModpackInfo() {
        val file = selectedMrpackFile ?: return
        val result = modpackService.loadModpackInfo(file)
        result.fold(
            onSuccess = { (info, index) ->
                modpackInfo = info
                modpackIndex = index
                statusMessage = "✅ Modpack cargado correctamente"
            },
            onFailure = { why ->
                statusMessage = "❌ Error cargando modpack: ${why.message}"
                modpackInfo = null
                modpackIndex = null
            }
        )
    }

    suspend fun installModpack(): Result<Unit> = withContext(Dispatchers.IO) {
        val mcPath = minecraftPath ?: return@withContext Result.failure(IllegalStateException("No minecraft path selected"))
        val info = modpackInfo ?: return@withContext Result.failure(IllegalStateException("No modpack info loaded"))

        isInstalling = true
        installProgress = 0f
        statusMessage = "🔄 Preparando instalación..."

        return@withContext runCatching {
            statusMessage = "Verificando ruta de Minecraft..."
            fileService.ensureMinecraftDirectoryStructure(mcPath).getOrThrow()
            installProgress = 0.1f

            installLoader(info, mcPath)
            installProgress = 0.2f

            if (installMode == InstallMode.API_DOWNLOAD) {
                val response = apiResponse!!
                if (!response.available) {
                    throw java.io.IOException("No hay modpack disponible en el servidor")
                }

                val tempMrpackFile = kotlin.io.path.createTempFile("modpack", ".mrpack")

                statusMessage = "Descargando modpack desde el servidor..."
                var downloadProgress = 0f
                apiService.downloadModpack(tempMrpackFile) { downloaded, total ->
                    if (total > 0) {
                        downloadProgress = 0.2f + (0.2f * (downloaded.toFloat() / total.toFloat()))
                        installProgress = downloadProgress
                    }
                }.getOrThrow()
                installProgress = 0.4f

                val result = modpackService.loadModpackInfo(tempMrpackFile.toFile())
                result.onSuccess { (_, index) ->
                    modpackIndex = index
                    statusMessage = "Extrayendo archivos incluidos..."
                    modpackService.extractIncludedFiles(tempMrpackFile.toFile(), mcPath).getOrThrow()
                    installProgress = 0.5f

                    cleanModsFolder(mcPath)
                    downloadAndInstallMods(index, mcPath)

                    tempMrpackFile.toFile().delete()
                }.getOrThrow()
            } else {
                val mrpackFile = selectedMrpackFile ?: throw IllegalStateException("No mrpack file selected")
                val index = modpackIndex ?: throw IllegalStateException("No modpack index loaded")

                statusMessage = "Extrayendo archivos incluidos..."
                modpackService.extractIncludedFiles(mrpackFile, mcPath).getOrThrow()
                installProgress = 0.3f

                cleanModsFolder(mcPath)
                downloadAndInstallMods(index, mcPath)
            }

            val finalIndex = modpackIndex ?: throw IllegalStateException("No modpack index available")
            versionService.saveInstalledVersion(mcPath, info, finalIndex)

            statusMessage = "✅ ¡Modpack instalado correctamente!"
            installProgress = 1.0f
        }.onFailure { why ->
            statusMessage = "❌ Error durante la instalación: ${why.message}"
            why.printStackTrace()
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
            onFailure = { why -> throw why }
        )
    }

    private fun cleanModsFolder(mcPath: Path) {
        statusMessage = "🔄 Limpiando carpeta de mods..."
        fileService.cleanModsFolder(mcPath).getOrThrow()
    }

    private fun downloadAndInstallMods(index: JsonNode, mcPath: Path) {
        val modFiles = modpackService.parseModFiles(index)

        if (modFiles.isEmpty()) {
            statusMessage = "⚠️ No se encontraron mods para descargar"
            return
        }

        val totalFiles = modFiles.size
        val baseProgress = if (installMode == InstallMode.API_DOWNLOAD) 0.5f else 0.3f
        val progressRange = if (installMode == InstallMode.API_DOWNLOAD) 0.5f else 0.7f

        modFiles.forEachIndexed { index, modFile ->
            val fileNum = index + 1
            installProgress = baseProgress + (progressRange * (fileNum.toFloat() / totalFiles))
            statusMessage = "🔄 Descargando mod $fileNum/$totalFiles: ${Paths.get(modFile.path).fileName}"

            downloadService.downloadAndVerifyModFile(modFile, mcPath).getOrThrow()
        }
    }

    fun clear() {
        selectedMrpackFile = null
        apiResponse = null
        modpackIndex = null
        modpackInfo = null
        statusMessage = ""
        installProgress = 0f
    }

    fun formatFileSize(bytes: Long): String {
        return FileUtils.formatFileSize(bytes)
    }

    fun canClear(): Boolean {
        return installMode == InstallMode.LOCAL_FILE &&
               !isInstalling && 
               (selectedMrpackFile != null || statusMessage.isNotEmpty())
    }
    
    fun canPlay(): Boolean {
        return minecraftPath != null && 
               minecraftPath!!.exists() &&
               !isInstalling &&
               modpackInfo != null &&
               launcherService.isMinecraftLauncherInstalled()
    }
    
    fun playOrUpdate() {
        val mcPath = minecraftPath ?: return
        val info = modpackInfo ?: return

        if (installMode == InstallMode.LOCAL_FILE) {
            updateDialogMessage = "¿Deseas instalar ${info.name} ${info.version}?\n\n" +
                "⚠️ Se borrarán todos los mods actuales en la carpeta /mods"
            showUpdateDialog = true
            return
        }

        viewModelScope.launch {
            statusMessage = "Verificando versión del modpack..."
            
            val index = withContext(Dispatchers.IO) {
                // Operador `?:` = `if (foo != null) { }`
                modpackIndex ?: runCatching {
                    val tempMrpackFile = kotlin.io.path.createTempFile("modpack_check", ".mrpack")

                    apiService.downloadModpack(tempMrpackFile) { _, _ -> }.getOrThrow()

                    val result = modpackService.loadModpackInfo(tempMrpackFile.toFile())
                    val loadedIndex = result.getOrThrow().second

                    tempMrpackFile.toFile().delete()
                    loadedIndex
                }.onFailure { why ->
                    println("⚠️ Error descargando modpack para verificación: ${why.message}")
                }.getOrNull()
            }

            val comparison = withContext(Dispatchers.IO) {
                versionService.compareVersions(mcPath, info, index)
            }.getOrThrow()

            when {
                !comparison.isInstalled -> openConfirmDialog(info)
                comparison.needsUpdate -> updateModpack(comparison)
                else -> skipUpdate()
            }
        }
    }

    fun skipUpdate() {
        statusMessage = "Modpack actualizado, iniciando Minecraft..."
        launchMinecraftDirectly()
    }

    fun openConfirmDialog(info: ModpackInfo) {
        updateDialogMessage = "El modpack no está instalado.\n\n" +
                "¿Deseas instalar ${info.name} ${info.version}?\n\n" +
                "⚠️ Se borrarán todos los mods actuales en la carpeta /mods"
        showUpdateDialog = true
    }

    fun updateModpack(comparison: ModpackVersionService.VersionComparison) {
        val message = buildString {
            append("Se detectó una nueva versión del modpack:\n\n")
            append("Instalada: ${comparison.installedVersion}\n")
            append("Disponible: ${comparison.availableVersion}\n\n")
            if (comparison.modsChanged) {
                append("⚠️ Los mods han cambiado. Se borrarán todos los mods actuales en la carpeta /mods\n\n")
            }
            append("¿Deseas actualizar?")
        }
        updateDialogMessage = message
        showUpdateDialog = true
    }
    
    fun confirmUpdate() {
        showUpdateDialog = false
        viewModelScope.launch {
            installModpack().onSuccess {
                launchMinecraftDirectly()
            }
        }
    }
    
    fun cancelUpdate() {
        showUpdateDialog = false
        statusMessage = "Actualización cancelada"
    }

    private fun launchMinecraftDirectly() {
        val mcPath = minecraftPath ?: return
        val info = modpackInfo ?: return
        
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                launcherService.launchMinecraft(mcPath, info.name)
                    .onSuccess {
                        statusMessage = "🎮 Minecraft launcher iniciado"
                    }
                    .onFailure { exception ->
                        statusMessage = "❌ Error al iniciar Minecraft: ${exception.message}"
                    }
            }
        }
    }
}