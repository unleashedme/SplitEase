package com.example.splitease.ui

import android.app.Application
import com.example.splitease.data.AppContainer
import com.example.splitease.data.DefaultAppContainer
import com.example.splitease.datastore.AuthStore

class AppApplication: Application() {
    lateinit var container: AppContainer
    lateinit var authStore: AuthStore

    override fun onCreate() {
        super.onCreate()
        authStore = AuthStore(this)
        container = DefaultAppContainer(authStore)
    }
    fun resetContainer() {
        container = DefaultAppContainer(authStore)
    }
}