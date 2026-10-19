package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatMessageDto(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class StreamOptionsDto(
    @Json(name = "include_usage") val includeUsage: Boolean = true
)

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<ChatMessageDto>,
    @Json(name = "temperature") val temperature: Float = 0.7f,
    @Json(name = "max_tokens") val maxTokens: Int = 4096,
    @Json(name = "top_p") val topP: Float = 1.0f,
    @Json(name = "stream") val stream: Boolean = true,
    @Json(name = "stream_options") val streamOptions: StreamOptionsDto? = StreamOptionsDto(includeUsage = true)
)

@JsonClass(generateAdapter = true)
data class UsageDto(
    @Json(name = "prompt_tokens") val promptTokens: Int = 0,
    @Json(name = "completion_tokens") val completionTokens: Int = 0,
    @Json(name = "total_tokens") val totalTokens: Int = 0,
    @Json(name = "prompt_time") val promptTime: Double? = null,
    @Json(name = "completion_time") val completionTime: Double? = null,
    @Json(name = "total_time") val totalTime: Double? = null
)

@JsonClass(generateAdapter = true)
data class ChoiceDeltaDto(
    @Json(name = "role") val role: String? = null,
    @Json(name = "content") val content: String? = null
)

@JsonClass(generateAdapter = true)
data class ChoiceMessageDto(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class StreamChoiceDto(
    @Json(name = "index") val index: Int = 0,
    @Json(name = "delta") val delta: ChoiceDeltaDto? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class ChoiceDto(
    @Json(name = "index") val index: Int = 0,
    @Json(name = "message") val message: ChoiceMessageDto,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatCompletionResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "choices") val choices: List<ChoiceDto> = emptyList(),
    @Json(name = "usage") val usage: UsageDto? = null,
    @Json(name = "model") val model: String? = null
)

@JsonClass(generateAdapter = true)
data class StreamChunkResponse(
    @Json(name = "id") val id: String? = null,
    @Json(name = "choices") val choices: List<StreamChoiceDto> = emptyList(),
    @Json(name = "usage") val usage: UsageDto? = null,
    @Json(name = "model") val model: String? = null
)

@JsonClass(generateAdapter = true)
data class GroqModelItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "object") val objectType: String? = "model",
    @Json(name = "created") val created: Long? = 0L,
    @Json(name = "owned_by") val ownedBy: String? = null,
    @Json(name = "active") val active: Boolean? = true,
    @Json(name = "context_window") val contextWindow: Int? = null
)

@JsonClass(generateAdapter = true)
data class GroqModelsListResponse(
    @Json(name = "object") val objectType: String? = "list",
    @Json(name = "data") val data: List<GroqModelItemDto> = emptyList()
)
