package org.crawkatt.mrpackinstaller.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.crawkatt.mrpackinstaller.config.AppConfig
import org.crawkatt.mrpackinstaller.data.ApiResponse
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Path
import kotlin.jvm.java

class ApiService {
    private val objectMapper = ObjectMapper()
    private val baseUrl: String = AppConfig.apiBaseUrl

    companion object {
        private const val USER_AGENT = "MrpackInstaller/1.0"
        private const val CONNECTION_TIMEOUT = 30000
        private const val READ_TIMEOUT = 30000
    }

    fun downloadModpack(targetPath: Path, onProgress: (downloaded: Long, total: Long) -> Unit): Result<Unit> {
        return runCatching {
            println("🔍 Intentando descargar desde: $baseUrl/download")
            val connection = createConnection("$baseUrl/download")
            
            println("📡 Código de respuesta: ${connection.responseCode}")
            println("📡 Headers: ${connection.headerFields}")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                val errorMessage = runCatching {
                    connection.errorStream?.use { it.readBytes().toString(Charsets.UTF_8) } ?: "Sin mensaje de error"
                }.onFailure { why ->
                    "Error leyendo mensaje de error: ${why.message}"
                }

                throw IOException("Error descargando modpack (Status: ${connection.responseCode})\nError del servidor: $errorMessage")
            }

            targetPath.parent?.toFile()?.mkdirs()
            val totalSize = connection.contentLengthLong
            println("📦 Tamaño del archivo: $totalSize bytes")

            var downloaded: Long = 0
            connection.inputStream.use { input ->
                targetPath.toFile().outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead = input.read(buffer)
                    while (bytesRead != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        onProgress(downloaded, totalSize)
                        bytesRead = input.read(buffer)
                    }
                }
            }
            println("✅ Descarga completada: $downloaded bytes")
        }.onFailure { exception ->
            println("❌ Error en downloadModpack: ${exception.message}")
            exception.printStackTrace()
        }
    }

    fun getModpackInfo(): Result<ApiResponse> {
        return runCatching {
            println("🔍 Intentando conectar a: $baseUrl/info")
            val connection = createConnection("$baseUrl/info")
            
            println("📡 Código de respuesta: ${connection.responseCode}")
            println("📡 Headers: ${connection.headerFields}")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                val errorMessage = runCatching {
                    connection.errorStream?.use { it.readBytes().toString(Charsets.UTF_8) } ?: "Sin mensaje de error"
                }.onFailure { why ->
                    "Error leyendo mensaje de error: ${why.message}"
                }

                throw IOException("Error al obtener información del modpack (Status: ${connection.responseCode})\nError del servidor: $errorMessage")
            }

            val response = connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            println("📦 Respuesta del servidor: $response")
            objectMapper.readValue(response, ApiResponse::class.java)
        }.onFailure { why ->
            println("❌ Error en getModpackInfo: ${why.message}")
            why.printStackTrace()
        }
    }

    private fun createConnection(url: String): HttpURLConnection {
        val connection = URI.create(url).toURL().openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = CONNECTION_TIMEOUT
        connection.readTimeout = READ_TIMEOUT
        return connection
    }
}
