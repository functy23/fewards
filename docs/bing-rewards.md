# 微软积分（Microsoft Rewards / Bing）协议与本仓实现

这份文档记录 `core/bing` 用到的**协议事实**、这些事实的来源，以及为什么这里没有一个字节
是从别的仓库复制过来的。改 `core/bing` 之前先读它，不要在实现里另找一套「更完整」的接口。

## 为什么不做自动搜索刷分

只做**领取**：每日活动卡片、更多促销、阅读赚分、Bing App 每日连签，外加积分只读展示。

不做的理由是官方的，不是猜的。微软支持页 "Limiting your searches in Microsoft Rewards"
（<https://support.microsoft.com/en-us/accounts-billing/rewards/limiting-your-searches-in-microsoft-rewards>）原文：

> Refrain from using tricks to gain points quickly such as spamming letters or numbers into Bing,
> clicking through articles quickly or using bookmarks to open articles in a rapid manner…
> **Do not use programs, bots, or macros to help with searching.**
>
> Repeated violations may result in further actions against your account including
> **suspension of your account and invalidating all earned points.**

另外两个纯工程上的理由：搜索分**只能**靠真实搜索拿到（`claim` 领不到），
而且「补满 60 次搜索 × 4–11 秒」≈ 6–10 分钟，会撞 WorkManager 常规 Worker 的执行上限，
要长跑就得引入前台服务 + `FOREGROUND_SERVICE_DATA_SYNC`，与「一次点按、几秒结束」的现有形态冲突。

## 协议事实

### 鉴权：OAuth 授权码 + refresh_token（公开客户端）

| 项 | 值 |
|---|---|
| 授权页 | `https://login.live.com/oauth20_authorize.srf` |
| `client_id` | `0000000040170455`（公开客户端，**无 client_secret**） |
| `scope` | `service::prod.rewardsplatform.microsoft.com::MBI_SSL` |
| `redirect_uri` | `https://login.live.com/oauth20_desktop.srf` |
| `response_type` | `code` |
| 换 token | `https://login.live.com/oauth20_token.srf`（`grant_type=authorization_code` / `REFRESH_TOKEN`） |

本仓在应用内 WebView（`BingAuthWebView`）里完成登录：拦到 `oauth20_desktop.srf` 回跳就取 `code`，
**不加载那个桌面页面**；回跳必须校验一次性 `state`。

换 token 的响应体在不同情况下可能是 JSON，也可能是 `application/x-www-form-urlencoded`；
`BingOAuth.parseTokenResponse` 两种格式都认，这条分支有单测钉住。
刷新时微软**可能不换发**新的 `refresh_token`——此时要沿用旧的，不能把它清空。

### 榜单与领取：dapi

| 用途 | 请求 |
|---|---|
| 余额 + 任务清单 | `GET https://prod.rewardsplatform.microsoft.com/dapi/me?channel=SAAndroid&options=511` |
| 领取一张卡片 | `POST https://prod.rewardsplatform.microsoft.com/dapi/me/activities`，body `{amount:1, country, id:<64位hex>, type:101, attributes:{offerid}}` |
| Bing App 每日连签 | 同上端点，body `{amount:1, country, id, type:103, channel:"SAAndroid", attributes:{}, risk_context:{}}`，另加 `X-Rewards-AppId: SAAndroid`、`X-Rewards-PartnerId: startapp` |

请求头：`Authorization: Bearer <access_token>`、`X-Rewards-Country`、`X-Rewards-Language`、
`X-Rewards-AppId: SAAndroid/31.4.2110003555`、`X-Rewards-IsMobile: true`。

响应信封是 `{"response": {...}}`：`/me` 给 `balance`、`profile.attributes.country`、`promotions[]`；
`/me/activities` 给 `response.activity.p`（本次到账分）与 `response.isDuplicate`。

`promotions[]` 的真实字段都在 `attributes` 里，且大小写不统一：
`offerid` / `offerId`、`progress` / `activityprogress`、`max` / `activitymax`、
`complete` / `hidden` 为字符串形式的布尔、分类标签在 `Classification.Tag`。解析时两种都认。

