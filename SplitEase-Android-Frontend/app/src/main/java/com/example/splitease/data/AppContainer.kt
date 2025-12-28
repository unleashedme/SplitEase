package com.example.splitease.data

import com.example.splitease.network.ApiService
import com.example.splitease.datastore.AuthStore
import com.example.splitease.ui.AppInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory


interface AppContainer{
    val repository: Repository
}

class DefaultAppContainer(authStore: AuthStore): AppContainer {

    private val baseUrl = "https://jose-unsacramental-incuriously.ngrok-free.dev"
        // "http://10.0.2.2:8080/"
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory()) // required for Kotlin data classes to convert JSON to text
        .build()


    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AppInterceptor(authStore))
        .build()
    private val retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .baseUrl(baseUrl)
        .build()

    private val retrofitService: ApiService by lazy{
        retrofit.create(ApiService::class.java)
    }

    override val repository: Repository by lazy{
        NetworkRepository(retrofitService)
    }
}