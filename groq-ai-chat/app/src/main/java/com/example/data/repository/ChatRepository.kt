package com.example.data.repository

import com.example.data.local.ConversationDao
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import com.example.data.local.ModelUsageStat
import com.example.data.local.TokenAggregate
import com.example.data.model.ChatCompletionRequest
import com.example.data.model.ChatMessageDto
import com.example.data.preferences.AppSettings
import com.example.data.preferences.UserPreferences
import com.example.data.remote.GroqClient
import com.example.data.remote.StreamEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val preferences: UserPreferences,
    private val groqClient: GroqClient = GroqClient()
) {
    val settings: Flow<AppSettings> = preferences.settings

    val conversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()
        .flowOn(Dispatchers.IO)

    fun getMessages(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForConversation(conversationId)
            .flowOn(Dispatchers.IO)

    fun getConversation(conversationId: String): Flow<ConversationEntity?> =
        conversationDao.getConversationById(conversationId)
            .flowOn(Dispatchers.IO)

    val globalStats: Flow<TokenAggregate> = messageDao.getGlobalTokenAggregate()
        .flowOn(Dispatchers.IO)

    val modelStats: Flow<List<ModelUsageStat>> = messageDao.getModelUsageStats()
        .flowOn(Dispatchers.IO)

    suspend fun createConversation(
        title: String = "New Chat",
        modelId: String,
        systemPrompt: String = ""
    ): ConversationEntity = withContext(Dispatchers.IO) {
        val newConv = ConversationEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            modelId = modelId,
            systemPrompt = systemPrompt
        )
        conversationDao.insertConversation(newConv)
        newConv
    }

    suspend fun updateConversationTitle(id: String, title: String) = withContext(Dispatchers.IO) {
        conversationDao.updateTitle(id, title.trim(), System.currentTimeMillis())
    }

    suspend fun updateConversationModel(id: String, modelId: String) = withContext(Dispatchers.IO) {
        conversationDao.updateModel(id, modelId, System.currentTimeMillis())
    }

    suspend fun deleteConversation(id: String) = withContext(Dispatchers.IO) {
        messageDao.deleteMessagesForConversation(id)
        conversationDao.deleteConversationById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        messageDao.clearAllMessages()
        conversationDao.clearAllConversations()
    }

    suspend fun insertUserMessage(
        conversationId: String,
        content: String
    ): MessageEntity = withContext(Dispatchers.IO) {
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = "user",
            content = content.trim(),
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(message)

        // Check if conversation needs an updated title
        val conv = conversationDao.getConversationByIdOnce(conversationId)
        if (conv != null && (conv.title == "New Chat" || conv.title.isBlank())) {
            val previewTitle = if (content.length > 32) {
                content.take(30).trim() + "..."
            } else {
                content.trim()
            }
            conversationDao.updateTitle(conversationId, previewTitle, System.currentTimeMillis())
        }

        message
    }

    suspend fun saveAssistantMessage(
        conversationId: String,
        content: String,
        promptTokens: Int,
        completionTokens: Int,
        responseTimeMs: Long,
        tokensPerSec: Double,
        modelUsed: String
    ): MessageEntity = withContext(Dispatchers.IO) {
        val total = promptTokens + completionTokens
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = "assistant",
            content = content.trim(),
            timestamp = System.currentTimeMillis(),
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = total,
            responseTimeMs = responseTimeMs,
            tokensPerSecond = tokensPerSec,
            modelUsed = modelUsed
        )
        messageDao.insertMessage(message)
        conversationDao.addTokens(conversationId, promptTokens, completionTokens, System.currentTimeMillis())
        message
    }

    suspend fun deleteMessage(id: String) = withContext(Dispatchers.IO) {
        messageDao.deleteMessageById(id)
    }

    suspend fun getContextMessages(conversationId: String): List<ChatMessageDto> = withContext(Dispatchers.IO) {
        val dbMessages = messageDao.getMessagesForConversationOnce(conversationId)
        val conv = conversationDao.getConversationByIdOnce(conversationId)
        val currentSettings = preferences.settings.value

        val dtoList = mutableListOf<ChatMessageDto>()

        // System prompt
        val effectiveSystemPrompt = when {
            conv?.systemPrompt?.isNotBlank() == true -> conv.systemPrompt
            currentSettings.systemPrompt.isNotBlank() -> currentSettings.systemPrompt
            else -> null
        }
        if (!effectiveSystemPrompt.isNullOrBlank()) {
            dtoList.add(ChatMessageDto(role = "system", content = effectiveSystemPrompt))
        }

        // Add history messages
        dbMessages.forEach { msg ->
            if (msg.role == "user" || msg.role == "assistant") {
                dtoList.add(ChatMessageDto(role = msg.role, content = msg.content))
            }
        }
        dtoList
    }

    fun streamGroq(
        requestMessages: List<ChatMessageDto>,
        model: String,
        temperature: Float,
        maxTokens: Int
    ): Flow<StreamEvent> {
        val currentSettings = preferences.settings.value
        val request = ChatCompletionRequest(
            model = model,
            messages = requestMessages,
            temperature = temperature,
            maxTokens = maxTokens,
            stream = true
        )
        return groqClient.streamChatCompletion(
            baseUrl = currentSettings.baseUrl,
            apiKey = currentSettings.apiKey,
            request = request
        )
    }

    // Preferences forwarders
    fun updateApiKey(key: String) = preferences.updateApiKey(key)
    fun updateBaseUrl(url: String) = preferences.updateBaseUrl(url)
    fun updateModel(model: String) = preferences.updateModel(model)
    fun updateCustomModel(model: String) = preferences.updateCustomModel(model)
    fun updateTemperature(temp: Float) = preferences.updateTemperature(temp)
    fun updateMaxTokens(tokens: Int) = preferences.updateMaxTokens(tokens)
    fun updateSystemPrompt(prompt: String) = preferences.updateSystemPrompt(prompt)
    fun updateStreamResponse(enabled: Boolean) = preferences.updateStreamResponse(enabled)
    fun updateThemeMode(mode: String) = preferences.updateThemeMode(mode)
    fun updateUseDynamicColors(enabled: Boolean) = preferences.updateUseDynamicColors(enabled)

    suspend fun fetchRemoteModels(): Result<List<com.example.data.model.GroqModelInfo>> {
        val currentSettings = preferences.settings.value
        val result = groqClient.fetchModels(
            baseUrl = currentSettings.baseUrl,
            apiKey = currentSettings.apiKey
        )
        return result.map { list ->
            list.map { com.example.data.model.GroqModelRegistry.fromDto(it) }
        }
    }
}
