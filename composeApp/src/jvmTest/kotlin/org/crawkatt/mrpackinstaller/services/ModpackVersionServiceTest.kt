package org.crawkatt.mrpackinstaller.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.crawkatt.mrpackinstaller.data.LoaderType
import org.crawkatt.mrpackinstaller.data.ModpackInfo
import kotlin.test.*
import java.nio.file.Files
import java.nio.file.Path

class ModpackVersionServiceTest {
    private lateinit var versionService: ModpackVersionService
    private lateinit var objectMapper: ObjectMapper
    private lateinit var tempDir: Path

    @BeforeTest
    fun setUp() {
        versionService = ModpackVersionService()
        objectMapper = ObjectMapper()
        tempDir = Files.createTempDirectory("version-test")
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
    fun testCompareVersions_NotInstalled() {
        val modpackInfo = createTestModpackInfo("TestPack", "1.0.0")
        val comparison = versionService.compareVersions(tempDir, modpackInfo, null).getOrThrow()

        assertFalse(comparison.isInstalled, "Should not be installed")
        assertFalse(comparison.needsUpdate, "Should not need update when not installed")
        assertNull(comparison.installedVersion)
        assertEquals("1.0.0", comparison.availableVersion)
    }

    @Test
    fun testCompareVersions_SameVersion() {
        val modpackInfo = createTestModpackInfo("TestPack", "1.0.0")
        val modpackIndex = createTestModpackIndex(listOf("mod1.jar", "mod2.jar"))

        versionService.saveInstalledVersion(tempDir, modpackInfo, modpackIndex)
        val comparison = versionService.compareVersions(tempDir, modpackInfo, modpackIndex).getOrThrow()

        assertTrue(comparison.isInstalled, "Should be installed")
        assertFalse(comparison.needsUpdate, "Should not need update for same version")
        assertEquals("1.0.0", comparison.installedVersion)
        assertEquals("1.0.0", comparison.availableVersion)
        assertFalse(comparison.modsChanged, "Mods should not have changed")
    }

    @Test
    fun testCompareVersions_DifferentVersion() {
        val oldModpackInfo = createTestModpackInfo("TestPack", "1.0.0")
        val modpackIndex = createTestModpackIndex(listOf("mod1.jar", "mod2.jar"))

        versionService.saveInstalledVersion(tempDir, oldModpackInfo, modpackIndex)
        val newModpackInfo = createTestModpackInfo("TestPack", "1.1.0")
        val comparison = versionService.compareVersions(tempDir, newModpackInfo, modpackIndex).getOrThrow()

        assertTrue(comparison.isInstalled, "Should be installed")
        assertTrue(comparison.needsUpdate, "Should need update for different version")
        assertEquals("1.0.0", comparison.installedVersion)
        assertEquals("1.1.0", comparison.availableVersion)
    }

    @Test
    fun testCompareVersions_ModsChanged() {
        val modpackInfo = createTestModpackInfo("TestPack", "1.0.0")
        val oldIndex = createTestModpackIndex(listOf("mod1.jar", "mod2.jar"))

        versionService.saveInstalledVersion(tempDir, modpackInfo, oldIndex)
        val newIndex = createTestModpackIndex(listOf("mod1.jar", "mod3.jar"))
        val comparison = versionService.compareVersions(tempDir, modpackInfo, newIndex).getOrThrow()

        assertTrue(comparison.isInstalled, "Should be installed")
        assertTrue(comparison.needsUpdate, "Should need update when mods changed")
        assertTrue(comparison.modsChanged, "Mods should have changed")
    }

    @Test
    fun testCompareVersions_DifferentName() {
        val oldModpackInfo = createTestModpackInfo("TestPack", "1.0.0")
        val modpackIndex = createTestModpackIndex(listOf("mod1.jar"))

        versionService.saveInstalledVersion(tempDir, oldModpackInfo, modpackIndex)
        val newModpackInfo = createTestModpackInfo("NewTestPack", "1.0.0")
        val comparison = versionService.compareVersions(tempDir, newModpackInfo, modpackIndex).getOrThrow()

        assertTrue(comparison.isInstalled, "Should be installed")
        assertTrue(comparison.needsUpdate, "Should need update for different name")
    }

    @Test
    fun testCompareVersions_NoIndexProvided() {
        val modpackInfo = createTestModpackInfo("TestPack", "1.0.0")
        val modpackIndex = createTestModpackIndex(listOf("mod1.jar", "mod2.jar"))

        versionService.saveInstalledVersion(tempDir, modpackInfo, modpackIndex)
        val comparison = versionService.compareVersions(tempDir, modpackInfo, null).getOrThrow()

        assertTrue(comparison.isInstalled, "Should be installed")
        assertFalse(comparison.needsUpdate, "Should not need update when index is not provided")
        assertFalse(comparison.modsChanged, "Mods should not be marked as changed when index is null")
    }

    @Test
    fun testSaveAndGetInstalledModpackInfo() {
        val modpackInfo = createTestModpackInfo("TestPack", "1.0.0")
        val modpackIndex = createTestModpackIndex(listOf("mod1.jar", "mod2.jar"))

        versionService.saveInstalledVersion(tempDir, modpackInfo, modpackIndex)
        val savedInfo = versionService.getInstalledModpackInfo(tempDir)?.getOrNull()

        assertNotNull(savedInfo)
        assertEquals("TestPack", savedInfo.name)
        assertEquals("1.0.0", savedInfo.version)
        assertEquals(2, savedInfo.modCount)
    }

    @Test
    fun testGetInstalledModpackInfo_NotInstalled() {
        val savedInfo = versionService.getInstalledModpackInfo(tempDir)
        assertNull(savedInfo, "Should return null when not installed")
    }

    private fun createTestModpackInfo(name: String, version: String): ModpackInfo {
        return ModpackInfo(
            name = name,
            version = version,
            summary = "Test modpack",
            minecraftVersion = "1.20.1",
            loader = "Fabric 0.15.0",
            loaderVersion = "0.15.0",
            loaderType = LoaderType.FABRIC,
            modCount = 2,
            totalSize = 1000000
        )
    }

    private fun createTestModpackIndex(modUrls: List<String>): com.fasterxml.jackson.databind.JsonNode {
        val root = objectMapper.createObjectNode()
        val filesArray = root.putArray("files")

        modUrls.forEach { url ->
            val fileNode = objectMapper.createObjectNode()
            fileNode.put("path", "mods/${url}")
            val downloadsArray = fileNode.putArray("downloads")
            downloadsArray.add("https://example.com/mods/${url}")
            filesArray.add(fileNode)
        }

        return root
    }
}