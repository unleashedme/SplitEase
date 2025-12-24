package com.example.splitease.ui

import com.example.splitease.datastore.AuthStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AppInterceptor(private val authStore: AuthStore): Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        val request = chain.request()

        if (request.url.encodedPath.contains("/login")) {
            return chain.proceed(request)
        }

        val token = runBlocking {
            authStore.getToken()
        }

        val newRequest = request.newBuilder()
            .apply {
                token?.let {
                    addHeader("Authorization", "Bearer $it")
                }
            }
            .build()

        return chain.proceed(newRequest)
    }

}
//What is an Interceptor (conceptually)
//
//An Interceptor is a piece of code that runs before every HTTP request (and/or after every response).


//Why AuthInterceptor exists at all?
//
//Because after login:
//
//Backend requires JWT on every request
//
//You don’t want to manually add headers everywhere
//
//AuthInterceptor:
//
//Runs before every request
//
//Reads token from AuthStore
//
//Adds Authorization header