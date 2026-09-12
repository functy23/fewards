package com.functy.fewards.data.repository

interface SettingsRepository {
    // 界面
    var themeMode: Int
    var miuixMonet: Boolean
    var keyColor: Int
    var colorStyle: String
    var colorSpec: String
    var enableBlur: Boolean
    var enableFloatingBottomBar: Boolean
    var enableFloatingBottomBarBlur: Boolean
    var navigationRailExpanded: Boolean
    var pageScale: Float
    var predictiveBackAnimation: Int // 0 None 1 MIUIX 2 AOSP 3 Scale 4 Classic

    // 通知与完成总览
    var taskNotification: Boolean
    var overviewAutoDismiss: Boolean
    var overviewHoldSeconds: Float

    // 米游社
    var mhyMasterEnabled: Boolean
    var mhyGameSign: Boolean
    var mhyBbsSign: Boolean
    var mhyRead: Boolean
    var mhyLike: Boolean
    var mhyCancelLike: Boolean
    var mhyShare: Boolean
    var mhyCaptchaPolicy: Int // 0 = 跳过并记录；1 = 打码接口
    var mhyCaptchaApiUrl: String

    // WorkBuddy
    var wbMasterEnabled: Boolean
}
