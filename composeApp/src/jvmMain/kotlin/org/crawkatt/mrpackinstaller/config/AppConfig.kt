package org.crawkatt.mrpackinstaller.config

import java.util.Properties

object AppConfig {
    private val properties: Properties by lazy {
        Properties().apply {
            val inputStream = AppConfig::class.java.classLoader.getResourceAsStream("application.properties")
                ?: throw IllegalStateException("application.properties not found in resources")
            inputStream.use { load(it) }
        }
    }
    
    val apiBaseUrl: String by lazy {
        val url = properties.getProperty("api.base.url") 
            ?: throw IllegalStateException("api.base.url not found in application.properties")
        validateUrl(url)
        url
    }

    fun validateUrl(url: String) {
        require(url.isNotBlank()) { "URL cannot be blank" }
        require(url.startsWith("http://") || url.startsWith("https://")) { 
            "URL must start with http:// or https://, got: '$url'" 
        }
        require(!url.contains(" ")) { "URL cannot contain spaces" }
    }
}
