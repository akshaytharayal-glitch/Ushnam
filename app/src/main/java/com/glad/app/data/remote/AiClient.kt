package com.glad.app.data.remote

import com.glad.app.data.prefs.AiConfig
import org.json.JSONArray
import org.json.JSONObject

data class ChatMessage(val role: String, val content: String)

/**
 * Talks to an OpenAI-compatible chat completions endpoint.
 * Default: Pollinations' free endpoint (no key). Optional: user-supplied endpoint + key.
 * No keys are bundled in the app.
 */
class AiClient {
    suspend fun chat(config: AiConfig, messages: List<ChatMessage>): String {
        val custom = config.useCustom
        val url = if (custom) config.endpoint.trim() else FREE_URL
        if (custom && !url.startsWith("https://")) {
            throw IllegalArgumentException("Custom endpoint must start with https://")
        }
        val model = if (custom) config.model.trim().ifBlank { "gpt-4o-mini" } else "openai"
        val body = JSONObject()
            .put("model", model)
            .put("messages", JSONArray(messages.map { JSONObject().put("role", it.role).put("content", it.content) }))
        val headers = if (custom && config.apiKey.isNotBlank()) {
            mapOf("Authorization" to "Bearer ${config.apiKey.trim()}")
        } else emptyMap()

        val response = Http.postJson(url, body.toString(), headers, timeoutMs = 60_000)
        val text = JSONObject(response)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .optString("content", "")
            .trim()
        if (text.isEmpty()) throw IllegalStateException("The AI returned an empty answer. Try again.")
        return text
    }

    private companion object {
        const val FREE_URL = "https://text.pollinations.ai/openai"
    }
}
