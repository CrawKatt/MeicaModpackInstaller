package org.crawkatt.mrpackinstaller.services

import org.crawkatt.mrpackinstaller.data.ModFile
import org.crawkatt.mrpackinstaller.utils.FileUtils
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

class DownloadService {
    
    companion object {
        private const val USER_AGENT = "MrpackInstaller/1.0"
        private const val CONNECTION_TIMEOUT = 30000
        private const val READ_TIMEOUT = 30000
        private const val BUFFER_SIZE = 8192
    }

    fun downloadFile(url: String, targetPath: Path): Result<Unit> {
        return runCatching {
            val connection = createConnection(url)
            
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Error downloading: $url (Status: ${connection.responseCode})")
            }
            
            FileUtils.ensureDirectoryExists(targetPath.parent)
            
            connection.inputStream.use { input ->
                FileOutputStream(targetPath.toFile()).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead = input.read(buffer)
                    while (bytesRead != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesRead = input.read(buffer)
                    }
                }
            }
        }
    }

    fun downloadAndVerifyModFile(modFile: ModFile, minecraftPath: Path): Result<Unit> {
        return runCatching {
            val targetPath = minecraftPath.resolve(modFile.path)
            downloadFile(modFile.downloadUrl, targetPath).getOrThrow()

            val hashVerification = FileUtils.verifyFileHash(targetPath, modFile.sha1)
            if (hashVerification.isFailure || !hashVerification.getOrElse { false }) {
                Files.deleteIfExists(targetPath)
                throw IOException("Hash verification failed for file: ${modFile.path}")
            }
        }
    }

    private fun createConnection(url: String): HttpURLConnection {
        val connection = URI.create(url).toURL().openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.connectTimeout = CONNECTION_TIMEOUT
        connection.readTimeout = READ_TIMEOUT
        return connection
    }
}