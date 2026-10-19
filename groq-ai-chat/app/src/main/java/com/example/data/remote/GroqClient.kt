package com.example.data.remote

import com.example.data.model.ChatCompletionRequest
import com.example.data.model.ChatCompletionResponse
import com.example.data.model.ChatMessageDto
import com.example.data.model.GroqModelItemDto
import com.example.data.model.GroqModelsListResponse
import com.example.data.model.StreamChunkResponse
import com.example.data.model.UsageDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

sealed class StreamEvent {
    data class Content(val delta: String) : StreamEvent()
    data class Completed(
        val totalText: String,
        val usage: UsageDto?,
        val responseTimeMs: Long,
        val tokensPerSecond: Double
    ) : StreamEvent()
    data class Error(val throwable: Throwable) : StreamEvent()
}

class GroqClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(ChatCompletionRequest::class.java)
    private val responseAdapter = moshi.adapter(ChatCompletionResponse::class.java)
    private val chunkAdapter = moshi.adapter(StreamChunkResponse::class.java)
    private val modelsAdapter = moshi.adapter(GroqModelsListResponse::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun streamChatCompletion(
        baseUrl: String,
        apiKey: String,
        request: ChatCompletionRequest
    ): Flow<StreamEvent> = callbackFlow {
        val startTime = System.currentTimeMillis()
        val accumulatedText = StringBuilder()
        var finalUsage: UsageDto? = null
        var completionTokensCount = 0

        val jsonBody = requestAdapter.toJson(request.copy(stream = true))
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toRequestBody(mediaType)

        val fullUrl = if (baseUrl.endsWith("/")) "${baseUrl}chat/completions" else "$baseUrl/chat/completions"

        val httpRequest = Request.Builder()
            .url(fullUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val call = okHttpClient.newCall(httpRequest)

        call.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                trySend(StreamEvent.Error(e))
                close(e)
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: ""
                        val message = when (response.code) {
                            401 -> "Authentication error (401): Please verify your Groq API Key in Settings."
                            429 -> "Rate limit reached (429): Groq limits exceeded. Please wait a moment."
                            404 -> "Endpoint or model not found (404): Check API Base URL or Model name."
                            else -> "Groq API Error (${response.code}): $errorBody"
                        }
                        trySend(StreamEvent.Error(IOException(message)))
                        close()
                        return
                    }

                    val body = response.body ?: throw IOException("Empty response body from Groq API")
                    val reader = BufferedReader(InputStreamReader(body.byteStream()))

                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line?.trim() ?: continue
                        if (currentLine.isEmpty() || currentLine.startsWith(":")) continue

                        if (currentLine.startsWith("data:")) {
                            val data = currentLine.removePrefix("data:").trim()
                            if (data == "[DONE]") {
                                break
                            }

                            try {
                                val chunk = chunkAdapter.fromJson(data)
                                chunk?.choices?.firstOrNull()?.delta?.content?.let { delta ->
                                    if (delta.isNotEmpty()) {
                                        accumulatedText.append(delta)
                                        completionTokensCount++
                                        trySend(StreamEvent.Content(delta))
                                    }
                                }
                                if (chunk?.usage != null) {
                                    finalUsage = chunk.usage
                                }
                            } catch (_: Exception) {
                                // Skip unparseable heartbeats or comments
                            }
                        }
                    }

                    val totalTimeMs = System.currentTimeMillis() - startTime
                    val totalCompletionTokens = finalUsage?.completionTokens
                        ?: completionTokensCount.coerceAtLeast(1)

                    val tokensPerSec = if (totalTimeMs > 0) {
                        (totalCompletionTokens.toDouble() / (totalTimeMs.toDouble() / 1000.0))
                    } else {
                        0.0
                    }

                    trySend(
                        StreamEvent.Completed(
                            totalText = accumulatedText.toString(),
                            usage = finalUsage,
                            responseTimeMs = totalTimeMs,
                            tokensPerSecond = tokensPerSec
                        )
                    )
                    close()
                } catch (e: Exception) {
                    trySend(StreamEvent.Error(e))
                    close(e)
                } finally {
                    response.close()
                }
            }
        })

        awaitClose {
            call.cancel()
        }
    }

    suspend fun directChatCompletion(
        baseUrl: String,
        apiKey: String,
        request: ChatCompletionRequest
    ): Result<ChatCompletionResponse> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val jsonBody = requestAdapter.toJson(request.copy(stream = false))
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toRequestBody(mediaType)

            val fullUrl = if (baseUrl.endsWith("/")) "${baseUrl}chat/completions" else "$baseUrl/chat/completions"

            val httpRequest = Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(httpRequest).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                val msg = when (response.code) {
                    401 -> "Invalid API Key (401). Please check your key in settings."
                    429 -> "Rate limit reached (429). Please wait a moment."
                    else -> "API Error (${response.code}): $errorBody"
                }
                return@withContext Result.failure(IOException(msg))
            }

            val bodyString = response.body?.string() ?: throw IOException("Empty response")
            val parsed = responseAdapter.fromJson(bodyString) ?: throw IOException("Failed to parse response")
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchModels(
        baseUrl: String,
        apiKey: String
    ): Result<List<GroqModelItemDto>> = withContext(Dispatchers.IO) {
        try {
            val fullUrl = if (baseUrl.endsWith("/")) "${baseUrl}models" else "$baseUrl/models"

            val httpRequest = Request.Builder()
                .url(fullUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .get()
                .build()

            val response = okHttpClient.newCall(httpRequest).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                val msg = when (response.code) {
                    401 -> "Invalid API Key (401). Please check your key in settings."
                    429 -> "Rate limit reached (429). Please wait a moment."
                    else -> "API Error (${response.code}): $errorBody"
                }
                return@withContext Result.failure(IOException(msg))
            }

            val bodyString = response.body?.string() ?: throw IOException("Empty response from /models")
            val parsed = modelsAdapter.fromJson(bodyString)
                ?: throw IOException("Failed to parse models response")
            Result.success(parsed.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
