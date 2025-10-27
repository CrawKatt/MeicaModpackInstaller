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
        properties.getProperty("api.base.url") 
            ?: throw IllegalStateException("api.base.url not found in application.properties")
    }
}
