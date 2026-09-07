# Fewards

Android 自动签到工具：**米游社**（游戏签到 + 米游币任务）与 **WorkBuddy**（每日积分）。纯 Kotlin + Jetpack Compose 双主题（Miuix / Material Design 3），UI 骨架完整搬运自 [KernelSU manager](https://github.com/tiann/KernelSU)。

## 功能

| 任务 | 内容 |
|---|---|
| 米游社 | 扫码登录（stoken v2）/ Cookie 登录；游戏社区签到（原神/星铁/绝区零/崩3/未定/崩2 luna 接口）；米游币任务（社区签到/看帖/点赞/分享）；验证码策略（默认跳过并记录，可选打码接口） |
| WorkBuddy | 粘贴桌面端 accessToken，`checkin-status` + `daily-checkin`（HTTP 400 + code=10001 幂等视为成功） |

- 主页：Fewards 大标题 + 「已完成/未完成」状态大卡（对号/错号背景）+ 任务清单 + 复选框区 + 「开始执行」（点击震动）+ 日志框 + KSU 同款「支持项目/了解更多」底卡
- 账号页：米游社（扫码 ↔ Cookie 切换、二维码轮询、多账号列表）+ WorkBuddy（token 导入、多账号）
- 设置页：界面（MD3 ↔ Miuix 一键切换、浅色/深色/跟随系统、Monet、关键色、模糊、悬浮底栏等，全部照搬 KSU）；米游社板块（总开关 + 各子项开关 + 验证码策略）；WorkBuddy 板块（总开关）；定时时间 + 通知开关
- 定时：AlarmManager 精确闹钟（无权限时退化为非精确）+ 开机自启重注册；执行经 WorkManager 包裹，息屏可继续
- 所有 token / cookie 仅存本机 SharedPreferences（`secure_store`），日志永不打印凭据

## 构建

```bash
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

工具链：AGP 9.4.0 / Kotlin 2.4.10 / Compose BOM 2026.08.00 / miuix 0.9.3 / minSdk 31 / target+compile 37 / Java 21。

## 使用

1. 「账号」页：米游社**扫码登录**（生成二维码 → 米游社 App 扫码确认）或粘贴 Cookie（需含 stoken + mid）；WorkBuddy 粘贴桌面端 accessToken
2. 「设置」页：按需开关任务分项、验证码策略、每日定时时间
3. 「首页」：勾选要执行的任务 → **开始执行**（带震动反馈），日志框实时输出

## 架构

```
app/src/main/java/com/functy/fewards/
├── core/                  # AppLog（环形日志）
│   ├── mihoyo/            # DsSign（DS1/DS2/X4/app 签名）+ MihoyoConstants + MihoyoApi + MihoyoEngine
│   └── workbuddy/         # WorkBuddyEngine
├── data/repository/       # SettingsRepository / AccountRepository（含本地完成标记）
├── ui/                    # KSU 移植 UI（theme/navigation3/bottombar/liquid/effect 原样）
│   ├── screen/home/       # 主页（Miuix + Material 双实现）
│   ├── screen/account/    # 账号页（扫码二维码 + Cookie + token）
│   ├── screen/settings/   # 设置页
│   ├── screen/colorpalette/  # 主题页（KSU 原样）
│   └── screen/about/      # 关于页（KSU 原样）
├── viewmodel/             # MainActivityViewModel / SettingsViewModel / AccountViewModel / TaskViewModel / TaskRunner
└── work/                  # TaskScheduler（精确闹钟）/ TaskWorker / BootReceiver
```

## 接口微调提示

- 米游社 DS salt、act_id、接口地址均取自 MiyoQian 常量表，**可根据实际抓包微调**（`core/mihoyo/DsSign.kt`、`MihoyoConstants.kt`）
- stoken 验证/刷新 cookie_token 使用老接口 `getCookieAccountInfoBySToken`（老接口而非 ma-cn-session，规避 -5300 风控）
- WorkBuddy 接口 `copilot.tencent.com/billing/meter/*` 来自公开仓库 workbuddy-checkin，**可根据实际抓包微调**
- 打码接口约定：POST `{gt, challenge}` → 返回 `{validate}`（`MihoyoEngine.callCaptchaApi`）

## 免责声明

本项目仅供学习交流，请遵守各平台服务条款；账号风险自负。