### 哪些卡片能一键领

`BingPromotions.isClaimable`：

- `complete` → 不用领；
- `type == "checkin"` → **即使 `hidden` 也可领**（每日签到卡渲染在独立挂件里，所以被标 hidden）；
- 其余 `hidden` → 不领（信息类 / 抽奖类占位）；
- 只有 `urlreward`（每日活动 / 打卡）与 `msnreadearn`（阅读赚分）能一次领掉；
- `search` 及其它类型一律不领。

### 领取结果判定

`BingOutcome.interpretClaim`：

| 响应 | 结论 |
|---|---|
| HTTP 401 / 403 | token 失效 → 刷新后重试一次 |
| `response.isDuplicate == true` | 今天已领过，**算成功**（幂等） |
| `response.activity` 存在 | 到账，分数取 `activity.p` |
| `error` / `success == false` / `response.success == false` | 被拒绝 |
| HTTP 2xx 但既无 `activity` 也无 `isDuplicate` | **算失败**——「有 response」不等于「到账」 |

## 事实来源（只作事实来源，不复制代码）

这三份实现用来交叉核对端点、字段与判定规则。**许可证决定了它们只能这样用**：
协议事实（URL、HTTP 方法、payload 字段名、`client_id`、取值）不受版权保护，可以照着重写；
但代码本身不行，所以 `core/bing` 全部是本仓自行实现的。

| 仓库 | 许可证 | 用它核对了什么 |
|---|---|---|
| `QingJ01/Get-Microsoft-Rewards` | MIT | OAuth 授权 / 刷新 URL、`dapi/me`、`type:103` 连签、`X-Rewards-*` 头。**这是与国区最贴近的一份**（其实现直接使用 `X-Rewards-Country: cn`） |
| `SkyBlue997/MicrosoftRewardsPilot` | **无 LICENSE** | `dapi` 请求 / 响应形状、`options=511`、`isClaimable` 的类型白名单与「checkin 虽 hidden 可领」这条例外、`type:103` 连签的额外头 |
| `993671009/microsoft-rewards-refactored` | **无 LICENSE** | `options=613` / `105` 变体、`type:101`/`103` payload、`isDuplicate`、以及「`error`/`success:false` 也要判失败」这条 |

两个无 LICENSE 的仓库在本文档里**只作事实来源**，没有引入任何代码、注释或文本。

## 与 legacy 的关系

`rewards.bing.com/api/getuserinfo`、`/api/reportactivity`、
`www.bing.com/rewardsapp/ncheader|reportActivity`、`www.bing.com/msrewards/api/v1/ReportActivity`
是旧一代（服务端渲染页面 + IG 埋点）的端点。2026 年 `rewards.bing.com` 已重写为 Next.js SPA，
旧 DOM 抓取失效；社区实现（含 2026-10 仍在更新的脚本）虽然还在用 legacy 的搜索三连，
但**领取类操作已统一转向 dapi**。

本仓只实现 dapi。**不要把 legacy 端点当主线加回来**，除非 dapi 出现明确的、可复现的失效。

## 已知未核实项

- `client_id=0000000040170455` 在 2026 年的有效性：由多个 2026-09/10 仍在推送的实现间接支持，
  **未做真机端到端验证**。第一次真机登录请重点看这一步。
- 是否存在 device code 流程：已查阅的实现里**都没有**，一律用 `oauth20_desktop.srf` 回跳。
- `refresh_token` 的实际有效期 / 掉线频率：未核实。
- `profile.attributes` 里除 `country` 以外的显示名字段（`BingOutcome.DISPLAY_NAME_KEYS`）是
  **防御性候选**，不同地区 / 账号类型下结构不一致；取不到时账号列表退回不可逆指纹标签。
- 各地区 Rewards 可用性：官方 regions 页（<https://support.microsoft.com/en-us/accounts-billing/rewards/microsoft-rewards-regions>）
  的 Asia 段包含 `China`，但**具体账号的目录与配额**未逐一核实。
