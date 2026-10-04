package com.functy.fewards.core.bing

/**
 * Bing / Microsoft Rewards 协议常量（dapi 代）。
 *
 * 重要：本模块走的是 **新版 dapi 后端**（prod.rewardsplatform.microsoft.com/dapi），
 * 不是 legacy 的 rewards.bing.com/api/getuserinfo + reportactivity。
 * 2026 年 rewards.bing.com 已重写为 Next.js SPA，旧 DOM 抓取路线失效；dapi 正是
 * 官方 Bing 安卓 App 自己用的接口，本 App 以 `channel=SAAndroid` 冒用同一通道。
 *
 * 鉴权是 OAuth 授权码 + refresh_token（公开客户端，无 client_secret）。
 * 端点 / client_id / 字段名属于事实性协议信息；实现为本项目自行重写。
 */
object BingConstants {

    // ==================== OAuth（login.live.com 消费级客户端） ====================

    /** 公开客户端 id（Bing 系应用通用，无 client_secret）。 */
    const val CLIENT_ID = "0000000040170455"

    /** 授权码回跳地址。桌面客户端专用页，App 内由 WebView 拦截，不真正加载。 */
    const val REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf"

    const val SCOPE = "service::prod.rewardsplatform.microsoft.com::MBI_SSL"
    const val AUTHORIZE_URL = "https://login.live.com/oauth20_authorize.srf"
    const val TOKEN_URL = "https://login.live.com/oauth20_token.srf"

    // ==================== dapi ====================

    const val DAPI_BASE = "https://prod.rewardsplatform.microsoft.com/dapi"
    const val CHANNEL = "SAAndroid"

    /** `/dapi/me` 的 options 位掩码；511 与 613 均在社区实现中实测可用。 */
    const val OPTIONS = "511"

    /** App 通道标识。带上更贴近官方 App 的请求特征。 */
    const val APP_ID = "SAAndroid/31.4.2110003555"

    /**
     * 默认地区。国区账号用小写 `cn`（社区实现实测可用）。
     * 拉到 `profile.attributes.country` 后会以服务端返回值为准并落库。
     */
    const val DEFAULT_COUNTRY = "CN"

    /** access_token 提前多少秒视为过期并触发刷新。 */
    const val TOKEN_REFRESH_SKEW_SECONDS = 300L

    /** 领奖之间的小随机间隔下限 / 上限（毫秒），避免连点。 */
    const val CLAIM_DELAY_MIN_MS = 900L
    const val CLAIM_DELAY_MAX_MS = 2200L
}
