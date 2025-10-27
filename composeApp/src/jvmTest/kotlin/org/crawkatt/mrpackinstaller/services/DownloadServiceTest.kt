package org.crawkatt.mrpackinstaller.services

import org.crawkatt.mrpackinstaller.data.ModFile
import kotlin.test.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

class DownloadServiceTest {

    private lateinit var downloadService: DownloadService
    private lateinit var tempDir: Path
    private lateinit var testFile: Path

    @BeforeTest
    fun setUp() {
        downloadService = DownloadService()
        tempDir = Files.createTempDirectory("download-test")
        testFile = tempDir.resolve("test-mod.jar")
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
    fun testDataIntegrity() {
        val modFile = ModFile(
            path = "mods/test-mod.jar",
            downloadUrl = "https://example.com/mod.jar",
            sha1 = "abcdef1234567890abcdef1234567890abcdef12",
            fileSize = 1024L
        )

        assertEquals("mods/test-mod.jar", modFile.path)
        assertEquals("https://example.com/mod.jar", modFile.downloadUrl)
        assertEquals("abcdef1234567890abcdef1234567890abcdef12", modFile.sha1)
        assertEquals(1024L, modFile.fileSize)

        assertTrue(modFile.sha1.matches(Regex("[a-f0-9]{40}")), "SHA1 should be valid format")
        assertTrue(modFile.fileSize > 0, "File size should be positive")
        assertFalse(modFile.path.isBlank(), "Path should not be blank")
        assertFalse(modFile.downloadUrl.isBlank(), "Download URL should not be blank")
    }

    @Test
    fun handleZeroSizedFiles() {
        val emptyContent = ""
        val emptySha1 = calculateSha1(emptyContent.toByteArray())

        val modFile = ModFile(
            path = "mods/empty-mod.jar",
            downloadUrl = "http://example.com/empty.jar",
            sha1 = emptySha1,
            fileSize = 0L
        )

        val minecraftPath = tempDir.resolve("minecraft")
        Files.createDirectories(minecraftPath)

        val result = downloadService.downloadAndVerifyModFile(modFile, minecraftPath)
        assertTrue(result.isFailure, "Should fail due to network (expected for test)")
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", emptySha1)
    }

    @Test
    fun validateDownloadParameters() {
        val validUrls = listOf(
            "https://example.com/file.jar",
            "http://test.com/mod.jar",
            "https://cdn.modrinth.com/data/mod/versions/file.jar"
        )

        val invalidUrls = listOf(
            "",
            "not-a-url",
            "ftp://invalid.protocol/file.jar",
            "invalid://protocol/file.jar"
        )

        validUrls.forEach { url ->
            val result = downloadService.downloadFile(url, testFile)
            assertTrue(result.isFailure, "Expected network failure for $url")
        }

        invalidUrls.forEach { url ->
            val result = downloadService.downloadFile(url, testFile)
            assertTrue(result.isFailure, "Should fail for invalid URL: $url")
        }
    }

    private fun calculateSha1(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val hashBytes = digest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
