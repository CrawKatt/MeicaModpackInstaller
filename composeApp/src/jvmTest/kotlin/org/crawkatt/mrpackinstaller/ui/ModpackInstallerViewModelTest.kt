package org.crawkatt.mrpackinstaller.ui

import org.crawkatt.mrpackinstaller.data.LoaderType
import org.crawkatt.mrpackinstaller.data.ModpackInfo
import kotlin.test.*
import java.nio.file.Files
import java.nio.file.Path

class ModpackInstallerViewModelTest {

    private lateinit var viewModel: ModpackInstallerViewModel
    private lateinit var tempDir: Path

    @BeforeTest
    fun setUp() {
        viewModel = ModpackInstallerViewModel()
        tempDir = Files.createTempDirectory("viewmodel-test")
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
    fun initialStateTest() {
        assertNull(viewModel.selectedMrpackFile)
        assertNull(viewModel.modpackInfo)
        assertNull(viewModel.modpackIndex)
        assertFalse(viewModel.isInstalling)
        assertEquals(0f, viewModel.installProgress)
        assertEquals("", viewModel.statusMessage)
    }

    @Test
    fun setModpackInfoTest() {
        val testModpackInfo = ModpackInfo(
            name = "Test Modpack",
            version = "1.0.0",
            summary = "Test modpack for testing",
            minecraftVersion = "1.20.1",
            loader = "Fabric: 0.15.0",
            loaderVersion = "0.15.0",
            loaderType = LoaderType.FABRIC,
            modCount = 5,
            totalSize = 10240L
        )

        viewModel.modpackInfo = testModpackInfo
        assertEquals(testModpackInfo, viewModel.modpackInfo)
        assertEquals("Test Modpack", viewModel.modpackInfo?.name)
        assertEquals(LoaderType.FABRIC, viewModel.modpackInfo?.loaderType)
        assertEquals(5, viewModel.modpackInfo?.modCount)
    }

    @Test
    fun setMinecraftPathTest() {
        val testPath = tempDir.resolve("minecraft")
        Files.createDirectories(testPath)

        viewModel.minecraftPath = testPath
        assertEquals(testPath, viewModel.minecraftPath)
        assertTrue(Files.exists(testPath))
        assertTrue(Files.isDirectory(testPath))
    }

    @Test
    fun setMrpackFileTest() {
        val testFile = tempDir.resolve("test.mrpack").toFile()
        testFile.createNewFile()

        viewModel.selectedMrpackFile = testFile
        assertEquals(testFile, viewModel.selectedMrpackFile)
        assertTrue(testFile.exists())
        assertTrue(testFile.name.endsWith(".mrpack"))
    }

    @Test
    fun manageInstallationStateTest() {
        assertFalse(viewModel.isInstalling)
        assertEquals(0f, viewModel.installProgress)

        viewModel.isInstalling = true
        viewModel.installProgress = 0.5f

        assertTrue(viewModel.isInstalling)
        assertEquals(0.5f, viewModel.installProgress)

        viewModel.isInstalling = false
        viewModel.installProgress = 1.0f

        assertFalse(viewModel.isInstalling)
        assertEquals(1.0f, viewModel.installProgress)
    }

    @Test
    fun updateStatusMessagesTest() {
        assertEquals("", viewModel.statusMessage)

        viewModel.statusMessage = "🔄 Loading modpack..."
        assertEquals("🔄 Loading modpack...", viewModel.statusMessage)
        assertTrue(viewModel.statusMessage.contains("🔄"))

        viewModel.statusMessage = "✅ Installation completed successfully!"
        assertEquals("✅ Installation completed successfully!", viewModel.statusMessage)
        assertTrue(viewModel.statusMessage.contains("✅"))

        viewModel.statusMessage = "❌ Installation failed!"
        assertEquals("❌ Installation failed!", viewModel.statusMessage)
        assertTrue(viewModel.statusMessage.contains("❌"))
    }

    @Test
    fun validateInstallationRequirementsTest() {
        val testFile = tempDir.resolve("test.mrpack").toFile()
        val testPath = tempDir.resolve("minecraft")
        val testModpackInfo = createTestModpackInfo()

        testFile.createNewFile()
        Files.createDirectories(testPath)
        assertFalse(canInstallBasicValidation())

        viewModel.selectedMrpackFile = testFile
        assertFalse(canInstallBasicValidation())

        viewModel.minecraftPath = testPath
        assertFalse(canInstallBasicValidation())

        viewModel.modpackInfo = testModpackInfo
        assertTrue(canInstallBasicValidation())

        viewModel.isInstalling = true
        assertFalse(canInstallBasicValidation())
    }

    @Test
    fun handleDifferentLoaderTest() {
        val fabricInfo = ModpackInfo(
            name = "Fabric Pack",
            version = "1.0.0",
            summary = "Fabric modpack",
            minecraftVersion = "1.20.1",
            loader = "Fabric: 0.15.0",
            loaderVersion = "0.15.0",
            loaderType = LoaderType.FABRIC,
            modCount = 3,
            totalSize = 5120L
        )

        viewModel.modpackInfo = fabricInfo
        assertEquals(LoaderType.FABRIC, viewModel.modpackInfo?.loaderType)
        assertEquals(viewModel.modpackInfo?.loader?.contains("Fabric"), true)

        val forgeInfo = ModpackInfo(
            name = "Forge Pack",
            version = "1.0.0",
            summary = "Forge modpack",
            minecraftVersion = "1.20.1",
            loader = "Forge: 47.2.0",
            loaderVersion = "47.2.0",
            loaderType = LoaderType.FORGE,
            modCount = 8,
            totalSize = 15360L
        )

        viewModel.modpackInfo = forgeInfo
        assertEquals(LoaderType.FORGE, viewModel.modpackInfo?.loaderType)
        assertEquals(viewModel.modpackInfo?.loader?.contains("Forge"), true)

        val neoforgeInfo = ModpackInfo(
            name = "NeoForge Pack",
            version = "1.0.0",
            summary = "NeoForge modpack",
            minecraftVersion = "1.20.4",
            loader = "NeoForge: 20.4.190",
            loaderVersion = "20.4.190",
            loaderType = LoaderType.NEOFORGE,
            modCount = 6,
            totalSize = 12288L
        )

        viewModel.modpackInfo = neoforgeInfo
        assertEquals(LoaderType.NEOFORGE, viewModel.modpackInfo?.loaderType)
        assertEquals(viewModel.modpackInfo?.loader?.contains("NeoForge"), true)
    }

    @Test
    fun handleInstallationProgressTest() {
        val validProgressValues = listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)
        
        validProgressValues.forEach { progress ->
            viewModel.installProgress = progress
            assertEquals(progress, viewModel.installProgress)
            assertTrue(viewModel.installProgress >= 0.0f)
            assertTrue(viewModel.installProgress <= 1.0f)
        }

        viewModel.isInstalling = true
        viewModel.installProgress = 0.0f
        assertTrue(viewModel.isInstalling)
        assertEquals(0.0f, viewModel.installProgress)

        viewModel.installProgress = 0.5f
        assertTrue(viewModel.isInstalling)
        assertEquals(0.5f, viewModel.installProgress)

        viewModel.installProgress = 1.0f
        viewModel.isInstalling = false
        assertFalse(viewModel.isInstalling)
        assertEquals(1.0f, viewModel.installProgress)
    }

