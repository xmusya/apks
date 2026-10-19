package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class TokenAggregate(
    val totalPrompt: Long = 0L,
    val totalCompletion: Long = 0L,
    val totalAll: Long = 0L,
    val avgResponseTime: Double = 0.0,
    val avgTokensPerSec: Double = 0.0,
    val totalAssistantMessages: Int = 0
)

data class ModelUsageStat(
    val modelUsed: String,
    val count: Int,
    val totalTokens: Long
)

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesForConversationOnce(conversationId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()

    @Query("""
        SELECT 
            COALESCE(SUM(promptTokens), 0) as totalPrompt,
            COALESCE(SUM(completionTokens), 0) as totalCompletion,
            COALESCE(SUM(totalTokens), 0) as totalAll,
            COALESCE(AVG(CASE WHEN responseTimeMs > 0 THEN responseTimeMs ELSE NULL END), 0) as avgResponseTime,
            COALESCE(AVG(CASE WHEN tokensPerSecond > 0 THEN tokensPerSecond ELSE NULL END), 0) as avgTokensPerSec,
            COUNT(CASE WHEN role = 'assistant' THEN 1 ELSE NULL END) as totalAssistantMessages
        FROM messages
    """)
    fun getGlobalTokenAggregate(): Flow<TokenAggregate>

    @Query("""
        SELECT 
            modelUsed, 
            COUNT(*) as count, 
            SUM(totalTokens) as totalTokens 
        FROM messages 
        WHERE role = 'assistant' AND modelUsed != '' 
        GROUP BY modelUsed 
        ORDER BY count DESC
    """)
    fun getModelUsageStats(): Flow<List<ModelUsageStat>>
}
