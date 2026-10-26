package com.example.splitease.ui

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.example.splitease.data.AppContainer
import com.example.splitease.data.DefaultAppContainer
import com.example.splitease.datastore.AuthStore
import okhttp3.OkHttpClient

class AppApplication: Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
    lateinit var authStore: AuthStore

    override fun onCreate() {
        super.onCreate()
        authStore = AuthStore(this)
        container = DefaultAppContainer(authStore)
    }
    fun resetContainer() {
        // Drop the logged-out user's cached data and stop any refresh still running
        container.dataStore.close()
        container = DefaultAppContainer(authStore)
    }

    /**
     * Deletes cached bill photos, which belong to the logged-out user's groups, so the next
     * person on this phone doesn't get them. Call off the main thread: it deletes files.
     */
    @OptIn(ExperimentalCoilApi::class) // DiskCache.clear() is stable in practice but still marked experimental in Coil 2
    fun clearImageCache() {
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }

    // Bill photos are only served to group members, so Coil has to send the login token too
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { OkHttpClient.Builder().addInterceptor(AppInterceptor(authStore)).build() }
            .crossfade(true)
            .build()
}