    @Test
    fun maintainDataConsistencyTest() {
        val testFile = tempDir.resolve("consistent.mrpack").toFile()
        val testPath = tempDir.resolve("minecraft")
        val testInfo = createTestModpackInfo()

        testFile.createNewFile()
        Files.createDirectories(testPath)

        viewModel.selectedMrpackFile = testFile
        viewModel.minecraftPath = testPath
        viewModel.modpackInfo = testInfo
        viewModel.statusMessage = "All data set"

        assertNotNull(viewModel.selectedMrpackFile)
        assertNotNull(viewModel.minecraftPath)
        assertNotNull(viewModel.modpackInfo)
        assertTrue(viewModel.statusMessage.isNotEmpty())

        assertEquals(testFile.name, viewModel.selectedMrpackFile?.name)
        assertEquals(testPath.fileName.toString(), viewModel.minecraftPath?.fileName.toString())
        assertEquals("Test Modpack", viewModel.modpackInfo?.name)
    }

    private fun createTestModpackInfo(): ModpackInfo {
        return ModpackInfo(
            name = "Test Modpack",
            version = "1.0.0",
            summary = "Test modpack for unit testing",
            minecraftVersion = "1.20.1",
            loader = "Fabric: 0.15.0",
            loaderVersion = "0.15.0",
            loaderType = LoaderType.FABRIC,
            modCount = 5,
            totalSize = 10240L
        )
    }

    private fun canInstallBasicValidation(): Boolean {
        return viewModel.selectedMrpackFile != null &&
               viewModel.minecraftPath != null &&
               viewModel.modpackInfo != null &&
               !viewModel.isInstalling
    }
}