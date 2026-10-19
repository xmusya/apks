package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 9091,
    val path: String = "/transmission/rpc",
    val useHttps: Boolean = false,
    val username: String = "",
    val password: String = "",
    val isActive: Boolean = false,
    val isDemoMode: Boolean = false
) {
    val displayUrl: String
        get() = if (isDemoMode) {
            "Демо-сервер (Песочница)"
        } else {
            val scheme = if (useHttps) "https" else "http"
            "$scheme://$host:$port$path"
        }
}

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
