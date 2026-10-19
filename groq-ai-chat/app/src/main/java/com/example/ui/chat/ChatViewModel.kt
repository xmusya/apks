package com.example.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.ModelUsageStat
import com.example.data.local.TokenAggregate
import com.example.data.model.GroqModelInfo
import com.example.data.model.GroqModelRegistry
import com.example.data.preferences.AppSettings
import com.example.data.preferences.UserPreferences
import com.example.data.remote.StreamEvent
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionStats(
    val promptTokens: Long = 0L,
    val completionTokens: Long = 0L,
    val totalTokens: Long = 0L,
    val avgResponseTimeMs: Double = 0.0,
    val avgTokensPerSec: Double = 0.0,
    val messageCount: Int = 0
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val preferences = UserPreferences(application)
    private val repository = ChatRepository(
        conversationDao = db.conversationDao(),
        messageDao = db.messageDao(),
        preferences = preferences
    )

    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = preferences.settings.value
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val rawConversations: StateFlow<List<ConversationEntity>> = repository.conversations.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredConversations: StateFlow<List<ConversationEntity>> = combine(
        rawConversations,
        _searchQuery
    ) { convs, query ->
        if (query.isBlank()) {
            convs
        } else {
            convs.filter { it.title.contains(query, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()

    private val _liveResponseTimeMs = MutableStateFlow(0L)
    val liveResponseTimeMs: StateFlow<Long> = _liveResponseTimeMs.asStateFlow()

    private val _liveTokensPerSec = MutableStateFlow(0.0)
    val liveTokensPerSec: StateFlow<Double> = _liveTokensPerSec.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val globalStats: StateFlow<TokenAggregate> = repository.globalStats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TokenAggregate()
    )

    val modelStats: StateFlow<List<ModelUsageStat>> = repository.modelStats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _sessionStats = MutableStateFlow(SessionStats())
    val sessionStats: StateFlow<SessionStats> = _sessionStats.asStateFlow()

    private val _availableModels = MutableStateFlow<List<GroqModelInfo>>(GroqModelRegistry.DEFAULT_MODELS)
    val availableModels: StateFlow<List<GroqModelInfo>> = _availableModels.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels.asStateFlow()

    private val _modelsFetchError = MutableStateFlow<String?>(null)
    val modelsFetchError: StateFlow<String?> = _modelsFetchError.asStateFlow()

    private var streamJob: Job? = null
    private var observeMessagesJob: Job? = null

    init {
        viewModelScope.launch {
            rawConversations.collect { convList ->
                // Auto-select first conversation if none selected and list is not empty
                if (_activeConversationId.value == null && convList.isNotEmpty()) {
                    selectConversation(convList.first().id)
                }
            }
        }

        // Fetch models from Groq API if API key is present
        refreshModels()
    }

    fun refreshModels() {
        viewModelScope.launch {
            val key = settings.value.apiKey
            if (key.isBlank()) return@launch

            _isLoadingModels.value = true
            _modelsFetchError.value = null

            val result = repository.fetchRemoteModels()
            result.onSuccess { models ->
                if (models.isNotEmpty()) {
                    _availableModels.value = models
                }
            }.onFailure { e ->
                _modelsFetchError.value = e.localizedMessage ?: "Failed to fetch models"
            }
            _isLoadingModels.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectConversation(id: String) {
        if (_activeConversationId.value == id) return
        _activeConversationId.value = id
        observeMessagesForActiveConversation(id)
    }

    private fun observeMessagesForActiveConversation(conversationId: String) {
        observeMessagesJob?.cancel()
        observeMessagesJob = viewModelScope.launch {
            repository.getMessages(conversationId).collect { msgList ->
                _messages.value = msgList
            }
        }
    }

    fun createNewConversation(title: String = "New Chat") {
        viewModelScope.launch {
            val currentModel = settings.value.effectiveModel
            val newConv = repository.createConversation(
                title = title,
                modelId = currentModel,
                systemPrompt = settings.value.systemPrompt
            )
            _activeConversationId.value = newConv.id
            observeMessagesForActiveConversation(newConv.id)
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            val isCurrent = _activeConversationId.value == id
            repository.deleteConversation(id)
            if (isCurrent) {
                val remaining = rawConversations.value.filter { it.id != id }
                if (remaining.isNotEmpty()) {
                    selectConversation(remaining.first().id)
                } else {
                    _activeConversationId.value = null
                    _messages.value = emptyList()
                }
            }
        }
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            repository.updateConversationTitle(id, newTitle)
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            repository.clearAll()
            _activeConversationId.value = null
            _messages.value = emptyList()
        }
    }

    fun updateConversationModel(conversationId: String, modelId: String) {
        viewModelScope.launch {
            repository.updateConversationModel(conversationId, modelId)
        }
    }

    fun sendMessage(promptText: String) {
        val trimmed = promptText.trim()
        if (trimmed.isEmpty() || _isStreaming.value) return

        val apiKey = settings.value.apiKey.trim()
        if (apiKey.isEmpty()) {
            _errorMessage.value = "Please configure your Groq API Key in Settings to start chatting."
            return
        }

        viewModelScope.launch {
            var targetConversationId = _activeConversationId.value
            if (targetConversationId == null) {
                val currentModel = settings.value.effectiveModel
                val newConv = repository.createConversation(
                    title = if (trimmed.length > 25) trimmed.take(25) + "..." else trimmed,
                    modelId = currentModel,
                    systemPrompt = settings.value.systemPrompt
                )
                targetConversationId = newConv.id
                _activeConversationId.value = targetConversationId
                observeMessagesForActiveConversation(targetConversationId)
            }

            // Save user message to database
            repository.insertUserMessage(targetConversationId, trimmed)

            // Prepare context
            val contextMessages = repository.getContextMessages(targetConversationId)
            val currentSettings = settings.value
            val effectiveModel = currentSettings.effectiveModel

            _isStreaming.value = true
            _streamingContent.value = ""
            _liveResponseTimeMs.value = 0L
            _liveTokensPerSec.value = 0.0

            val startTime = System.currentTimeMillis()

            streamJob?.cancel()
            streamJob = launch {
                try {
                    repository.streamGroq(
                        requestMessages = contextMessages,
                        model = effectiveModel,
                        temperature = currentSettings.temperature,
                        maxTokens = currentSettings.maxTokens
                    ).collect { event ->
                        when (event) {
                            is StreamEvent.Content -> {
                                _streamingContent.value += event.delta
                                val elapsed = System.currentTimeMillis() - startTime
                                _liveResponseTimeMs.value = elapsed
                                if (elapsed > 0) {
                                    val approxTokens = (_streamingContent.value.length / 4).coerceAtLeast(1)
                                    _liveTokensPerSec.value = (approxTokens.toDouble() / (elapsed.toDouble() / 1000.0))
                                }
                            }
                            is StreamEvent.Completed -> {
                                _isStreaming.value = false
                                val finalPromptTokens = event.usage?.promptTokens
                                    ?: (contextMessages.sumOf { it.content.length } / 4)
                                val finalCompletionTokens = event.usage?.completionTokens
                                    ?: (event.totalText.length / 4).coerceAtLeast(1)

                                repository.saveAssistantMessage(
                                    conversationId = targetConversationId,
                                    content = event.totalText,
                                    promptTokens = finalPromptTokens,
                                    completionTokens = finalCompletionTokens,
                                    responseTimeMs = event.responseTimeMs,
                                    tokensPerSec = event.tokensPerSecond,
                                    modelUsed = effectiveModel
                                )

                                _sessionStats.update { curr ->
                                    val count = curr.messageCount + 1
                                    val prompt = curr.promptTokens + finalPromptTokens
                                    val compl = curr.completionTokens + finalCompletionTokens
                                    val total = curr.totalTokens + finalPromptTokens + finalCompletionTokens
                                    val avgTime = if (count > 0) ((curr.avgResponseTimeMs * curr.messageCount) + event.responseTimeMs) / count else event.responseTimeMs.toDouble()
                                    val avgSpeed = if (event.tokensPerSecond > 0) {
                                        ((curr.avgTokensPerSec * curr.messageCount) + event.tokensPerSecond) / count
                                    } else curr.avgTokensPerSec
                                    SessionStats(
                                        promptTokens = prompt,
                                        completionTokens = compl,
                                        totalTokens = total,
                                        avgResponseTimeMs = avgTime,
                                        avgTokensPerSec = avgSpeed,
                                        messageCount = count
                                    )
                                }

                                _streamingContent.value = ""
                            }
                            is StreamEvent.Error -> {
                                _isStreaming.value = false
                                _errorMessage.value = event.throwable.localizedMessage
                                    ?: "Failed to communicate with Groq API"
                            }
                        }
                    }
                } catch (e: Exception) {
                    _isStreaming.value = false
                    _errorMessage.value = e.localizedMessage ?: "Unexpected error occurred"
                }
            }
        }
    }

    fun cancelStreaming() {
        streamJob?.cancel()
        _isStreaming.value = false
        val partial = _streamingContent.value
        val convId = _activeConversationId.value
        if (partial.isNotBlank() && convId != null) {
            viewModelScope.launch {
                val elapsed = _liveResponseTimeMs.value
                val approxTokens = (partial.length / 4).coerceAtLeast(1)
                val tokPerSec = if (elapsed > 0) (approxTokens.toDouble() / (elapsed.toDouble() / 1000.0)) else 0.0
                repository.saveAssistantMessage(
                    conversationId = convId,
                    content = partial,
                    promptTokens = 0,
                    completionTokens = approxTokens,
                    responseTimeMs = elapsed,
                    tokensPerSec = tokPerSec,
                    modelUsed = settings.value.effectiveModel
                )
                _streamingContent.value = ""
            }
        } else {
            _streamingContent.value = ""
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    // Settings actions
    fun setApiKey(key: String) {
        repository.updateApiKey(key)
        if (key.isNotBlank()) {
            refreshModels()
        }
    }

    fun setBaseUrl(url: String) {
        repository.updateBaseUrl(url)
        if (settings.value.apiKey.isNotBlank()) {
            refreshModels()
        }
    }
    fun setModel(model: String) = repository.updateModel(model)
    fun setCustomModel(model: String) = repository.updateCustomModel(model)
    fun setTemperature(temp: Float) = repository.updateTemperature(temp)
    fun setMaxTokens(tokens: Int) = repository.updateMaxTokens(tokens)
    fun setSystemPrompt(prompt: String) = repository.updateSystemPrompt(prompt)
    fun setStreamResponse(enabled: Boolean) = repository.updateStreamResponse(enabled)
    fun setThemeMode(mode: String) = repository.updateThemeMode(mode)
    fun setUseDynamicColors(enabled: Boolean) = repository.updateUseDynamicColors(enabled)
}
