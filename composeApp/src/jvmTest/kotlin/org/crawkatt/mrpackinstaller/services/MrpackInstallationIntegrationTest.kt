package org.crawkatt.mrpackinstaller.services

import org.crawkatt.mrpackinstaller.data.LoaderType
import org.crawkatt.mrpackinstaller.data.ModFile
import kotlin.test.*
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MrpackInstallationIntegrationTest {

    private lateinit var modpackService: ModpackService
    private lateinit var tempDir: Path
    private lateinit var minecraftPath: Path

    @BeforeTest
    fun setUp() {
        modpackService = ModpackService()
        tempDir = Files.createTempDirectory("mrpack-integration-test")
        minecraftPath = tempDir.resolve("minecraft")
        Files.createDirectories(minecraftPath)
        createMinecraftDirectoryStructure()
    }

    @AfterTest
    fun tearDown() {
        Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .forEach { path ->
                runCatching {
                    Files.deleteIfExists(path)
                }
            }
    }

    @Test
    fun loadFabricModpackTest() {
        val mrpackFile = createCompleteFabricMrpack()

        val loadResult = modpackService.loadModpackInfo(mrpackFile)
        assertTrue(loadResult.isSuccess, "Should successfully load modpack info")

        val (modpackInfo, modpackIndex) = loadResult.getOrThrow()
        assertEquals("Complete Fabric Test", modpackInfo.name)
        assertEquals("1.0.0", modpackInfo.version)
        assertEquals(LoaderType.FABRIC, modpackInfo.loaderType)
        assertEquals("1.20.1", modpackInfo.minecraftVersion)
        assertEquals("0.15.0", modpackInfo.loaderVersion)

        verifyMinecraftDirectoryStructure()
        val extractResult = modpackService.extractIncludedFiles(mrpackFile, minecraftPath)
        assertTrue(extractResult.isSuccess, "Should extract included files successfully")

        verifyIncludedFilesExtracted()
        val modFiles = modpackService.parseModFiles(modpackIndex)
        assertEquals(2, modFiles.size, "Should have 2 mod files")

        verifyModFilesParsing(modFiles)
    }

    @Test
    fun loadForgeModpackTest() {
        val mrpackFile = createCompleteForgeModpack()
        val loadResult = modpackService.loadModpackInfo(mrpackFile)
        assertTrue(loadResult.isSuccess)

        val (modpackInfo, _) = loadResult.getOrThrow()
        assertEquals(LoaderType.FORGE, modpackInfo.loaderType)
        assertEquals("47.2.0", modpackInfo.loaderVersion)
        assertEquals("Complete Forge Test", modpackInfo.name)

        val extractResult = modpackService.extractIncludedFiles(mrpackFile, minecraftPath)
        assertTrue(extractResult.isSuccess)

        val forgeConfigPath = minecraftPath.resolve("config/forge.cfg")
        assertTrue(Files.exists(forgeConfigPath), "Forge config should be extracted")
    }

    @Test
    fun validateModFilePropertiesTest() {
        val mrpackFile = createCompleteFabricMrpack()
        val loadResult = modpackService.loadModpackInfo(mrpackFile)
        assertTrue(loadResult.isSuccess)

        val (_, modpackIndex) = loadResult.getOrThrow()
        val modFiles = modpackService.parseModFiles(modpackIndex)
        assertTrue(modFiles.isNotEmpty(), "Debería tener Mods")

        modFiles.forEach { modFile ->
            assertFalse(modFile.path.isBlank(), "La ruta del archivo mod no debe estar en blanco")
            assertFalse(modFile.downloadUrl.isBlank(), "La URL de descarga no debe estar en blanco")
            assertFalse(modFile.sha1.isBlank(), "SHA1 hash no debe estar en blanco")
            assertTrue(modFile.fileSize > 0, "El tamaño del archivo debe ser mayor que 0")
            assertTrue(modFile.downloadUrl.startsWith("http"), "La URL de descarga debe ser una URL HTTP válida")
            assertEquals(40, modFile.sha1.length, "El hash SHA1 debe tener 40 caracteres de longitud")
        }
    }

    @Test
    fun handleCorruptedMrpackFileTest() {
        val corruptedFile = createCorruptedMrpackFile()
        val loadResult = modpackService.loadModpackInfo(corruptedFile)

        assertTrue(loadResult.isFailure, "Should fail with corrupted file")
        val exception = loadResult.exceptionOrNull()
        assertNotNull(exception)
    }

    @Test
    fun validateFileExtractionTest() {
        val mrpackFile = createMrpackWithSpecificFiles()
        val extractResult = modpackService.extractIncludedFiles(mrpackFile, minecraftPath)
        assertTrue(extractResult.isSuccess)

        val optionsFile = minecraftPath.resolve("options.txt")
        assertTrue(Files.exists(optionsFile))
        val optionsContent = Files.readString(optionsFile)
        assertTrue(optionsContent.contains("key_jump:key.keyboard.space"))

        val modConfigFile = minecraftPath.resolve("config/modconfig.json")
        assertTrue(Files.exists(modConfigFile))
        val configContent = Files.readString(modConfigFile)
        assertTrue(configContent.contains("\"enabled\": true"))
    }

    private fun createMinecraftDirectoryStructure() {
        val requiredDirs = listOf("mods", "config", "resourcepacks", "shaderpacks", "saves", "logs")
        requiredDirs.forEach { dir ->
            Files.createDirectories(minecraftPath.resolve(dir))
        }
    }

    private fun verifyMinecraftDirectoryStructure() {
        val requiredDirs = listOf("mods", "config", "resourcepacks", "shaderpacks", "saves", "logs")
        requiredDirs.forEach { dir ->
            val dirPath = minecraftPath.resolve(dir)
            assertTrue(Files.exists(dirPath), "Directory $dir should exist")
            assertTrue(Files.isDirectory(dirPath), "$dir should be a directory")
        }
    }

    private fun verifyIncludedFilesExtracted() {
        val configFile = minecraftPath.resolve("config/fabric-client.toml")
        assertTrue(Files.exists(configFile), "Config file should be extracted")
        val configContent = Files.readString(configFile)
        assertTrue(configContent.contains("fabric_setting"))

        val resourcepackFile = minecraftPath.resolve("resourcepacks/default-pack.zip")
        assertTrue(Files.exists(resourcepackFile), "Resourcepack should be extracted")

        val indexFile = minecraftPath.resolve("modrinth.index.json")
        assertFalse(Files.exists(indexFile), "Index file should not be extracted to minecraft directory")
    }

    private fun verifyModFilesParsing(modFiles: List<ModFile>) {
        val firstMod = modFiles[0]
        assertEquals("mods/fabric-api.jar", firstMod.path)
        assertTrue(firstMod.downloadUrl.contains("fabric-api"))
        assertTrue(firstMod.sha1.matches(Regex("[a-f0-9]{40}")))
        assertEquals(1536000L, firstMod.fileSize)

        val secondMod = modFiles[1]
        assertEquals("mods/jei.jar", secondMod.path)
        assertTrue(secondMod.downloadUrl.contains("jei"))
        assertEquals(2048000L, secondMod.fileSize)
    }

    private fun createCompleteFabricMrpack(): File {
        val file = tempDir.resolve("complete_fabric_modpack.mrpack").toFile()

        val indexJson = """
            {
                "formatVersion": 1,
                "game": "minecraft",
                "versionId": "1.0.0",
                "name": "Complete Fabric Test",
                "summary": "Complete fabric modpack for integration testing",
                "dependencies": {
                    "minecraft": "1.20.1",
                    "fabric-loader": "0.15.0"
                },
                "files": [
                    {
                        "path": "mods/fabric-api.jar",
                        "hashes": {
                            "sha1": "a1b2c3d4e5f6789012345678901234567890abcd"
                        },
                        "fileSize": 1536000,
                        "downloads": ["https://cdn.modrinth.com/data/fabric-api/versions/fabric-api-1.20.1.jar"]
                    },
                    {
                        "path": "mods/jei.jar",
                        "hashes": {
                            "sha1": "b2c3d4e5f67890123456789012345678901abcde"
                        },
                        "fileSize": 2048000,
                        "downloads": ["https://cdn.modrinth.com/data/jei/versions/jei-1.20.1.jar"]
                    }
                ]
            }
        """.trimIndent()

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            zos.putNextEntry(ZipEntry("modrinth.index.json"))
            zos.write(indexJson.toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("config/fabric-client.toml"))
            zos.write("[fabric_setting]\nenabled = true\n".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("resourcepacks/default-pack.zip"))
            zos.write("dummy resourcepack content".toByteArray())
            zos.closeEntry()
        }

        return file
    }

    private fun createCompleteForgeModpack(): File {
        val file = tempDir.resolve("complete_forge_modpack.mrpack").toFile()

        val indexJson = """
            {
                "formatVersion": 1,
                "game": "minecraft",
                "versionId": "1.0.0",
                "name": "Complete Forge Test",
                "summary": "Complete forge modpack for integration testing",
                "dependencies": {
                    "minecraft": "1.20.1",
                    "forge": "47.2.0"
                },
                "files": []
            }
        """.trimIndent()

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            zos.putNextEntry(ZipEntry("modrinth.index.json"))
            zos.write(indexJson.toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("config/forge.cfg"))
            zos.write("# Forge configuration\nforge_setting=true\n".toByteArray())
            zos.closeEntry()
        }

        return file
    }

    private fun createCorruptedMrpackFile(): File {
        val file = tempDir.resolve("corrupted_modpack.mrpack").toFile()

        FileOutputStream(file).use { fos ->
            fos.write("This is definitely not a valid zip file content".toByteArray())
        }

        return file
    }

    private fun createMrpackWithSpecificFiles(): File {
        val file = tempDir.resolve("specific_files_modpack.mrpack").toFile()

        val indexJson = """
            {
                "formatVersion": 1,
                "game": "minecraft",
                "versionId": "1.0.0",
                "name": "Specific Files Test",
                "summary": "Test modpack with specific files",
                "dependencies": {
                    "minecraft": "1.20.1",
                    "fabric-loader": "0.15.0"
                },
                "files": []
            }
        """.trimIndent()

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            zos.putNextEntry(ZipEntry("modrinth.index.json"))
            zos.write(indexJson.toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("options.txt"))
            zos.write("key_jump:key.keyboard.space\nkey_sneak:key.keyboard.left.shift\n".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("config/modconfig.json"))
            zos.write("{\"enabled\": true, \"debug\": false}".toByteArray())
            zos.closeEntry()
        }

        return file
    }
}
