package org.crawkatt.mrpackinstaller.services

import org.crawkatt.mrpackinstaller.utils.FileUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class FileService {
    fun selectMrpackFile(): File? {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Seleccionar archivo .mrpack"
            fileFilter = FileNameExtensionFilter("Modpack Files (*.mrpack)", "mrpack")
            isFileHidingEnabled = false
        }

        return if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            fileChooser.selectedFile
        } else {
            null
        }
    }

    fun selectMinecraftDirectory(currentPath: Path?): Path? {
        val dirChooser = JFileChooser().apply {
            dialogTitle = "Seleccionar carpeta .minecraft"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isFileHidingEnabled = false
            currentPath?.let { currentDirectory = it.toFile() }
        }

        return if (dirChooser.showDialog(null, "Seleccionar") == JFileChooser.APPROVE_OPTION) {
            dirChooser.selectedFile.toPath()
        } else {
            null
        }
    }

    fun cleanModsFolder(minecraftPath: Path): Result<Unit> {
        return runCatching {
            val modsFolder = minecraftPath.resolve("mods")

            if (!Files.exists(modsFolder)) {
                Files.createDirectories(modsFolder)
                return@runCatching
            }

            Files.walk(modsFolder, 1).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .filter { it.fileName.toString().endsWith(".jar", ignoreCase = true) }
                    .forEach { file ->
                        val deleted = FileUtils.deleteFileSafely(file)
                        deleted.onSuccess {
                            println("Deleted: ${file.fileName}")
                        }
                    }
            }
        }
    }

    fun ensureMinecraftDirectoryStructure(minecraftPath: Path): Result<Unit> {
        return runCatching {
            val requiredDirs = listOf(
                minecraftPath.resolve("mods"),
                minecraftPath.resolve("config"),
                minecraftPath.resolve("resourcepacks"),
                minecraftPath.resolve("shaderpacks")
            )

            requiredDirs.forEach { dir ->
                FileUtils.ensureDirectoryExists(dir)
            }
        }
    }
}

