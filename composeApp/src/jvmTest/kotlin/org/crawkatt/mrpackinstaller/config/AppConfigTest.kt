package org.crawkatt.mrpackinstaller.config

import kotlin.test.Test
import kotlin.test.assertFailsWith

class AppConfigTest {
    @Test
    fun testValidHttpUrl() {
        AppConfig.validateUrl("http://localhost:3000/api")
        AppConfig.validateUrl("http://example.com/api")
        AppConfig.validateUrl("http://192.168.1.1:8080/api")
    }
    
    @Test
    fun testValidHttpsUrl() {
        AppConfig.validateUrl("https://api.example.com")
        AppConfig.validateUrl("https://example.com:443/api/v1")
    }
    
    @Test
    fun testBlankUrlShouldFail() {
        assertFailsWith<IllegalArgumentException> {
            AppConfig.validateUrl("")
        }
        assertFailsWith<IllegalArgumentException> {
            AppConfig.validateUrl("   ")
        }
    }
    
    @Test
    fun testUrlWithoutProtocolShouldFail() {
        assertFailsWith<IllegalArgumentException> {
            AppConfig.validateUrl("localhost:3000/api")
        }
        assertFailsWith<IllegalArgumentException> {
            AppConfig.validateUrl("example.com")
        }
    }

    @Test
    fun testUrlWithSpacesShouldFail() {
        assertFailsWith<IllegalArgumentException> {
            AppConfig.validateUrl("http://example .com")
        }
        assertFailsWith<IllegalArgumentException> {
            AppConfig.validateUrl("http://example.com /api")
        }
    }
}