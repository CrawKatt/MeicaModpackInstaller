package org.crawkatt.mrpackinstaller.services

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.crawkatt.mrpackinstaller.data.ModpackInfo
import java.nio.file.Path

class ModpackVersionService {
    private val objectMapper = ObjectMapper()

    data class VersionComparison(
        val isInstalled: Boolean,
        val needsUpdate: Boolean,
        val installedVersion: String?,
        val availableVersion: String?,
        val modsChanged: Boolean
    )

    fun compareVersions(minecraftPath: Path, newModpackInfo: ModpackInfo, newModpackIndex: JsonNode?): Result<VersionComparison> {
        val versionFile = minecraftPath.resolve(".mrpack_version.json").toFile()

        if (!versionFile.exists()) {
            return Result.success(VersionComparison(
                isInstalled = false,
                needsUpdate = false,
                installedVersion = null,
                availableVersion = newModpackInfo.version,
                modsChanged = false
            ))
        }

        return runCatching {
            val installedData = objectMapper.readTree(versionFile)
            val installedVersion = installedData.get("version")?.asText()
            val installedName = installedData.get("name")?.asText()
            val installedMods = installedData.get("mods")?.map { it.asText() }?.toSet() ?: emptySet()

            val newMods = newModpackIndex?.let { extractModsList(it) }
            val modsChanged = newMods?.let { installedMods != it } ?: false

            val versionChanged = installedVersion != newModpackInfo.version
            val nameChanged = installedName != newModpackInfo.name

            println("[DEBUG]: 🔍 Comparación de versiones:")
            println("[DEBUG]:   - Nombre: $installedName vs ${newModpackInfo.name} (cambió: $nameChanged)")
            println("[DEBUG]:   - Versión: $installedVersion vs ${newModpackInfo.version} (cambió: $versionChanged)")
            println("[DEBUG]:   - Mods instalados: ${installedMods.size}")
            println("[DEBUG]:   - Mods nuevos: ${newMods?.size ?: "desconocido"}")
            println("[DEBUG]:   - Mods cambiaron: $modsChanged")

            VersionComparison(
                isInstalled = true,
                needsUpdate = versionChanged || nameChanged || modsChanged,
                installedVersion = installedVersion,
                availableVersion = newModpackInfo.version,
                modsChanged = modsChanged
            )
        }.onFailure { why ->
            println("⚠️ Error leyendo versión instalada: ${why.message}")
            VersionComparison(
                isInstalled = false,
                needsUpdate = false,
                installedVersion = null,
                availableVersion = newModpackInfo.version,
                modsChanged = false
            )
        }
    }

    fun saveInstalledVersion(minecraftPath: Path, modpackInfo: ModpackInfo, modpackIndex: JsonNode) {
        val versionFile = minecraftPath.resolve(".mrpack_version.json").toFile()
        val modsList = extractModsList(modpackIndex)

        val versionData = objectMapper.createObjectNode().apply {
            put("name", modpackInfo.name)
            put("version", modpackInfo.version)
            put("minecraftVersion", modpackInfo.minecraftVersion)
            put("loader", modpackInfo.loader)
            put("installedAt", System.currentTimeMillis())
            putArray("mods").apply { modsList.forEach { add(it) } }
        }

        versionFile.writeText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(versionData))
        println("✅ Versión del modpack guardada: ${modpackInfo.name} ${modpackInfo.version}")
    }

    private fun extractModsList(modpackIndex: JsonNode): Set<String> {
        val mods = mutableSetOf<String>()
        val filesNode = modpackIndex.get("files")

        if (filesNode != null && filesNode.isArray) {
            filesNode.forEach { fileNode ->
                val path = fileNode.get("path")?.asText() ?: ""
                val downloads = fileNode.get("downloads")

                if (path.startsWith("mods/") && downloads != null && downloads.isArray && downloads.size() > 0) {
                    val url = downloads[0].asText()
                    mods.add(url)
                }
            }
        }

        return mods
    }

    fun getInstalledModpackInfo(minecraftPath: Path): Result<ModpackInfo>? {
        val versionFile = minecraftPath.resolve(".mrpack_version.json").toFile()
        if (!versionFile.exists()) return null

        return runCatching {
            val data = objectMapper.readTree(versionFile)
            ModpackInfo(
                name = data.get("name")?.asText() ?: "",
                version = data.get("version")?.asText() ?: "",
                summary = "",
                minecraftVersion = data.get("minecraftVersion")?.asText() ?: "",
                loader = data.get("loader")?.asText() ?: "",
                loaderVersion = "",
                loaderType = org.crawkatt.mrpackinstaller.data.LoaderType.NONE,
                modCount = data.get("mods")?.size() ?: 0,
                totalSize = 0
            )
        }
    }
}
