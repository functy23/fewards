# Fewards

Android 自动签到：**米游社**（游戏签到 + 米游币任务）和 **WorkBuddy**（每日积分）。Kotlin + Jetpack Compose，界面为 **Miuix**。

参考：

- UI：[KernelSU manager](https://github.com/tiann/KernelSU)、[InstallerX Revived](https://github.com/wxxsfxyzm/InstallerX-Revived)（导航 / 主题页）
- 米游社：[MiyoQian](https://github.com/Womsxd/MiyoQian)
- WorkBuddy：公开 HTTP 接口（`copilot.tencent.com/billing/meter`）

这是当前维护的版本。早期同功能仓库已归档，请只用本仓库：

- [functy23/autofewards](https://github.com/functy23/autofewards)（归档）
- [functy23/autorewards-runner](https://github.com/functy23/autorewards-runner)（归档，Flutter 母本）

## 功能

| 任务 | 内容 |
|---|---|
| 米游社 | 扫码登录（stoken v2）/ Cookie；游戏社区签到（默认原神 / 星铁 / 绝区零；常量表仍保留崩 3 / 未定 / 崩 2 的 act_id）；米游币（社区签到 / 看帖 / 点赞 / 分享）；验证码策略（默认跳过，可选打码接口） |
| WorkBuddy | 粘贴桌面端 accessToken；`checkin-status` + `daily-checkin`（`code=10001` 视为已签成功） |

- 主页：状态卡（未完成 / 执行中 / 已完成）+ 任务清单 + 开始执行 + 日志
- 控制中心：把「签到」快捷开关加到控制中心；点击即开始执行（与首页默认勾选相同），执行中保持开启、结束后关闭
- 账号：米游社扫码或 Cookie、多账号；WorkBuddy token 导入
- 设置：主题（Monet / 关键色 / 模糊 / 悬浮底栏 / 返回动画）、米游社与 WorkBuddy 总开关（开则展开子项）、任务通知与完成总览
- 执行走 WorkManager（进程被杀后仍可继续）
- 实时活动进度通知（完成总览可自动消失或点确认）
- 凭据只存本机 SharedPreferences；日志不打印 token / cookie

## 构建

给用户装的包必须是 **release + R8**：

```bash
./gradlew :app:assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

工具链：AGP 9.4.0 / Kotlin 2.4.10 / Compose BOM 2026.08.00 / miuix 0.9.3 / miuix-nav 0.9.4-rc01 / minSdk 31 / target 37 / Java 21。

macOS 一键复制 WorkBuddy Access Token：

```bash
chmod +x scripts/workbuddy-token.sh
./scripts/workbuddy-token.sh
```

## 使用

1. 「账号」：米游社扫码或粘贴含 stoken + mid 的 Cookie；WorkBuddy 粘贴桌面端 accessToken
2. 「设置」：开关任务分项、验证码策略、通知与完成总览
3. 「首页」：勾选任务 → 开始执行

## 架构

```
app/src/main/java/com/functy/fewards/
├── core/mihoyo/          # DS 签名、常量、HTTP、引擎
├── core/workbuddy/       # 签到引擎
├── data/repository/      # 设置、账号、配置导入导出
├── ui/screen/            # 主页 / 账号 / 设置 / 主题 / 关于（Miuix）
├── ui/viewmodel/
└── work/                 # WorkManager worker、通知
```

Agent 约定见 [AGENTS.md](AGENTS.md)。

## 接口

- 米游社 DS salt、act_id、地址以 MiyoQian 为准，抓包后可改 `DsSign.kt` / `MihoyoConstants.kt`
- stoken 换 cookie 走 `getCookieAccountInfoBySToken`
- WorkBuddy：`copilot.tencent.com/billing/meter/*`
- 打码接口：POST `{gt, challenge}` → `{validate}`

## 免责声明

仅供学习交流，遵守各平台条款；账号风险自负。
