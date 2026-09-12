# Fewards — Agent 约定

给改这个仓库的 agent 用。人读 README。版本号、依赖坐标、SDK 以 `build.gradle.kts` / `gradle/libs.versions.toml` / `settings.gradle.kts` 为准，不要把某次构建的版本号抄进本文件当永久事实。

## 这是什么

Android 自动签到 App：米游社（游戏社区签到 + 米游币任务）+ WorkBuddy（每日积分）。Kotlin + Compose，**界面只有 Miuix**。仓库：`https://github.com/functy23/fewards`。包名 `com.functy.fewards`。

权威母本（只认这些，不要另找「更完整」的旧工程）：

| 层 | 认 | 不认 |
|---|---|---|
| UI / 导航 / 主题页手感 | [KernelSU manager](https://github.com/tiann/KernelSU)、导航与主题页可对照 [InstallerX-Revived](https://github.com/wxxsfxyzm/InstallerX-Revived) | Flutter、`rewards_runner`、autofewards 归档仓、本机 `fewards2` |
| 米游社协议 / DS / retcode / 任务流 | [MiyoQian](https://github.com/Womsxd/MiyoQian) | 自造 salt、自造 act_id、用 `getTokenBySToken` 换 cookie |
| WorkBuddy | 本仓 `WorkBuddyEngine` + 公开 HTTP（`copilot.tencent.com/billing/meter/*`） | `workbuddy-checkin` 空脚手架当实现、臆造头像接口 |

`rewards_runner` / Flutter 母本含难修细节与严重 BUG，**禁止参考、禁止对照、禁止移植**。旧文档里的「踩坑须知 / 需求细节」作废；只允许引用仍成立的工具链事实（镜像、坐标后缀），并以本文件与源码为准。

产品范围停在：默认三游戏签到 + BBS 米游币 + WorkBuddy 每日积分。不要做云游戏、商城兑换、定时闹钟、导航徽标、Markdown 更新弹窗。

## 动手前

1. 需求没到约 95% 把握就先问；相关问题一次问完。
2. 逻辑改动（DS / cookie / retcode / WorkBuddy / 配置导入 / 头像补全 / 执行入队）必须让 `./gradlew :app:testDebugUnitTest` 保持全绿。
3. 不要加 instrumented Compose UI 测试，除非用户明确要求。JVM 单测钉的是结果映射，不是像素。
4. 不要为了「跑通」去改测试里的 act_id / 域名 / 路径；先改实现或先确认接口真变了，再同步测试。
5. 功能改完（含修崩溃、加磁贴这类用户会装到手机上的改动）**直接出包**，不必等用户再说「出包」。只改文档、只改 AGENTS.md 不出包。
6. **软件版本（`fewardsVersionName`）只在推送时加。** 本地开发出包只动构建位（`fewardsVersionCode` 末两位）。推送后本地必须与远程同一 commit、同一对 versionName/versionCode，不要在本地多 bump 一截没推上去的软件版本。

## 出包（每次用户向 APK）

给用户装或对照的包必须是 **release + R8**（`optimization.enable`）。Debug 首帧 composition hitch 是预期，不要用进场 delay 糊弄。只 `compile` 不算交付。签名目前用 debug signingConfig（见 `app/build.gradle.kts`），这是有意的。

`fewardsVersionCode` 由 `fewardsVersionName` 推导，不是独立计数器：`M` `MM` `PP` 是版本号各位，末两位 `BB` 是构建号。

| versionName | versionCode |
|---|---|
| 2.0.1 | 2000100（发布构建；本地构建位 +1 → 2000101） |
| 2.0.2 | 2000200 |
| 2.1.0 | 2010000 |
| 1.3.20 | 1032000 |

小数位在 versionCode 里是两位定长，补零；`BB` 在发布包里为 `00`，本地出包时 +1（可递增到 99，进位到 `PP`）。

### 本地开发出包

1. 只把 `fewardsVersionCode` 的构建位 +1（保留当前 versionName 对应的 `M` `MM` `PP`）。**不要改 `fewardsVersionName`。**
2. `./gradlew :app:assembleRelease`
3. 复制到 Downloads，文件名带软件版本与构建号，**不要互相覆盖**：

```bash
cp app/build/outputs/apk/release/app-release.apk ~/Downloads/Fewards-<versionName>-<versionCode>-release.apk
```

### 推送

1. 把 `fewardsVersionName` patch +1（例如 1.3.19 → 1.3.20），`fewardsVersionCode` 同步改成新版本对应的发布号（构建位 `00`，如 1.3.20 → 1032000）。
2. 提交（版本向：`v<versionName>: …`），再 `git push`。
3. 用**新的** versionName 再打一份 release APK 拷到 Downloads，保证用户装到的包、git 标签语义、远程仓库三者一致。
4. 推完后确认 `git status` 干净、本地 HEAD == `origin/<branch>`。

### 发 Release

推送后每个推送版本都要有 GitHub Release（v1.1.0 起的历史里程碑已补齐，后续只发新版本）：

1. 注释 tag 打在版本提交上：`git tag -a v<versionName> -m "v<versionName>: …"`，再 `git push origin v<versionName>`。
2. `gh release create v<versionName> --verify-tag --title "v<versionName>" --notes-file <notes>`，**附上该版本的 release APK**（`~/Downloads` 里那份，重命名成 `Fewards-<versionName>-release.apk`）。
3. 只有最新版本是 Latest；补发历史版本时加 `--latest=false`，且历史 Release 只写说明、不挂 APK（旧版本不重建）。

## 构建环境

- JDK 21。不要降到 17。
- Maven 走 Aliyun 镜像链（`settings.gradle.kts`）。不要改成直连 Maven Central。
- miuix 坐标必须带 `-android` 后缀（`miuix-ui-android` 等）。
- **导航库互斥**：用 `top.yukonga.miuix.kmp:miuix-nav-android`（`miuixNav`）。`miuix-navigation3-ui-android` 的包名覆盖 `androidx.navigation3.ui`，与 `androidx.navigation3:navigation3-ui` **二选一**；两个都留会 Duplicate class。本仓只要 `miuix-nav` + `androidx.navigation3:navigation3-runtime`。
- Room / DataStore 在 gradle 里有坐标，**源码未使用**。设置与账号走 SharedPreferences。不要顺手接 Room。
- 已删除 commonmark / webkit 依赖，不要为「关于页 Markdown」加回来。

## 目录

```
app/src/main/java/com/functy/fewards/
  FewardsApplication.kt   # okhttp、今日状态水合、WorkManager Configuration.Provider
  core/mihoyo/            # DsSign、constants、HTTP、engine、头像/昵称补全
  core/workbuddy/         # check-in engine、JWT 标签
  data/repository/        # settings、accounts、config transfer
  ui/screen/              # home, account, settings, colorpalette, about（全 Miuix）
  ui/viewmodel/           # TaskRunner = 执行逻辑；入队不在这里
  ui/navigation/          # Route.Main / About / ColorPalette
  work/                   # TaskWorker 入队、通知、NotificationActionReceiver、QS Tile
scripts/workbuddy-token.sh
.github/workflows/test.yml
app/src/test/java/com/functy/fewards/
  core/mihoyo/            # DS、cookie、retcode、constants 契约、profile hydrator
  core/workbuddy/         # 签到判定、JWT 标签
  data/repository/        # 配置导入解析
```

`TaskRunner` 在 `ui/viewmodel` 是历史位置；**入队入口是 `work.TaskWorker.enqueue`**。不要再让 `TaskRunner` import `TaskWorker`（会环依赖）。Worker 可以调 `TaskRunner.execute`。

## 执行路径

首页「开始执行」与控制中心「签到」磁贴走同一条链：

1. `TaskNotifier.startRun`（立刻出通知）
2. `TaskWorker.enqueue(context, runWb, runMhy)`（WorkManager 一次性任务）
3. `TaskWorker.doWork` → `TaskRunner.execute`
4. wb / mhy 并行；`finally` 里 `running = false` 并 `RunTasksTileService.refresh`

磁贴：执行中 `Tile.STATE_ACTIVE`，结束 `INACTIVE`。执行中再点不取消。无已配置任务则保持关。Manifest 里 `WorkManagerInitializer` 被 `tools:node="remove"` 掉了，所以 `FewardsApplication` **必须**实现 `Configuration.Provider`（`workManagerConfiguration`）。缺这个，一点「开始执行」就会 `WorkManager is not initialized properly` 崩。

没有 AlarmManager / BootReceiver / 精确闹钟权限。不要加回「每日定时」。

控制中心磁贴类名 `com.functy.fewards.work.RunTasksTileService`，R8 已 keep。用户侧：控制中心 → 编辑 → 添加「签到」。

## 米游社

- 实际签到游戏冻在 `MihoyoConstants.SIGN_GAME_KEYS` = `genshin, starrail, zzz`。`GAMES` 全表仍留六套 act_id，**引擎不要按表全签**。改默认目标必须同步 `MihoyoConstantsContractTest.signTargetsStayOnDefaultThreeGamesAndTwoForums`。
- 社区签到分区冻在 `BBS_SIGN_FORUM_GIDS` = `5, 2`（大别野、原神）。看帖用第一个分区。
- 已签 / `can_get_points == 0` / 文案含「已签」「已完成」→ **成功**，不是失败。retcode：`0` 成功，`-5003` 已签，`-100` cookie 过期（刷 cookie_token 再试一次），`1034` 验证码（默认跳过并记日志；策略 1 才走打码 POST `{gt, challenge}` → `{validate}`）。
- 换 cookie **只用** `getCookieAccountInfoBySToken`。不要用 `ma-cn-session getTokenBySToken`（非官方设备 -5300）。
- Web cookie 组装走 `MihoyoApi.buildFetchedWebCookie`。`cookie_token=$cookie_token` 这种字面量插值是已修过的 bug，单测钉了。不要再写成 `${'$'}cookie_token`。
- 头像/昵称：凭据可用后立刻 `MihoyoProfileHydrator`（扫码、Cookie 导入、配置导入、账号页 refresh）。不要拖到点「开始执行」。拉取中账号列表用加载圈占位。`TaskRunner` 里仍保留缺 cookie_token / 缺头像时的补全，当作兜底。

## WorkBuddy

- `POST https://copilot.tencent.com/billing/meter/checkin-status` 与 `/daily-checkin`，Bearer token，body `{}`。
- 最终成败看 daily-checkin：HTTP 2xx + `code==0` 成功；HTTP 400 + `code==10001` **今日已签，视为成功**；401/403 token 过期。`data.today_checked_in` 不可靠，只作短路。
- 列表名从 JWT 解：`nickname`（trim 控制字符）优先，否则 `preferred_username`。实现：`WorkBuddyLabel.decodeProfile`。
- **没有头像接口。** JWT 若带 `picture` / `avatar_url` 等 http 地址就用；否则账号列表用昵称首字母色块。不要接腾讯用户资料 API。
- macOS 取 token：`scripts/workbuddy-token.sh`（读 CodeBuddyExtension 本地 auth）。不要把 token 写进仓库或日志。
- **扫码授权**（`WorkBuddyLogin`，移植自 workbuddy-manager `server/services/tencent.py`）：
  1. `POST /v2/plugin/auth/state?platform=CLI` → `data{state, authUrl}`；`authUrl` 即二维码内容。
  2. `GET /v2/plugin/auth/token?state=…` → **未扫码返回非 0 码（实测 11217 "login ing..."）**，别当错误；`code==0` 时 `data` 是**驼峰** `accessToken/refreshToken/expiresIn/domain`。
  3. `GET /v2/plugin/login/account?state=…`，带 `Authorization: Bearer {accessToken}` → `data{uid, nickname, enterpriseId}`。
  - 统一信封 `{code,msg,data}`：判定只看 `code`。**缺 `code` 字段必须算失败**（`hasOkCode`），不能因为「没有 code」就当成功。
  - 本地会话 TTL 5 分钟（`STATE_TTL_MS`），轮询 2 秒一次。轮询期间没有单独的「已扫码」信号。
  - 扫码成功后**只纳管，不自动签到**；签到仍由首页「开始执行」统一触发。

## 账号与配置

- 凭据只存在本机 SharedPreferences（`AccountRepository` / `SettingsRepositoryImpl`）。日志禁止打印 token / cookie / stoken。
- 配置 JSON：`_format=fewards-config`，`ConfigTransferParser` + `ConfigTransferRepository`。兼容裸 cookie / 裸 JWT。导入后米游社会 hydrate 头像。
- 输入框用 miuix `basic.TextField`（`MultilineInputField` 包装）。不要用已删除的 `EditText.kt`。
- **WorkBuddy 多账号**：`WorkBuddyAccount` 带 `uid/enterpriseId/refreshToken/expiresAt`（老数据没有这些字段，读出来是空/0）。
  - `AccountRepository.addWorkBuddyAccount` 去重顺序：同 `uid` → 老账号（uid 空）同 `label` → 同 `id`；命中原地替换，不新增。**不要退回「只按 id 去重」**，否则同一账号重复扫码会多出一条。
  - `WorkBuddyLabel.decodeProfile` 的 `uid` 取 JWT `sub`（实测与 `/login/account` 的 uid 一致），`expiresAt` 取 `exp`，仅用于导入去重；权威 uid 以扫码接口为准。

## UI 约定

- **只有 Miuix。** 不要加回 MD3 / 双 UI。
- 设置子项跟父开关 **展开/收起**（Monet 模式），不要灰掉。
- 停留时间选择用 **OverlaySpinnerPreference**（列表 + 确定），不要一排 TextButton。
- 主题页：WindowSpinnerPreference；不要把 `Scaffold.popupHost` 置空。
- 主题颜色模式用轮廓 Tab（不是下拉）。
- 主页图标 28.dp；复选框行文字 `weight(1f)`，Checkbox 靠右。
- 图标/头像圆角：miuix `squircleClip(cornerRadius = size * 0.30f)`（`SquircleIcon` / 账号头像共用）。禁止自写 `G2SquircleShape`：`mid = √2−1` 会把 45° 点放到 0.414r，四角内缩约 2×。也不要用普通 `RoundedCornerShape` 充 squircle。
- 图标 png 在 `drawable-nodpi/`（`miyoushe`、`workbuddy`）。README 宣传图在 `docs/screenshots/`（home / account / settings / theme），换界面后同步更新。
- 账号头像网络图：`AccountMiuix.UrlImage`（OkHttp + G2 裁剪）。加载中 `InfiniteProgressIndicator`，失败回落到字母头像。
- **添加账号弹窗只有一份**：`ui/component/miuix/AddAccountDialog`（米游社与 WorkBuddy 共用）。不要再各写一份二维码弹窗。
  - 账号页只列**已登录账号**，米游社 / WorkBuddy 用灰色小标题（`SectionHeader`）分组；没有账号时显示 `account_empty` 提示。
  - 右上角「+」用 `WindowListPopup` 弹「添加米游社账号 / 添加 WorkBuddy 账号」；两项都打开同一个 `AddAccountDialog`。菜单行是本文件里的 `AddMenuRow`，**不要换回 miuix `DropdownImpl`**：它恒定给尾部选中勾留 `CheckIconStartPadding + CheckIconSize`（≈32dp），这里没有选中态，那段槽位就是文字后面的一截空白。`WindowListPopup` 要传 `horizontalMargin = 12.dp`，否则菜单右边缘贴着屏幕右边。
  - 「+」是**带圈按钮**（`IconButton` + `backgroundColor = surfaceContainerHigh`；minWidth/minHeight 与默认 cornerRadius 都是 40dp，给底色即正圆）。它和大标题同一条中线：`TopAppBar` 把 actions 垂直居中在 52dp 的收起高度里，大标题却排在 52dp 之下，所以用 `Modifier.offset` 下移「半个收起高度 + 半个 title1 行高」，再按 `scrollBehavior.state.collapsedFraction` 收回原位。
  - 弹窗内用 `TabRowWithContour` 切「扫码登录 / 凭据登录」：扫码页生成二维码，凭据页是输入框 + 导入。WorkBuddy 的凭据 Tab 是 Token 粘贴。
  - 开合由调用方的 `show` 状态驱动，只有 `QrState.Confirmed` 才自动关；过期/失败保持打开以露出「重新获取」。凭据导入成功（`onImportCredential` 返回 true）也自动关。
  - 弹窗的**挂载**由调用方的 `mounted` 标志控制（不是 `qrState`）：切到凭据 Tab 会立刻把 `qrState` 归零，只按 `qrState` 判断会让关闭动画演一半就消失。关闭走 `WindowDialog` 下滑淡出动画，`onDismissFinished` 里才置 `mounted=false` 并取消轮询。
  - 二维码申请以「弹窗开合 + Tab」为 key 的 `LaunchedEffect` 驱动，弹出动画结束（`delay(450)`）后才申请；切走再切回扫码 Tab 会重新申请一张新码，过期/失败后重开能自愈。
- 实时任务通知 ongoing，完成后再提升；自动消失跟 `overviewAutoDismiss` / `overviewHoldSeconds`。

## 导航与预测返回

- 路由只有 `Route.Main` / `About` / `ColorPalette`。主页三页是 pager，不是三条 Route。不要加回 `Route.Home/Account/Settings`。
- `NavDisplayEffects.dimAmount = 0.5`（库在页面之间的 scrim）。入场 route 第 0 帧必须是不透明 surface。
- 圆角裁剪：`if (roundAllCorners && navCornerRadius <= 0.dp) 32.dp else navCornerRadius`。不要无条件 32.dp（MIUIX/None 在 Leading 模式、系统没报半径时会闪黑）。
- 系统预测返回：manifest `android:enableOnBackInvokedCallback="true"` 静态声明。没有「启用预测返回」设置项，不要加回来。
- 返回**动画**是纯 Compose，读 `MainActivityUiState.predictiveBackAnimation`。要立即生效，`MainActivityViewModel.observedKeys` 必须包含 `predictive_back_animation`。选项：无 / MIUIX / AOSP / 缩放 / 经典。

## 不要加回去的东西

这些是评过、确认过、已经删掉的。加回等于制造死链或崩溃：

- `enablePredictiveBack` 设置链
- 导航徽标（`NavigationBadgeState` / `LocalEnableNavigationBadge` / 底栏 badge）
- 定时闹钟（`TaskScheduler`、`BootReceiver`、`SCHEDULE_EXACT_ALARM`、`USE_EXACT_ALARM`、`RECEIVE_BOOT_COMPLETED`）
- 可配置签到游戏/分区 prefs（改常量，不改设置项）
- `Dialog.kt` / `DialogMiuix.kt` / `MarkdownContent.kt` / `GithubMarkdown.kt` / Monet CSS WebView
- `EditText.kt`、`WarningCard.kt`、`ScrollToTop.kt`、`ThemeExt.kt`、`ui/util/Colors.kt`、`MonetColorsProvider`
- 未使用的 strings（预测返回开关、定时、导航徽标、签到游戏/分区文案）

## 测试

`./gradlew :app:testDebugUnitTest`，CI：`.github/workflows/test.yml`。

改了下面这些必须有/更新 JVM 测试：DS 签名、cookie 解析与 `buildFetchedWebCookie`、米游社已签判定、WorkBuddy `interpretDailyCheckin`、WorkBuddy 扫码协议（`WorkBuddyLoginTest`：信封 code 优先、11217 视为等待、驼峰字段、缺 uid 未就绪、缺 code 不算成功）、`ConfigTransferParser`、`SIGN_GAME_KEYS` / act_id / 域名。头像 hydrator 的「要不要拉」用 `MihoyoProfileHydratorTest` 钉，JWT 的 uid/exp 用 `WorkBuddyLabelTest` 钉。

## 提交

版本向提交跟现有风格：`v<versionName>: 一句话说明用户能感知的变化`。测试向：`test: …`。文档向：`docs: …`。不要把 `scripts/stutter_ab.py`（未跟踪的对照脚本）塞进提交。不要改 git config。不要在没要求时 push。
