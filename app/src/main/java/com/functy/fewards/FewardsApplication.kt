package com.functy.fewards

import android.app.Application
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

lateinit var fewardsApp: FewardsApplication

class FewardsApplication : Application(), ViewModelStoreOwner {

    lateinit var okhttpClient: OkHttpClient
    private val appViewModelStore by lazy { ViewModelStore() }

    override fun onCreate() {
        super.onCreate()
        fewardsApp = this

        okhttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore
}
