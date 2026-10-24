package com.example.splitease.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * App-wide channel for request outcome toasts. ViewModels post messages here and
 * [RequestToastHost] shows them, so ViewModels don't need a Context.
 */
object RequestToasts {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages = _messages.asSharedFlow()

    fun success(message: String) {
        _messages.tryEmit(message)
    }

    fun failure(message: String) {
        _messages.tryEmit(message)
    }

    // e.g. failure("Couldn't add expense", e) -> "Couldn't add expense: Percentages add up to 90 ..."
    fun failure(action: String, error: Throwable) {
        val reason = when (error) {
            is HttpException -> error.response()?.serverMessage() ?: "server error (${error.code()})"
            is IOException -> "check your internet connection"
            else -> error.message
        }
        _messages.tryEmit(if (reason.isNullOrBlank()) action else "$action: $reason")
    }

    fun failure(action: String, response: Response<*>) {
        val reason = response.serverMessage() ?: "server error (${response.code()})"
        _messages.tryEmit("$action: $reason")
    }

    // The backend sends errors as {"message": "..."}
    private fun Response<*>.serverMessage(): String? =
        errorBody()?.string()
            ?.let { runCatching { JSONObject(it).optString("message") }.getOrNull() }
            ?.takeIf { it.isNotBlank() }
}

@Composable
fun RequestToastHost() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        var lastMessage: String? = null
        var lastShownAt = 0L
        RequestToasts.messages.collect { message ->
            // Several screens refresh together, so the same failure can arrive a few times at once
            val now = System.currentTimeMillis()
            if (message == lastMessage && now - lastShownAt < 3000) return@collect
            lastMessage = message
            lastShownAt = now
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
