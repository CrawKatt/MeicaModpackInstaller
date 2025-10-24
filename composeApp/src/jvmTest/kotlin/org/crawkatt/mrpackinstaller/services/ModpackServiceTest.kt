package org.crawkatt.mrpackinstaller.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.crawkatt.mrpackinstaller.data.LoaderType
import kotlin.test.*
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ModpackServiceTest {

    private lateinit var modpackService: ModpackService
    private lateinit var objectMapper: ObjectMapper
    private lateinit var tempDir: Path

    @BeforeTest
    fun setUp() {
        modpackService = ModpackService()
        objectMapper = ObjectMapper()
        tempDir = Files.createTempDirectory("mrpack-test")
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
    fun loadValidFabricMrpackFile() {
        val modpackFile = createValidFabricMrpackFile()
        val result = modpackService.loadModpackInfo(modpackFile)
        assertTrue(result.isSuccess, "Should successfully load valid mrpack file")
        val (modpackInfo, jsonNode) = result.getOrThrow()
        
        assertEquals("Test Fabric Modpack", modpackInfo.name)
        assertEquals("1.0.0", modpackInfo.version)
        assertEquals("Test fabric modpack description", modpackInfo.summary)
        assertEquals("1.20.1", modpackInfo.minecraftVersion)
        assertEquals("Fabric: 0.15.0", modpackInfo.loader)
        assertEquals("0.15.0", modpackInfo.loaderVersion)
        assertEquals(LoaderType.FABRIC, modpackInfo.loaderType)
        assertEquals(2, modpackInfo.modCount)
        assertTrue(modpackInfo.totalSize > 0)
        
        assertNotNull(jsonNode)
        assertEquals("Test Fabric Modpack", jsonNode.path("name").asText())
    }

    @Test
    fun loadValidForgeMrpackFile() {
        val modpackFile = createValidForgeMrpackFile()
        val result = modpackService.loadModpackInfo(modpackFile)

        assertTrue(result.isSuccess)
        val (modpackInfo, _) = result.getOrThrow()
        
        assertEquals(LoaderType.FORGE, modpackInfo.loaderType)
        assertEquals("Forge: 47.2.0", modpackInfo.loader)
        assertEquals("47.2.0", modpackInfo.loaderVersion)
        assertEquals("Test Forge Modpack", modpackInfo.name)
    }

    @Test
    fun loadValidNeoForgeMrpackFile() {
        val modpackFile = createValidNeoForgeMrpackFile()
        val result = modpackService.loadModpackInfo(modpackFile)

        assertTrue(result.isSuccess)
        val (modpackInfo, _) = result.getOrThrow()
        
        assertEquals(LoaderType.NEOFORGE, modpackInfo.loaderType)
        assertEquals("NeoForge: 20.4.190", modpackInfo.loader)
        assertEquals("20.4.190", modpackInfo.loaderVersion)
    }

    @Test
    fun extractFilesTest() {
        val modpackFile = createMrpackWithIncludedFiles()
        val minecraftPath = tempDir.resolve("minecraft")
        Files.createDirectories(minecraftPath)

        val result = modpackService.extractIncludedFiles(modpackFile, minecraftPath)
        assertTrue(result.isSuccess, "Should successfully extract files")

        val configFile = minecraftPath.resolve("config/test-config.json")
        val resourcepackFile = minecraftPath.resolve("resourcepacks/test-pack.zip")
        
        assertTrue(Files.exists(configFile), "Config file should be extracted")
        assertTrue(Files.exists(resourcepackFile), "Resourcepack file should be extracted")

        val configContent = Files.readString(configFile)
        assertTrue(configContent.contains("test_config"))

        val indexFile = minecraftPath.resolve("modrinth.index.json")
        assertFalse(Files.exists(indexFile), "Index file should not be extracted")
    }

    @Test
    fun parseModFilesTest() {
        val jsonContent = """
            {
                "files": [
                    {
                        "path": "mods/mod1.jar",
                        "hashes": {
                            "sha1": "abc123"
                        },
                        "fileSize": 1024,
                        "downloads": ["https://example.com/mod1.jar"]
                    },
                    {
                        "path": "mods/mod2.jar",
                        "hashes": {
                            "sha1": "def456"
                        },
                        "fileSize": 2048,
                        "downloads": ["https://example.com/mod2.jar", "https://mirror.com/mod2.jar"]
                    }
                ]
            }
        """.trimIndent()
        
        val jsonNode = objectMapper.readTree(jsonContent)
        val modFiles = modpackService.parseModFiles(jsonNode)
        assertEquals(2, modFiles.size)
        
        val mod1 = modFiles[0]
        assertEquals("mods/mod1.jar", mod1.path)
        assertEquals("abc123", mod1.sha1)
        assertEquals(1024L, mod1.fileSize)
        assertEquals("https://example.com/mod1.jar", mod1.downloadUrl)
        
        val mod2 = modFiles[1]
        assertEquals("mods/mod2.jar", mod2.path)
        assertEquals("def456", mod2.sha1)
        assertEquals(2048L, mod2.fileSize)
        assertEquals("https://example.com/mod2.jar", mod2.downloadUrl)
    }

    @Test
    fun skipFilesWithoutDownloadsTest() {
        val jsonContent = """
            {
                "files": [
                    {
                        "path": "mods/mod1.jar",
                        "hashes": {
                            "sha1": "abc123"
                        },
                        "fileSize": 1024,
                        "downloads": []
                    }
                ]
            }
        """.trimIndent()
        
        val jsonNode = objectMapper.readTree(jsonContent)
        val modFiles = modpackService.parseModFiles(jsonNode)
        assertTrue(modFiles.isEmpty(), "Should skip files without downloads")
    }

    private fun createValidFabricMrpackFile(): File {
        val file = tempDir.resolve("test_fabric_modpack.mrpack").toFile()
        
        val indexJson = """
            {
                "formatVersion": 1,
                "game": "minecraft",
                "versionId": "1.0.0",
                "name": "Test Fabric Modpack",
                "summary": "Test fabric modpack description",
                "dependencies": {
                    "minecraft": "1.20.1",
                    "fabric-loader": "0.15.0"
                },
                "files": [
                    {
                        "path": "mods/mod1.jar",
                        "hashes": {
                            "sha1": "abc123"
                        },
                        "fileSize": 1024,
                        "downloads": ["https://example.com/mod1.jar"]
                    },
                    {
                        "path": "mods/mod2.jar",
                        "hashes": {
                            "sha1": "def456"
                        },
                        "fileSize": 2048,
                        "downloads": ["https://example.com/mod2.jar"]
                    }
                ]
            }
        """.trimIndent()
        
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            zos.putNextEntry(ZipEntry("modrinth.index.json"))
            zos.write(indexJson.toByteArray())
            zos.closeEntry()
        }
        
        return file
    }

    private fun createValidForgeMrpackFile(): File {
        val file = tempDir.resolve("test_forge_modpack.mrpack").toFile()
        
        val indexJson = """
            {
                "formatVersion": 1,
                "game": "minecraft",
                "versionId": "1.0.0",
                "name": "Test Forge Modpack",
                "summary": "Test forge modpack",
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
        }
        
        return file
    }

    private fun createValidNeoForgeMrpackFile(): File {
        val file = tempDir.resolve("test_neoforge_modpack.mrpack").toFile()
        
        val indexJson = """
            {
                "formatVersion": 1,
                "game": "minecraft",
                "versionId": "1.0.0",
                "name": "Test NeoForge Modpack",
                "summary": "Test neoforge modpack",
                "dependencies": {
                    "minecraft": "1.20.4",
                    "neoforge": "20.4.190"
                },
                "files": []
            }
        """.trimIndent()
        
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            zos.putNextEntry(ZipEntry("modrinth.index.json"))
            zos.write(indexJson.toByteArray())
            zos.closeEntry()
        }
        
        return file
    }

    private fun createMrpackWithIncludedFiles(): File {
        val file = tempDir.resolve("modpack_with_files.mrpack").toFile()
        
        val indexJson = """
            {
                "formatVersion": 1,
                "game": "minecraft",
                "versionId": "1.0.0",
                "name": "Test Modpack with Files",
                "summary": "Test modpack with included files",
                "dependencies": {
                    "minecraft": "1.20.1",
                    "fabric-loader": "0.15.0"
                },
                "files": []
            }
        """.trimIndent()
        
        val configContent = """{"test_config": true}"""
        val resourcepackContent = "dummy resourcepack content"
        
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            zos.putNextEntry(ZipEntry("modrinth.index.json"))
            zos.write(indexJson.toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("config/test-config.json"))
            zos.write(configContent.toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("resourcepacks/test-pack.zip"))
            zos.write(resourcepackContent.toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
            zos.write("Should be skipped".toByteArray())
            zos.closeEntry()
        }
        
        return file
    }
}