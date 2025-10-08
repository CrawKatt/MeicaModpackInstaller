package org.crawkatt.mrpackinstaller

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.io.path.exists
import kotlin.math.ln
import kotlin.math.pow

data class ModFile(
    val path: String,
    val downloadUrl: String,
    val sha1: String,
    val fileSize: Long
)

data class ModpackInfo(
    val name: String,
    val version: String,
    val summary: String,
    val minecraftVersion: String,
    val loader: String,
    val loaderVersion: String,
    val loaderType: LoaderType,
    val modCount: Int,
    val totalSize: Long
)

enum class LoaderType {
    FORGE, FABRIC, NEOFORGE, QUILT, NONE
}

class App {
    private val objectMapper = ObjectMapper()

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
        val os = System.getProperty("os.name").lowercase()
        val userHome = System.getProperty("user.home")
        val defaultPath = when {
            os.contains("win") -> Paths.get(userHome, "AppData", "Roaming", ".minecraft")
            os.contains("mac") -> Paths.get(userHome, "Library", "Application Support", "minecraft")
            else -> Paths.get(userHome, ".minecraft")
        }

        if (defaultPath.exists()) {
            minecraftPath = defaultPath
        } else {
            statusMessage = "⚠️ No se pudo detectar la carpeta .minecraft automáticamente"
        }
    }

    fun selectMrpackFile() {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Seleccionar archivo .mrpack"
            fileFilter = FileNameExtensionFilter("Modpack Files (*.mrpack)", "mrpack")
            isFileHidingEnabled = false
        }

        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            selectedMrpackFile = fileChooser.selectedFile
            loadModpackInfo()
        }
    }

    fun selectMinecraftPath() {
        val dirChooser = JFileChooser().apply {
            dialogTitle = "Seleccionar carpeta .minecraft"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isFileHidingEnabled = false
            minecraftPath?.let { currentDirectory = it.toFile() }
        }

        if (dirChooser.showDialog(null, "Seleccionar") == JFileChooser.APPROVE_OPTION) {
            minecraftPath = dirChooser.selectedFile.toPath()
        }
    }

    private fun loadModpackInfo(): Result<ModpackInfo> {
        val file = selectedMrpackFile ?: return Result.failure(IllegalStateException("No hay archivo seleccionado"))

        return runCatching {
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "modrinth.index.json") {
                        val baos = ByteArrayOutputStream()
                        val buffer = ByteArray(1024)
                        var len = zis.read(buffer)
                        while (len > 0) {
                            baos.write(buffer, 0, len)
                            len = zis.read(buffer)
                        }

                        val root = objectMapper.readTree(baos.toByteArray())
                        modpackIndex = root

                        val info = parseModpackInfo(root)
                        modpackInfo = info
                        return@runCatching info
                    }
                    entry = zis.nextEntry
                }
            }
            throw IOException("No se encontró modrinth.index.json en el .mrpack")
        }
    }

    private fun parseModpackInfo(root: JsonNode): ModpackInfo {
        val files = root.path("files")
        val modCount = if (files.isArray) files.size() else 0

        var totalSize = 0L
        if (files.isArray) {
            files.forEach { file ->
                totalSize += file.path("fileSize").asLong(0)
            }
        }

        val dependencies = root.path("dependencies")
        val forge = dependencies.path("forge")
        val fabric = dependencies.path("fabric-loader")
        val quilt = dependencies.path("quilt-loader")
        val neoforge = dependencies.path("neoforge")

        val loaderType: LoaderType
        val loaderVersion: String
        val loaderDisplay: String

        when {
            !forge.isMissingNode -> {
                loaderType = LoaderType.FORGE
                loaderVersion = forge.asText()
                loaderDisplay = "Forge: $loaderVersion"
            }

            !neoforge.isMissingNode -> {
                loaderType = LoaderType.NEOFORGE
                loaderVersion = neoforge.asText()
                loaderDisplay = "NeoForge: $loaderVersion"
            }

            !fabric.isMissingNode -> {
                loaderType = LoaderType.FABRIC
                loaderVersion = fabric.asText()
                loaderDisplay = "Fabric: $loaderVersion"
            }

            !quilt.isMissingNode -> {
                loaderType = LoaderType.QUILT
                loaderVersion = quilt.asText()
                loaderDisplay = "Quilt: $loaderVersion"
            }

            else -> {
                loaderType = LoaderType.NONE
                loaderVersion = ""
                loaderDisplay = "No especificado"
            }
        }

        return ModpackInfo(
            name = root.path("name").asText("No especificado"),
            version = root.path("versionId").asText("No especificada"),
            summary = root.path("summary").asText("No disponible"),
            minecraftVersion = dependencies.path("minecraft").asText("No especificada"),
            loader = loaderDisplay,
            loaderVersion = loaderVersion,
            loaderType = loaderType,
            modCount = modCount,
            totalSize = totalSize
        )
    }

    suspend fun installModpack(): Result<Unit> = withContext(Dispatchers.IO) {
        isInstalling = true
        installProgress = 0f
        statusMessage = "🔄 Preparando instalación..."

        runCatching {
            installLoader()
            cleanModsFolder()
            extractIncludedFiles()
            downloadAndInstallMods()
        }
            .onSuccess {
                statusMessage = "✅ ¡Modpack instalado correctamente!"
            }
            .onFailure { e ->
                statusMessage = "❌ Error durante la instalación: ${e.message}"
                e.printStackTrace()
            }
            .also {
                isInstalling = false
            }
    }

    private fun installLoader() {
        val info = modpackInfo ?: throw IOException("No hay información del modpack")

        if (info.loaderType == LoaderType.NONE) {
            statusMessage = "⚠️ No se requiere loader para este modpack"
            return
        }

        statusMessage = "🔄 Instalando ${info.loaderType.name}..."

        when (info.loaderType) {
            LoaderType.FORGE -> installForge(info.minecraftVersion, info.loaderVersion)
            LoaderType.NEOFORGE -> installNeoForge(info.loaderVersion)
            LoaderType.FABRIC -> installFabric(info.minecraftVersion, info.loaderVersion)
            LoaderType.QUILT -> installQuilt(info.minecraftVersion, info.loaderVersion)
            else -> {}
        }
    }

    private fun installForge(mcVersion: String, forgeVersion: String) {
        installWithInstaller(
            "Forge",
            "https://maven.minecraftforge.net/net/minecraftforge/forge/$mcVersion-$forgeVersion/forge-$mcVersion-$forgeVersion-installer.jar"
        ) {
            listOf("--installClient", it.toString())
        }
    }

    private fun installNeoForge(neoforgeVersion: String) {
        installWithInstaller(
            "NeoForge",
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/$neoforgeVersion/neoforge-$neoforgeVersion-installer.jar"
        ) {
            listOf("--installClient", it.toString())
        }
    }

    private fun installFabric(mcVersion: String, fabricVersion: String): Result<Unit> {
        val url = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.1/fabric-installer-1.0.1.jar"
        val launcherDir = minecraftPath?.parent ?: minecraftPath!!

        fun args(dir: Path) = listOf(
            "client",
            "-mcversion", mcVersion,
            "-loader", fabricVersion,
            "-dir", dir.toString()
        )

        return installWithInstaller("Fabric", url) { args(launcherDir) }
            .recoverCatching {
                println("Primer intento falló, probando con directorio .minecraft...")
                installWithInstaller("Fabric", url) { args(minecraftPath!!) }.getOrThrow()
            }
    }

    private fun installQuilt(mcVersion: String, quiltVersion: String) {
        installWithInstaller(
            "Quilt",
            "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/0.9.1/quilt-installer-0.9.1.jar"
        ) {
            listOf(
                "install", "client", mcVersion, quiltVersion,
                "--install-dir=$minecraftPath",
                "--no-profile"
            )
        }
    }

    private fun installWithInstaller(
        name: String,
        url: String,
        argsProvider: (Path) -> List<String>
    ): Result<Unit> {
        val tempDir = Files.createTempDirectory("${name.lowercase()}_installer")
        val installerFile = tempDir.resolve("$name-installer.jar")

        return runCatching {
            statusMessage = "🔄 Descargando instalador de $name..."
            downloadFile(url, installerFile)

            statusMessage = "🔄 Ejecutando instalador de $name..."
            val process = ProcessBuilder(
                listOf("java", "-jar", installerFile.toString()) + argsProvider(minecraftPath!!)
            ).redirectErrorStream(true).start()

            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.lineSequence().forEach { line ->
                    println("$name: $line")
                }
            }

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw IOException("El instalador de $name falló con código: $exitCode")
            }

            statusMessage = "✅ $name instalado correctamente"
        }.onFailure { e ->
            statusMessage = "❌ Error instalando $name: ${e.message}"
        }.also {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
    }

    private fun downloadFile(url: String, targetPath: Path) {
        val connection = URI.create(url).toURL().openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "MrpackInstaller/1.0")
        connection.connectTimeout = 30000
        connection.readTimeout = 30000

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException("Error al descargar: $url (Código: ${connection.responseCode})")
        }

        connection.inputStream.use { input ->
            FileOutputStream(targetPath.toFile()).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead = input.read(buffer)
                while (bytesRead != -1) {
                    output.write(buffer, 0, bytesRead)
                    bytesRead = input.read(buffer)
                }
            }
        }
    }

    private fun cleanModsFolder() {
        statusMessage = "🔄 Limpiando carpeta de mods..."

        val modsFolder = minecraftPath?.resolve("mods") ?: return

        if (!modsFolder.exists()) {
            Files.createDirectories(modsFolder)
            return
        }

        Files.walk(modsFolder, 1).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .filter { it.fileName.toString().endsWith(".jar", ignoreCase = true) }
                .forEach { file ->
                    try {
                        Files.delete(file)
                        println("Eliminado: ${file.fileName}")
                    } catch (e: Exception) {
                        println("No se pudo eliminar: ${file.fileName} - ${e.message}")
                    }
                }
        }
    }

    private fun extractIncludedFiles() {
        statusMessage = "🔄 Extrayendo archivos de configuración..."

        selectedMrpackFile?.let { file ->
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val entryName = entry.name
                        if (entryName != "modrinth.index.json" && !entryName.startsWith("META-INF/")) {
                            val targetPath = minecraftPath!!.resolve(entryName)
                            Files.createDirectories(targetPath.parent)

                            FileOutputStream(targetPath.toFile()).use { fos ->
                                val buffer = ByteArray(1024)
                                var len = zis.read(buffer)
                                while (len > 0) {
                                    fos.write(buffer, 0, len)
                                    len = zis.read(buffer)
                                }
                            }
                        }
                    }
                    entry = zis.nextEntry
                }
            }
        }
    }

    private fun downloadAndInstallMods() {
        val index = modpackIndex ?: throw IOException("No se ha cargado información del modpack")
        val files = index.path("files")

        if (!files.isArray) {
            throw IOException("No se encontraron archivos para descargar")
        }

        val modFiles = mutableListOf<ModFile>()
        files.forEach { file ->
            val downloads = file.path("downloads")
            if (downloads.isArray && downloads.size() > 0) {
                modFiles.add(
                    ModFile(
                        path = file.path("path").asText(),
                        sha1 = file.path("hashes").path("sha1").asText(),
                        fileSize = file.path("fileSize").asLong(),
                        downloadUrl = downloads[0].asText()
                    )
                )
            }
        }

        val totalFiles = modFiles.size
        modFiles.forEachIndexed { index, modFile ->
            val fileNum = index + 1
            installProgress = fileNum.toFloat() / totalFiles
            statusMessage = "🔄 Descargando mod $fileNum/$totalFiles: ${Paths.get(modFile.path).fileName}"

            downloadAndVerifyFile(modFile)
        }
    }

    private fun downloadAndVerifyFile(modFile: ModFile) {
        val targetPath = minecraftPath!!.resolve(modFile.path)
        Files.createDirectories(targetPath.parent)

        val url = URI.create(modFile.downloadUrl).toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "MrpackInstaller/1.0")
        connection.connectTimeout = 30000
        connection.readTimeout = 30000

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException("Error al descargar: ${modFile.downloadUrl} (Código: ${connection.responseCode})")
        }

        connection.inputStream.use { input ->
            FileOutputStream(targetPath.toFile()).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead = input.read(buffer)
                while (bytesRead != -1) {
                    output.write(buffer, 0, bytesRead)
                    bytesRead = input.read(buffer)
                }
            }
        }

        if (verifyFileHash(targetPath, modFile.sha1).isFailure) {
            Files.deleteIfExists(targetPath)
            throw IOException("Hash incorrecto para el archivo: ${modFile.path}")
        }
    }

    private fun verifyFileHash(filePath: Path, expectedSha1: String): Result<Boolean> {
        return runCatching {
            val digest = MessageDigest.getInstance("SHA-1")
            Files.newInputStream(filePath).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead = fis.read(buffer)
                while (bytesRead != -1) {
                    digest.update(buffer, 0, bytesRead)
                    bytesRead = fis.read(buffer)
                }
            }

            val hashBytes = digest.digest()
            val sb = StringBuilder()
            hashBytes.forEach { b ->
                sb.append(String.format("%02x", b))
            }

            sb.toString() == expectedSha1
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
        if (bytes < 1024) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format("%.1f %sB", bytes / 1024.0.pow(exp.toDouble()), pre)
    }
}

