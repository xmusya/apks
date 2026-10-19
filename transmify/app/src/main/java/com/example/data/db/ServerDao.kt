package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY id ASC")
    fun getAllServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE isActive = 1 LIMIT 1")
    fun getActiveServer(): Flow<ServerEntity?>

    @Query("SELECT * FROM servers WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveServerOnce(): ServerEntity?

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getServerById(id: Long): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerEntity): Long

    @Update
    suspend fun updateServer(server: ServerEntity)

    @Delete
    suspend fun deleteServer(server: ServerEntity)

    @Query("UPDATE servers SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE servers SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: Long)

    @Query("SELECT COUNT(*) FROM servers")
    suspend fun getServerCount(): Int
}

@Dao
interface SearchDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 15")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuery(query: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearHistory()
}
