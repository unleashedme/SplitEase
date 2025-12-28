package com.example.splitease

import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.splitease.data.Repository
import com.example.splitease.ui.AppApplication
import com.example.splitease.ui.SplitEaseRoot
import com.example.splitease.ui.screens.NotificationSetup
import com.example.splitease.ui.theme.SplitEaseTheme
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()

        val appContainer = (application as AppApplication).container
        val repository = appContainer.repository

        enableEdgeToEdge()
        setContent {
            SplitEaseTheme {
                NotificationSetup(repository)
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    SplitEaseRoot()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "split_alerts"
            val name = "SplitEase Alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance)

            val manager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}


object FCMHandler {
    fun syncTokenWithServer(repository: Repository,  newToken: String? = null) {
        if (newToken != null) {
            sendTokenToBackend(repository, newToken)
        } else {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    sendTokenToBackend(repository, task.result)
                }
            }
        }
    }

    private fun sendTokenToBackend(
        repository: Repository,
        newToken: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = repository.updateFcmToken(newToken)
                if (response.isSuccessful) {
                    Log.d("FCM_SYNC", "Token successfully updated on server")
                }
            } catch (e: Exception) {
                Log.e("FCM_SYNC", "Network error during token sync: ${e.message}")
            }
        }
    }
}


