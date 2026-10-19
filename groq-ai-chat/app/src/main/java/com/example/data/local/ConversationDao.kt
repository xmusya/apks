package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    fun getConversationById(id: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationByIdOnce(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTitle(id: String, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET modelId = :modelId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateModel(id: String, modelId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET totalPromptTokens = totalPromptTokens + :promptTokens, totalCompletionTokens = totalCompletionTokens + :completionTokens, updatedAt = :updatedAt WHERE id = :id")
    suspend fun addTokens(id: String, promptTokens: Int, completionTokens: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: String)

    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()
}
