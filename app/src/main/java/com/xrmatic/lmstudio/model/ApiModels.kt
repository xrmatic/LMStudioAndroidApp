package com.xrmatic.lmstudio.model

import com.google.gson.annotations.SerializedName

// ── Chat Request ────────────────────────────────────────────────────────────

data class ChatRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<ChatMessage>,
    @SerializedName("temperature") val temperature: Double = 0.7,
    @SerializedName("max_tokens") val maxTokens: Int = -1,
    @SerializedName("stream") val stream: Boolean = false
)

// ── Chat Message ─────────────────────────────────────────────────────────────

data class ChatMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
) {
    companion object {
        const val ROLE_SYSTEM = "system"
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
    }
}

// ── Chat Response ─────────────────────────────────────────────────────────────

data class ChatResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("object") val objectType: String?,
    @SerializedName("created") val created: Long?,
    @SerializedName("model") val model: String?,
    @SerializedName("choices") val choices: List<Choice>?,
    @SerializedName("usage") val usage: Usage?
)

data class Choice(
    @SerializedName("index") val index: Int,
    @SerializedName("message") val message: ChatMessage?,
    @SerializedName("finish_reason") val finishReason: String?
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)

// ── Models List Response ──────────────────────────────────────────────────────

data class ModelsResponse(
    @SerializedName("object") val objectType: String?,
    @SerializedName("data") val data: List<ModelInfo>?
)

data class ModelInfo(
    @SerializedName("id") val id: String,
    @SerializedName("object") val objectType: String?,
    @SerializedName("owned_by") val ownedBy: String?
)

// ── UI-level message (shown in RecyclerView) ──────────────────────────────────

data class UiMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val isError: Boolean = false
)
