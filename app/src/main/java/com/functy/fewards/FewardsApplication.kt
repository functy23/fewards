package com.functy.fewards

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.work.Configuration
import com.functy.fewards.ui.viewmodel.TaskRunner
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

lateinit var fewardsApp: FewardsApplication

class FewardsApplication : Application(), ViewModelStoreOwner, Configuration.Provider {

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

        // 进程冷启动时从本地完成标记恢复今日状态，避免主页被 TaskRunner 内存默认值盖成「未完成」。
        TaskRunner.refreshStatus()
    }

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore

    // Manifest 关掉了 WorkManagerInitializer，必须自行提供 Configuration。
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
