package org.example.project

import kotlinx.serialization.Serializable

enum class LogLevel {
    OFF,
    FATAL,
    ERROR,
    WARN,
    INFO,
    DEBUG,
    TRACE
}

@Serializable
data class VersionInfo(
    val appVersion: String = "",
    val commitHash: String = "",
    val commitDate: String = "",
    val lastUsedDate: String = ""
)

@Serializable
data class Settings(
    val language: String = "en",
    val statisticTimespan: String = "year",
    val logLevel: LogLevel = LogLevel.ERROR,
    val enableExpirationDates: Boolean = true,
    val versionInfo: VersionInfo = VersionInfo()
)
