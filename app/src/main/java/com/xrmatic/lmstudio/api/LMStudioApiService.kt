package com.xrmatic.lmstudio.api

import com.xrmatic.lmstudio.model.ChatRequest
import com.xrmatic.lmstudio.model.ChatResponse
import com.xrmatic.lmstudio.model.ModelsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit interface for the LM Studio OpenAI-compatible REST API.
 * Base URL is set dynamically from user settings.
 */
interface LMStudioApiService {

    /** List all loaded models. */
    @GET("v1/models")
    suspend fun listModels(): Response<ModelsResponse>

    /** Send a chat-completion request. */
    @POST("v1/chat/completions")
    suspend fun chatCompletion(@Body request: ChatRequest): Response<ChatResponse>
}
