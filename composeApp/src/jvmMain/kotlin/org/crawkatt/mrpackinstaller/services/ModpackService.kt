package org.crawkatt.mrpackinstaller.services

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.crawkatt.mrpackinstaller.data.LoaderType
import org.crawkatt.mrpackinstaller.data.ModFile
import org.crawkatt.mrpackinstaller.data.ModpackInfo
import org.crawkatt.mrpackinstaller.utils.FileUtils
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream

class ModpackService {
    private val objectMapper = ObjectMapper()

    fun loadModpackInfo(mrpackFile: File): Result<Pair<ModpackInfo, JsonNode>> {
        return runCatching {
            ZipInputStream(FileInputStream(mrpackFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "modrinth.index.json") {
                        val indexContent = readZipEntry(zis)
                        val root = objectMapper.readTree(indexContent)
                        val info = parseModpackInfo(root)
                        return@runCatching Pair(info, root)
                    }
                    entry = zis.nextEntry
                }
            }
            throw IOException("modrinth.index.json not found in .mrpack file")
        }
    }

    fun extractIncludedFiles(mrpackFile: File, minecraftPath: Path): Result<Unit> {
        return runCatching {
            ZipInputStream(FileInputStream(mrpackFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val entryName = entry.name
                        if (entryName != "modrinth.index.json" && !entryName.startsWith("META-INF/")) {
                            val targetPath = minecraftPath.resolve(entryName)
                            FileUtils.ensureDirectoryExists(targetPath.parent)
                            
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

    fun parseModFiles(modpackIndex: JsonNode): List<ModFile> {
        val files = modpackIndex.path("files")
        if (!files.isArray) return emptyList()
        
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
        return modFiles
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
        val (loaderType, loaderVersion, loaderDisplay) = determineLoader(dependencies)
        
        return ModpackInfo(
            name = root.path("name").asText("No specified"),
            version = root.path("versionId").asText("No specified"),
            summary = root.path("summary").asText("Not available"),
            minecraftVersion = dependencies.path("minecraft").asText("No specified"),
            loader = loaderDisplay,
            loaderVersion = loaderVersion,
            loaderType = loaderType,
            modCount = modCount,
            totalSize = totalSize
        )
    }

    private fun determineLoader(dependencies: JsonNode): Triple<LoaderType, String, String> {
        val forge = dependencies.path("forge")
        val fabric = dependencies.path("fabric-loader")
        val quilt = dependencies.path("quilt-loader")
        val neoforge = dependencies.path("neoforge")
        
        return when {
            !forge.isMissingNode -> {
                val version = forge.asText()
                Triple(LoaderType.FORGE, version, "Forge: $version")
            }
            !neoforge.isMissingNode -> {
                val version = neoforge.asText()
                Triple(LoaderType.NEOFORGE, version, "NeoForge: $version")
            }
            !fabric.isMissingNode -> {
                val version = fabric.asText()
                Triple(LoaderType.FABRIC, version, "Fabric: $version")
            }
            !quilt.isMissingNode -> {
                val version = quilt.asText()
                Triple(LoaderType.QUILT, version, "Quilt: $version")
            }
            else -> {
                Triple(LoaderType.NONE, "", "Not specified")
            }
        }
    }

    private fun readZipEntry(zis: ZipInputStream): ByteArray {
        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var len = zis.read(buffer)
        while (len > 0) {
            baos.write(buffer, 0, len)
            len = zis.read(buffer)
        }
        return baos.toByteArray()
    }
}