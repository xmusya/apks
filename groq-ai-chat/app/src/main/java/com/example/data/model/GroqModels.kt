package com.example.data.model

data class GroqModelInfo(
    val id: String,
    val name: String,
    val provider: String,
    val contextWindow: String,
    val description: String,
    val isRecommended: Boolean = false,
    val speedCategory: String = "Ultra-Fast",
    val isActive: Boolean = true
)

object GroqModelRegistry {
    // Helper to format raw model IDs from api.groq.com/openai/v1/models into readable info
    fun fromDto(dto: GroqModelItemDto): GroqModelInfo {
        val id = dto.id
        val provider = when {
            id.contains("llama", ignoreCase = true) -> "Meta"
            id.contains("deepseek", ignoreCase = true) -> "DeepSeek"
            id.contains("qwen", ignoreCase = true) -> "Alibaba Cloud"
            id.contains("mixtral", ignoreCase = true) || id.contains("mistral", ignoreCase = true) -> "Mistral AI"
            id.contains("gemma", ignoreCase = true) -> "Google"
            id.contains("whisper", ignoreCase = true) -> "OpenAI"
            else -> dto.ownedBy?.replaceFirstChar { it.uppercase() } ?: "Groq"
        }

        val name = id
            .replace("-", " ")
            .split(" ")
            .joinToString(" ") { word ->
                when (word.lowercase()) {
                    "it" -> "IT"
                    "versatile" -> "Versatile"
                    "instant" -> "Instant"
                    "preview" -> "Preview"
                    else -> word.replaceFirstChar { it.uppercase() }
                }
            }

        val contextStr = if (dto.contextWindow != null && dto.contextWindow > 0) {
            "${dto.contextWindow / 1024}k"
        } else {
            when {
                id.contains("128k") -> "128k"
                id.contains("32768") || id.contains("32k") -> "32k"
                id.contains("8192") || id.contains("8k") -> "8k"
                else -> "Default"
            }
        }

        val speed = when {
            id.contains("instant", ignoreCase = true) || id.contains("3b", ignoreCase = true) || id.contains("1b", ignoreCase = true) -> "Blazing (~550-700 tok/s)"
            id.contains("70b", ignoreCase = true) -> "Fast (~250-280 tok/s)"
            id.contains("8b", ignoreCase = true) -> "Ultra-Fast (~450 tok/s)"
            else -> "Fast (Groq LPU)"
        }

        return GroqModelInfo(
            id = id,
            name = name,
            provider = provider,
            contextWindow = contextStr,
            description = "Groq LPU accelerated inference (${dto.ownedBy ?: "groq"})",
            isRecommended = id.contains("llama-3.3-70b") || id.contains("llama-3.1-8b"),
            speedCategory = speed,
            isActive = dto.active ?: true
        )
    }

    val DEFAULT_MODELS = listOf(
        GroqModelInfo(
            id = "llama-3.3-70b-versatile",
            name = "Llama 3.3 70B Versatile",
            provider = "Meta",
            contextWindow = "128k",
            description = "State-of-the-art flagship 70B model with high reasoning capability and massive context.",
            isRecommended = true,
            speedCategory = "Fast (~250 tok/s)"
        ),
        GroqModelInfo(
            id = "llama-3.1-8b-instant",
            name = "Llama 3.1 8B Instant",
            provider = "Meta",
            contextWindow = "128k",
            description = "Extremely fast, low-latency 8B model ideal for general chat and quick answers.",
            isRecommended = true,
            speedCategory = "Blazing (~550 tok/s)"
        ),
        GroqModelInfo(
            id = "deepseek-r1-distill-llama-70b",
            name = "DeepSeek R1 Distill 70B",
            provider = "DeepSeek / Meta",
            contextWindow = "128k",
            description = "Specialized reasoning and complex step-by-step problem solving at Groq LPU speed.",
            isRecommended = true,
            speedCategory = "Fast (~220 tok/s)"
        ),
        GroqModelInfo(
            id = "qwen-2.5-coder-32b",
            name = "Qwen 2.5 Coder 32B",
            provider = "Alibaba Cloud",
            contextWindow = "128k",
            description = "Top-tier programming, refactoring, algorithms and debugging assistant.",
            isRecommended = false,
            speedCategory = "Ultra-Fast (~380 tok/s)"
        ),
        GroqModelInfo(
            id = "llama3-8b-tool-use-preview",
            name = "Llama 3 8B Tool Use Preview",
            provider = "Meta",
            contextWindow = "8k",
            description = "Specialized model fine-tuned for tool execution, function calling, and structured JSON output.",
            isRecommended = false,
            speedCategory = "Ultra-Fast (~450 tok/s)"
        ),
        GroqModelInfo(
            id = "mixtral-8x7b-32768",
            name = "Mixtral 8x7B (32b-instruct)",
            provider = "Mistral AI",
            contextWindow = "32k",
            description = "High-quality Mixture-of-Experts architecture with balanced multilingual performance.",
            isRecommended = false,
            speedCategory = "Very Fast (~480 tok/s)"
        ),
        GroqModelInfo(
            id = "gemma2-9b-it",
            name = "Gemma 2 9B IT",
            provider = "Google",
            contextWindow = "8k",
            description = "Google's lightweight, capable instruction-tuned architecture running on Groq LPUs.",
            isRecommended = false,
            speedCategory = "Blazing (~500 tok/s)"
        ),
        GroqModelInfo(
            id = "llama-3.2-3b-preview",
            name = "Llama 3.2 3B Preview",
            provider = "Meta",
            contextWindow = "128k",
            description = "Compact, lightning-fast edge-scale model with astonishing response times.",
            isRecommended = false,
            speedCategory = "Max Speed (~700+ tok/s)"
        ),
        GroqModelInfo(
            id = "llama-3.2-1b-preview",
            name = "Llama 3.2 1B Preview",
            provider = "Meta",
            contextWindow = "128k",
            description = "Ultra-lightweight edge model with sub-second response times.",
            isRecommended = false,
            speedCategory = "Max Speed (~800+ tok/s)"
        )
    )

    const val DEFAULT_MODEL = "llama-3.3-70b-versatile"
    const val DEFAULT_BASE_URL = "https://api.groq.com/openai/v1/"
}
