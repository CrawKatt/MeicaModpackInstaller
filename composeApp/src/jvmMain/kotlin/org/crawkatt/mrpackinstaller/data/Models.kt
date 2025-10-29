package org.crawkatt.mrpackinstaller.data

import com.fasterxml.jackson.annotation.JsonProperty

data class ModFile(
    val path: String,
    val downloadUrl: String,
    val sha1: String,
    val fileSize: Long
)

data class ModpackInfo(
    val name: String,
    val version: String,
    val summary: String?,
    val minecraftVersion: String,
    val loader: String,
    val loaderVersion: String,
    val loaderType: LoaderType,
    val modCount: Int,
    val totalSize: Long
)

data class ApiResponse(
    @param:JsonProperty("available") val available: Boolean,
    @param:JsonProperty("file_name") val fileName: String,
    @param:JsonProperty("file_size") val fileSize: Long,
    @param:JsonProperty("file_size_mb") val fileSizeMb: Double,
    @param:JsonProperty("modpack_info") val modpackInfo: ApiModpackInfo
)

data class ApiModpackInfo(
    @param:JsonProperty("name") val name: String,
    @param:JsonProperty("summary") val summary: String?,
    @param:JsonProperty("version_id") val versionId: String,
    @param:JsonProperty("format_version") val formatVersion: Int,
    @param:JsonProperty("minecraft_version") val minecraftVersion: String,
    @param:JsonProperty("loader") val loader: String,
    @param:JsonProperty("loader_version") val loaderVersion: String,
    @param:JsonProperty("mod_count") val modCount: Int,
    @param:JsonProperty("mods") val mods: List<ApiMod>
)

data class ApiMod(
    @param:JsonProperty("name") val name: String,
    @param:JsonProperty("file_size") val fileSize: Long,
    @param:JsonProperty("environment") val environment: String
)

enum class LoaderType {
    FORGE, FABRIC, NEOFORGE, QUILT, NONE
}

enum class InstallMode {
    LOCAL_FILE, API_DOWNLOAD
}
