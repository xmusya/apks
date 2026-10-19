package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.GroqModelRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val apiKey: String,
    val baseUrl: String,
    val selectedModel: String,
    val customModel: String,
    val temperature: Float,
    val maxTokens: Int,
    val systemPrompt: String,
    val streamResponse: Boolean,
    val themeMode: String, // "system", "dark", "light"
    val useDynamicColors: Boolean
) {
    val effectiveModel: String
        get() = if (selectedModel == "custom" && customModel.isNotBlank()) customModel.trim() else selectedModel
}

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("groq_chat_prefs", Context.MODE_PRIVATE)

    private val initialApiKey: String = run {
        val saved = prefs.getString(KEY_API_KEY, null)
        if (!saved.isNullOrBlank()) {
            saved
        } else {
            // Check BuildConfig safely if injected via Secrets / .env
            try {
                val field = com.example.BuildConfig::class.java.getField("GROQ_API_KEY")
                val value = field.get(null) as? String
                if (!value.isNullOrBlank() && !value.startsWith("MY_")) value else ""
            } catch (_: Throwable) {
                ""
            }
        }
    }

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            apiKey = prefs.getString(KEY_API_KEY, initialApiKey) ?: initialApiKey,
            baseUrl = prefs.getString(KEY_BASE_URL, GroqModelRegistry.DEFAULT_BASE_URL)
                ?: GroqModelRegistry.DEFAULT_BASE_URL,
            selectedModel = prefs.getString(KEY_MODEL, GroqModelRegistry.DEFAULT_MODEL)
                ?: GroqModelRegistry.DEFAULT_MODEL,
            customModel = prefs.getString(KEY_CUSTOM_MODEL, "") ?: "",
            temperature = prefs.getFloat(KEY_TEMPERATURE, 0.7f),
            maxTokens = prefs.getInt(KEY_MAX_TOKENS, 4096),
            systemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT)
                ?: DEFAULT_SYSTEM_PROMPT,
            streamResponse = prefs.getBoolean(KEY_STREAM, true),
            themeMode = prefs.getString(KEY_THEME_MODE, "system") ?: "system",
            useDynamicColors = prefs.getBoolean(KEY_DYNAMIC_COLORS, true)
        )
    }

    fun updateApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
        _settings.value = loadSettings()
    }

    fun updateBaseUrl(url: String) {
        val normalized = if (url.endsWith("/")) url else "$url/"
        prefs.edit().putString(KEY_BASE_URL, normalized).apply()
        _settings.value = loadSettings()
    }

    fun updateModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model).apply()
        _settings.value = loadSettings()
    }

    fun updateCustomModel(model: String) {
        prefs.edit().putString(KEY_CUSTOM_MODEL, model.trim()).apply()
        _settings.value = loadSettings()
    }

    fun updateTemperature(temp: Float) {
        prefs.edit().putFloat(KEY_TEMPERATURE, temp).apply()
        _settings.value = loadSettings()
    }

    fun updateMaxTokens(tokens: Int) {
        prefs.edit().putInt(KEY_MAX_TOKENS, tokens).apply()
        _settings.value = loadSettings()
    }

    fun updateSystemPrompt(prompt: String) {
        prefs.edit().putString(KEY_SYSTEM_PROMPT, prompt).apply()
        _settings.value = loadSettings()
    }

    fun updateStreamResponse(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STREAM, enabled).apply()
        _settings.value = loadSettings()
    }

    fun updateThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _settings.value = loadSettings()
    }

    fun updateUseDynamicColors(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLORS, enabled).apply()
        _settings.value = loadSettings()
    }

    companion object {
        private const val KEY_API_KEY = "key_api_key"
        private const val KEY_BASE_URL = "key_base_url"
        private const val KEY_MODEL = "key_selected_model"
        private const val KEY_CUSTOM_MODEL = "key_custom_model"
        private const val KEY_TEMPERATURE = "key_temperature"
        private const val KEY_MAX_TOKENS = "key_max_tokens"
        private const val KEY_SYSTEM_PROMPT = "key_system_prompt"
        private const val KEY_STREAM = "key_stream_response"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_DYNAMIC_COLORS = "key_dynamic_colors"

        const val DEFAULT_SYSTEM_PROMPT =
            "You are Groq AI, an exceptionally fast, concise, accurate, and helpful AI assistant powered by Groq's high-speed Language Processing Units (LPUs)."
    }
}
