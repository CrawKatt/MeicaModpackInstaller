package org.crawkatt.mrpackinstaller.data

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