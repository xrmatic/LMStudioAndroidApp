package com.xrmatic.lmstudio.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xrmatic.lmstudio.api.LMStudioClient
import com.xrmatic.lmstudio.model.ChatMessage
import com.xrmatic.lmstudio.model.ChatRequest
import com.xrmatic.lmstudio.model.ModelInfo
import com.xrmatic.lmstudio.model.UiMessage
import com.xrmatic.lmstudio.prefs.AppPreferences
import com.xrmatic.lmstudio.util.NetworkUtils
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)

    // ── Observable state ──────────────────────────────────────────────────

    private val _messages = MutableLiveData<List<UiMessage>>(emptyList())
    val messages: LiveData<List<UiMessage>> = _messages

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _models = MutableLiveData<List<ModelInfo>>(emptyList())
    val models: LiveData<List<ModelInfo>> = _models

    private val _selectedModel = MutableLiveData(prefs.selectedModel)
    val selectedModel: LiveData<String> = _selectedModel

    // ── Conversation history sent to the API ──────────────────────────────

    /** Full conversation history for the current session. */
    private val conversationHistory = mutableListOf<ChatMessage>()

    // ── Public API ────────────────────────────────────────────────────────

    /** Load available models from the LM Studio server. */
    fun loadModels() {
        val networkError = NetworkUtils.checkNetworkAllowed(getApplication(), prefs.wifiOnly)
        if (networkError != null) {
            _error.value = networkError
            return
        }
        viewModelScope.launch {
            try {
                val service = LMStudioClient.build(prefs)
                val response = service.listModels()
                if (response.isSuccessful) {
                    val modelList = response.body()?.data ?: emptyList()
                    _models.value = modelList
                    // Pre-select saved model or fall back to first available
                    val saved = prefs.selectedModel
                    val resolved = if (modelList.any { it.id == saved }) saved
                    else modelList.firstOrNull()?.id ?: ""
                    _selectedModel.value = resolved
                    prefs.selectedModel = resolved
                } else {
                    _error.value = "Failed to fetch models (HTTP ${response.code()})"
                }
            } catch (e: Exception) {
                _error.value = "Cannot reach server: ${e.message}"
            }
        }
    }

    /** Update the currently-selected model and persist the choice. */
    fun selectModel(modelId: String) {
        _selectedModel.value = modelId
        prefs.selectedModel = modelId
    }

    /**
     * Send a user message and append the assistant reply to the conversation.
     */
    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        val networkError = NetworkUtils.checkNetworkAllowed(getApplication(), prefs.wifiOnly)
        if (networkError != null) {
            _error.value = networkError
            return
        }

        val model = _selectedModel.value.orEmpty()
        if (model.isBlank()) {
            _error.value = "No model selected. Please wait for models to load or check your connection."
            return
        }

        // Append user message to UI
        appendUiMessage(UiMessage(role = ChatMessage.ROLE_USER, content = userText))

        // Maintain conversation history for context
        if (conversationHistory.isEmpty() && prefs.systemPrompt.isNotBlank()) {
            conversationHistory.add(ChatMessage(ChatMessage.ROLE_SYSTEM, prefs.systemPrompt))
        }
        conversationHistory.add(ChatMessage(ChatMessage.ROLE_USER, userText))

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val service = LMStudioClient.build(prefs)
                val request = ChatRequest(
                    model = model,
                    messages = conversationHistory.toList(),
                    temperature = prefs.temperature.toDouble()
                )
                val response = service.chatCompletion(request)
                if (response.isSuccessful) {
                    val assistantContent = response.body()
                        ?.choices
                        ?.firstOrNull()
                        ?.message
                        ?.content
                        ?: "(no response)"
                    conversationHistory.add(ChatMessage(ChatMessage.ROLE_ASSISTANT, assistantContent))
                    appendUiMessage(UiMessage(role = ChatMessage.ROLE_ASSISTANT, content = assistantContent))
                } else {
                    val errorMsg = "Request failed (HTTP ${response.code()})"
                    appendUiMessage(UiMessage(role = ChatMessage.ROLE_ASSISTANT, content = errorMsg, isError = true))
                }
            } catch (e: Exception) {
                val errorMsg = "Error: ${e.message}"
                appendUiMessage(UiMessage(role = ChatMessage.ROLE_ASSISTANT, content = errorMsg, isError = true))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Clear all messages and conversation history. */
    fun clearConversation() {
        conversationHistory.clear()
        _messages.value = emptyList()
    }

    /** Dismiss the current error. */
    fun clearError() {
        _error.value = null
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private fun appendUiMessage(msg: UiMessage) {
        val updated = (_messages.value ?: emptyList()) + msg
        _messages.value = updated
    }
}
