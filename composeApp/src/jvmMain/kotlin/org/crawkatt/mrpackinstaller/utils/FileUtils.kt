package org.crawkatt.mrpackinstaller.utils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import kotlin.io.path.exists
import kotlin.math.ln
import kotlin.math.pow

object FileUtils {
    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format("%.1f %sB", bytes / 1024.0.pow(exp.toDouble()), pre)
    }

    fun detectMinecraftPath(): Path? {
        val os = System.getProperty("os.name").lowercase()
        val userHome = System.getProperty("user.home")
        val defaultPath = when {
            os.contains("win") -> Paths.get(userHome, "AppData", "Roaming", ".minecraft")
            os.contains("mac") -> Paths.get(userHome, "Library", "Application Support", "minecraft")
            else -> Paths.get(userHome, ".minecraft")
        }
        
        return if (defaultPath.exists()) defaultPath else null
    }

    fun verifyFileHash(filePath: Path, expectedSha1: String): Result<Boolean> {
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

    fun ensureDirectoryExists(path: Path) {
        if (!Files.exists(path)) {
            Files.createDirectories(path)
        }
    }

    fun deleteFileSafely(path: Path): Result<Boolean> {
        return runCatching {
            Files.deleteIfExists(path)
        }.onFailure { why ->
            println("Could not delete file: ${path.fileName} - ${why.message}")
        }
    }
}