@Composable
@Preview
fun ModpackInstallerApp() {
    val viewModel = remember { App() }
    val scope = rememberCoroutineScope()

    val darkColors = darkColors(
        primary = Color(0xFF1DB954),
        primaryVariant = Color(0xFF1AA34A),
        secondary = Color(0xFF03DAC6),
        secondaryVariant = Color(0xFF018786),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        error = Color(0xFFCF6679),
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onBackground = Color(0xFFE1E1E1),
        onSurface = Color(0xFFE1E1E1),
        onError = Color.Black
    )

    MaterialTheme(colors = darkColors) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Instalador de Modpacks .mrpack",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold
                )

                Divider()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp,
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Archivo .mrpack", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = viewModel.selectedMrpackFile?.absolutePath ?: "",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("Selecciona un archivo...") },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = MaterialTheme.colors.onSurface,
                                    backgroundColor = Color(0xFF2A2A2A),
                                    cursorColor = MaterialTheme.colors.primary,
                                    focusedBorderColor = MaterialTheme.colors.primary,
                                    unfocusedBorderColor = Color(0xFF3A3A3A)
                                )
                            )
                            Button(
                                onClick = { viewModel.selectMrpackFile() },
                                enabled = !viewModel.isInstalling
                            ) {
                                Text("Examinar")
                            }
                        }
                    }
                }

                if (viewModel.modpackInfo != null) {
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

                            viewModel.modpackInfo?.let { info ->
                                InfoRow("Nombre:", info.name)
                                InfoRow("Versión:", info.version)
                                InfoRow("Resumen:", info.summary)
                                InfoRow("Versión de Minecraft:", info.minecraftVersion)
                                InfoRow("Loader:", info.loader)
                                InfoRow("Número de mods:", info.modCount.toString())
                                if (info.totalSize > 0) {
                                    InfoRow("Tamaño total aprox:", viewModel.formatFileSize(info.totalSize))
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp,
                    backgroundColor = MaterialTheme.colors.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Carpeta .minecraft", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = viewModel.minecraftPath?.toString() ?: "",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("Ruta de .minecraft") },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = MaterialTheme.colors.onSurface,
                                    backgroundColor = Color(0xFF2A2A2A),
                                    cursorColor = MaterialTheme.colors.primary,
                                    focusedBorderColor = MaterialTheme.colors.primary,
                                    unfocusedBorderColor = Color(0xFF3A3A3A)
                                )
                            )
                            Button(
                                onClick = { viewModel.selectMinecraftPath() },
                                enabled = !viewModel.isInstalling
                            ) {
                                Text("Cambiar")
                            }
                        }
                    }
                }

                if (viewModel.isInstalling) {
                    LinearProgressIndicator(
                        progress = viewModel.installProgress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colors.primary
                    )
                }

                if (viewModel.statusMessage.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFF2A2A2A),
                        elevation = 2.dp
                    ) {
                        Text(
                            text = viewModel.statusMessage,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colors.onSurface
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.clear() },
                        enabled = !viewModel.isInstalling,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colors.primary
                        )
                    ) {
                        Text("Limpiar")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.installModpack()
                            }
                        },
                        enabled = viewModel.selectedMrpackFile != null &&
                                viewModel.minecraftPath != null &&
                                viewModel.minecraftPath!!.exists() &&
                                !viewModel.isInstalling,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary
                        )
                    ) {
                        Text("Instalar", color = Color.White)
                    }
                }
            }
        }
    }
}

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