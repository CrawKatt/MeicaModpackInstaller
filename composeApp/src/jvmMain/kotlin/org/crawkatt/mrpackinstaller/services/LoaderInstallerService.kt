package org.crawkatt.mrpackinstaller.services

import org.crawkatt.mrpackinstaller.data.LoaderType
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class LoaderInstallerService {
    private val downloadService = DownloadService()

    fun installLoader(loaderType: LoaderType, minecraftVersion: String, loaderVersion: String, minecraftPath: Path): Result<Unit> {
        return when (loaderType) {
            LoaderType.FORGE -> installForge(minecraftVersion, loaderVersion, minecraftPath)
            LoaderType.NEOFORGE -> installNeoForge(loaderVersion, minecraftPath)
            LoaderType.FABRIC -> installFabric(minecraftVersion, loaderVersion, minecraftPath)
            LoaderType.QUILT -> installQuilt(minecraftVersion, loaderVersion, minecraftPath)
            LoaderType.NONE -> Result.success(Unit)
        }
    }

    private fun installForge(mcVersion: String, forgeVersion: String, minecraftPath: Path): Result<Unit> {
        val url = "https://maven.minecraftforge.net/net/minecraftforge/forge/$mcVersion-$forgeVersion/forge-$mcVersion-$forgeVersion-installer.jar"
        return installWithInstaller(
            name = "Forge",
            url = url,
            minecraftPath = minecraftPath
        ) { listOf("--installClient", it.toString()) }
    }

    private fun installNeoForge(neoforgeVersion: String, minecraftPath: Path): Result<Unit> {
        val url = "https://maven.neoforged.net/releases/net/neoforged/neoforge/$neoforgeVersion/neoforge-$neoforgeVersion-installer.jar"
        return installWithInstaller(
            name = "NeoForge",
            url = url,
            minecraftPath = minecraftPath
        ) { listOf("--installClient", it.toString()) }
    }

    private fun installFabric(mcVersion: String, fabricVersion: String, minecraftPath: Path): Result<Unit> {
        val url = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.1/fabric-installer-1.0.1.jar"
        val launcherDir = minecraftPath.parent ?: minecraftPath
        
        val argsProvider: (Path) -> List<String> = { dir ->
            listOf(
                "client",
                "-mcversion", mcVersion,
                "-loader", fabricVersion,
                "-dir", dir.toString()
            )
        }
        
        return installWithInstaller("Fabric", url, minecraftPath) { argsProvider(launcherDir) }
            .recoverCatching {
                println("First attempt failed, trying with .minecraft directory...")
                installWithInstaller("Fabric", url, minecraftPath) { argsProvider(minecraftPath) }.getOrThrow()
            }
    }

    private fun installQuilt(mcVersion: String, quiltVersion: String, minecraftPath: Path): Result<Unit> {
        val url = "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/0.9.1/quilt-installer-0.9.1.jar"
        return installWithInstaller(
            name = "Quilt",
            url = url,
            minecraftPath = minecraftPath
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
        minecraftPath: Path,
        argsProvider: (Path) -> List<String>
    ): Result<Unit> {
        val tempDir = Files.createTempDirectory("${name.lowercase()}_installer")
        val installerFile = tempDir.resolve("$name-installer.jar")
        
        return runCatching {
            downloadService.downloadFile(url, installerFile).getOrThrow()
            val command = listOf("java", "-jar", installerFile.toString()) + argsProvider(minecraftPath)

            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.lineSequence().forEach { line ->
                    println("$name: $line")
                }
            }
            
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw IOException("$name installer failed with exit code: $exitCode")
            }
        }.onFailure { e ->
            println("Error installing $name: ${e.message}")
        }.also {
            cleanupTempDirectory(tempDir)
        }
    }

    private fun cleanupTempDirectory(tempDir: Path): Result<Unit> {
        return runCatching {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }.onFailure { why ->
            println("Warning: Could not cleanup temp directory: ${why.message}")
        }
    }
}