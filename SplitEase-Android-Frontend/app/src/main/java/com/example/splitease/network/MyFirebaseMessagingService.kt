package com.example.splitease.network

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.splitease.FCMHandler
import com.example.splitease.R
import com.example.splitease.ui.AppApplication
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MyFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * Called when a message is received while the app is in the FOREGROUND.
     * By default, Firebase does not show a banner if the app is open.
     * We must manually build it here.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)


        val prefs = runBlocking {
            val authStore = (application as AppApplication).authStore
            authStore.profileUiStateFlow.first()
        }

        if (prefs.isNotificationsEnabled) {
            val title = remoteMessage.notification?.title ?: "SplitEase"
            val body = remoteMessage.notification?.body ?: ""
            sendNotification(title, body)
        }

    }

    //Called when Google refreshes the device token.
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val appContainer = (application as AppApplication).container
        val repository = appContainer.repository
        val authStore = (application as AppApplication).authStore

        CoroutineScope(Dispatchers.IO).launch {
            val token = authStore.getToken()
            if (!token.isNullOrEmpty()) {
                try{
                    val response = repository.updateFcmToken(token)
                    if (response.isSuccessful) {
                        Log.d("FCM_SERVICE", "New token synced successfully: $token")
                    }
                }catch(e: Exception){
                    Log.e("FCM_SERVICE", "Failed to sync new token: ${e.message}")
                }
            }

        }

        FCMHandler.syncTokenWithServer(repository, token)
    }

    private fun sendNotification(title: String, messageBody: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "split_alerts"

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.notification_96)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)


        notificationManager.notify(0, notificationBuilder.build())
    }
}