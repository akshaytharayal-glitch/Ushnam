package com.glad.app.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

class ApiException(val code: Int, message: String) : IOException(message)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

/** Runs [block], turning failures into a Result while letting coroutine cancellation through. */
suspend fun <T> safeApi(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

fun Throwable.userMessage(): String = when (this) {
    is UnknownHostException, is ConnectException -> "You're offline. Check your connection and try again."
    is SocketTimeoutException -> "The server took too long to respond. Try again."
    is ApiException -> when (code) {
        429 -> "Too many requests. Wait a few seconds and retry."
        401, 403 -> "Access denied. Check your API key or endpoint."
        404 -> "Nothing found."
        in 500..599 -> "The service is having trouble right now."
        else -> "Request failed ($code)."
    }
    is JSONException -> "Unexpected response from the server."
    else -> localizedMessage ?: "Something went wrong."
}

object Http {
    private const val UA = "Glad-Android/1.0"

    suspend fun get(url: String, headers: Map<String, String> = emptyMap(), timeoutMs: Int = 15_000): String =
        withContext(Dispatchers.IO) {
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = timeoutMs
                conn.readTimeout = timeoutMs
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("User-Agent", UA)
                headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
                readText(conn)
            } finally {
                conn.disconnect()
            }
        }

    suspend fun postJson(
        url: String,
        json: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Int = 60_000
    ): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", UA)
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            readText(conn)
        } finally {
            conn.disconnect()
        }
    }

    suspend fun bytes(url: String, timeoutMs: Int = 30_000): ByteArray = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", UA)
            val code = conn.responseCode
            if (code !in 200..299) throw ApiException(code, "HTTP $code")
            conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }

    private fun readText(conn: HttpURLConnection): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw ApiException(code, text.take(200))
        return text
    }
}
