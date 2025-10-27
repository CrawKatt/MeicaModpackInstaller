package org.crawkatt.mrpackinstaller.services

import java.io.File
import java.nio.file.Path

class MinecraftLauncherService {
    fun launchMinecraft(minecraftPath: Path, profileName: String): Result<Unit> {
        return runCatching {
            val os = System.getProperty("os.name").lowercase()
            val launcherPath = findMinecraftLauncher(os)
                ?: throw IllegalStateException("No se pudo encontrar el launcher de Minecraft")

            println("🚀 Lanzando Minecraft desde: $launcherPath")
            println("📁 Directorio de Minecraft: $minecraftPath")
            println("🎮 Perfil: $profileName")

            val processBuilder = when {
                os.contains("win") -> ProcessBuilder("cmd", "/c", "start", "", launcherPath)
                os.contains("mac") -> ProcessBuilder("open", launcherPath)
                else -> ProcessBuilder(launcherPath)
            }

            processBuilder.start()
            println("✅ Minecraft launcher iniciado correctamente")
        }.onFailure { exception ->
            println("❌ Error al lanzar Minecraft: ${exception.message}")
            exception.printStackTrace()
        }
    }

    private fun findMinecraftLauncher(os: String): String? {
        return when {
            os.contains("win") -> findForWindows()
            os.contains("mac") -> findForMacOS()
            else -> findForLinux()
        }
    }

    fun findForWindows(): String? {
        val programFiles = System.getenv("ProgramFiles(x86)") ?: System.getenv("ProgramFiles")
        val possiblePaths = listOf(
            "$programFiles\\Minecraft Launcher\\MinecraftLauncher.exe",
            "${System.getProperty("user.home")}\\AppData\\Local\\Packages\\Microsoft.4297127D64EC6_8wekyb3d8bbwe\\LocalCache\\Local\\Minecraft Launcher\\MinecraftLauncher.exe"
        )

        return possiblePaths.firstOrNull { File(it).exists() }
    }

    fun findForMacOS(): String? {
        val possiblePaths = listOf(
            "/Applications/Minecraft.app"
        )

        return possiblePaths.firstOrNull { File(it).exists() }
    }

    fun findForLinux(): String? {
        val possiblePaths = listOf(
            "/usr/bin/minecraft-launcher",
            "/usr/local/bin/minecraft-launcher",
            "${System.getProperty("user.home")}/.local/bin/minecraft-launcher"
        )

        return possiblePaths.firstOrNull { File(it).exists() }
    }

    fun isMinecraftLauncherInstalled(): Boolean {
        val os = System.getProperty("os.name").lowercase()
        return findMinecraftLauncher(os) != null
    }
}