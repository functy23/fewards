package com.functy.fewards.ui.component.miuix

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.functy.fewards.core.bing.BingOAuth

/**
 * Microsoft 账号授权用内嵌浏览器。
 *
 * 打开 [url]（login.live.com 的授权页），用户正常登录（含 2FA / passkey）。
 * 登录成功后微软会跳向 `oauth20_desktop.srf?code=…`，我们在**加载之前**用
 * [BingOAuth.isRedirect] 拦下并把完整的回跳地址交给 [onRedirect]，
 * 因此那个桌面端页面永远不会真的被渲染出来。
 *
 * 说明：
 *  - 用标准 Chrome 的 UA，覆盖掉 WebView 默认 UA 里的 `; wv)` 标记——
 *    部分登录页会据此判断「内嵌浏览器不支持」而拒绝渲染；
 *  - 开启第三方 Cookie，否则登录跳转会丢会话；
 *  - 只在 login.live.com 域内使用，不外加载第三方脚本（[WebViewClient] 不放开新的窗口）。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BingAuthWebView(
    url: String,
    onRedirect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnRedirect by rememberUpdatedState(onRedirect)
    val loadedUrl = remember { mutableStateOf("") }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef.value?.apply {
                stopLoading()
                webViewClient = WebViewClient()
                destroy()
            }
            webViewRef.value = null
        }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(RoundedCornerShape(20.dp)),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = BROWSER_USER_AGENT
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        val target = request.url.toString()
                        if (BingOAuth.isRedirect(target)) {
                            currentOnRedirect(target)
                            return true
                        }
                        return false
                    }

                    /** 兜底：某些跳转不走 shouldOverrideUrlLoading。 */
                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        if (url != null && BingOAuth.isRedirect(url)) {
                            view.stopLoading()
                            currentOnRedirect(url)
                        }
                    }
                }
                loadedUrl.value = url
                loadUrl(url)
                webViewRef.value = this
            }
        },
        update = { view ->
            if (loadedUrl.value != url) {
                loadedUrl.value = url
                view.loadUrl(url)
            }
        },
    )
}

/** 去掉 WebView 的 `; wv)` 标记，避免登录页判定「内嵌浏览器不支持」。 */
private const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
