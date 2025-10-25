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
    val summary: String,
    val minecraftVersion: String,
    val loader: String,
    val loaderVersion: String,
    val loaderType: LoaderType,
    val modCount: Int,
    val totalSize: Long
)

data class ApiResponse(
    @JsonProperty("available") val available: Boolean,
    @JsonProperty("file_name") val fileName: String,
    @JsonProperty("file_size") val fileSize: Long,
    @JsonProperty("file_size_mb") val fileSizeMb: Double,
    @JsonProperty("modpack_info") val modpackInfo: ApiModpackInfo
)

data class ApiModpackInfo(
    @JsonProperty("name") val name: String,
    @JsonProperty("summary") val summary: String,
    @JsonProperty("version_id") val version_id: String,
    @JsonProperty("format_version") val format_version: Int,
    @JsonProperty("minecraft_version") val minecraft_version: String,
    @JsonProperty("loader") val loader: String,
    @JsonProperty("loader_version") val loader_version: String,
    @JsonProperty("mod_count") val mod_count: Int,
    @JsonProperty("mods") val mods: List<ApiMod>
)

data class ApiMod(
    @JsonProperty("name") val name: String,
    @JsonProperty("file_size") val file_size: Long,
    @JsonProperty("environment") val environment: String
)

data class ApiModpack(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val minecraftVersion: String,
    val loader: String,
    val loaderVersion: String,
    val modCount: Int,
    val fileSize: Long,
    val imageUrl: String? = null,
    val author: String? = null,
    val downloadUrl: String
)

enum class LoaderType {
    FORGE, FABRIC, NEOFORGE, QUILT, NONE
}

enum class InstallMode {
    LOCAL_FILE, API_DOWNLOAD
}
