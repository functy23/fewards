# Fewards — Agent 约定

给改这个仓库的 agent 用。人读 README。版本号、依赖坐标、SDK 以 `build.gradle.kts` / `gradle/libs.versions.toml` / `settings.gradle.kts` 为准，不要把某次构建的版本号抄进本文件当永久事实。

## 文档约定（双语 + 徽章）

README 为**英文主文档**（`README.md`）+ **中文全量翻译**（`doc/README_zh-CN.md`），
两份内容一一对应，**改一边必须同步另一边**。两份文件顶部是同一组 shields.io 徽章
（语言/平台/CI/License/Release/Downloads/Stars/Repo Size/Contributors 按仓库实际能力裁剪，
没有的能力不放，避免死链），徽章下面一行语言切换：
`README.md` 用 `**English** | [简体中文](doc/README_zh-CN.md)`，
中文版用 `[English](../README.md) | **简体中文**`。增删徽章时两份一起改。

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

### miuix 版本

当前 miuix（含 `miuix-glass`）取自 [compose-miuix-ui/miuix PR #423](https://github.com/compose-miuix-ui/miuix/pull/423) 分支 `lingqiqi5211:feat/miuix-glass` 的**本机构建产物**，版本号 `0.9.4`。

- **Maven Central 上没有 0.9.4**：Central 最新是 `0.9.4-rc01`（2026-08-13），`miuix-glass` 从未发布；PR #423 未合并，该 fork 的 Actions 是 0 次运行，JitPack 上本仓历史构建全是 Error。所以 `settings.gradle.kts` 里加了 `mavenLocal()`，坐标由 `~/.m2` 提供。
- 换机器 / 清缓存后必须先重建一次，否则依赖解析失败：

```bash
git clone --branch feat/miuix-glass https://github.com/lingqiqi5211/miuix.git ~/Desktop/miuix-pr423
cd ~/Desktop/miuix-pr423
printf 'sdk.dir=%s\n' "$HOME/Library/Android/sdk" > local.properties
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home ./gradlew -Prelease \
  :miuix-core:publishAndroidPublicationToMavenLocal :miuix-ui:publishAndroidPublicationToMavenLocal \
  :miuix-preference:publishAndroidPublicationToMavenLocal :miuix-shader:publishAndroidPublicationToMavenLocal \
  :miuix-blur:publishAndroidPublicationToMavenLocal :miuix-squircle:publishAndroidPublicationToMavenLocal \
  :miuix-icons:publishAndroidPublicationToMavenLocal :miuix-nav:publishAndroidPublicationToMavenLocal \
  :miuix-glass:publishAndroidPublicationToMavenLocal
```

  `-Prelease` 不能省：默认会带上 `-<githash>-SNAPSHOT` 后缀，版本号就不是 `0.9.4`。只发 Android 制品，别跑全量 `publishToMavenLocal`（会连带编译 iOS/macOS/Wasm 原生目标）。
- `miuix-glass` 的 aar 声明 `minSdk 33`（AGSL），应用是 31：manifest 的 `tools:overrideLibrary` 里必须同时列 `top.yukonga.miuix.kmp.blur` 与 `top.yukonga.miuix.kmp.glass`。运行期由 `isRuntimeShaderSupported()` 兜底（底栏会因此换成 `FloatingNavigationBar`，见「玻璃材质」一节）。
- 上游合并发版后应改回 Central 坐标并删掉 `mavenLocal()`。

其余环境约束：

- **导航库互斥**：用 `top.yukonga.miuix.kmp:miuix-nav-android`。`miuix-navigation3-ui-android` 的包名覆盖 `androidx.navigation3.ui`，与 `androidx.navigation3:navigation3-ui` **二选一**；两个都留会 Duplicate class。本仓只要 `miuix-nav` + `androidx.navigation3:navigation3-runtime`。
- Room / DataStore 在 gradle 里有坐标，**源码未使用**。设置与账号走 SharedPreferences。不要顺手接 Room。
- 已删除 commonmark / webkit 依赖，不要为「关于页 Markdown」加回来。
- `local.properties`（`sdk.dir`）**未跟踪**，新克隆的树没有它，Gradle 会直接报 SDK location not found。

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
- 主题颜色模式用轮廓 Tab（`TabRowWithContour`），不是下拉。**不要**换成 `GlassSegmentedTabRow`，理由见「玻璃材质」一节。
- 主页图标 28.dp；复选框行文字 `weight(1f)`，Checkbox 靠右。
- 图标/头像圆角：miuix `squircleClip(cornerRadius = size * 0.30f)`（`SquircleIcon` / 账号头像共用）。禁止自写 `G2SquircleShape`：`mid = √2−1` 会把 45° 点放到 0.414r，四角内缩约 2×。也不要用普通 `RoundedCornerShape` 充 squircle。
- 图标 png 在 `drawable-nodpi/`（`miyoushe`、`workbuddy`）。README 宣传图在 `docs/screenshots/`，每个页面**浅色深色各一张**：`<page>-light.png` / `<page>-dark.png`（page = home / account / settings / theme），README 里两行分别展示。换界面后两套都要同步更新。
- 账号头像网络图：`AccountMiuix.UrlImage`（OkHttp + G2 裁剪）。加载中 `InfiniteProgressIndicator`，失败回落到字母头像。
- **添加账号弹窗只有一份**：`ui/component/miuix/AddAccountDialog`（米游社与 WorkBuddy 共用）。不要再各写一份二维码弹窗。
  - 账号页只列**已登录账号**，米游社 / WorkBuddy 用灰色小标题（`SectionHeader`）分组；没有账号时显示 `account_empty` 提示。
  - 右上角「+」用 **`GlassTransformPopup`** 弹「添加米游社账号 / 添加 WorkBuddy 账号」；两项都打开同一个 `AddAccountDialog`。不要退回 `WindowListPopup` + 自写 `AddMenuRow`，也别用 miuix `DropdownImpl`（它恒定给尾部选中勾留 ≈32dp，这里没有选中态）。
  - 「+」是 `GlassIconButton`（miuix-glass 的玻璃圆钮），图标用 **`MiuixIcons.AddCircle`**，在 `GlassTopAppBar` 的 `actions` 槽里。**不要退回** `IconButton` + `backgroundColor`（`IconButton` 只留给行内操作，见下）。
  - `GlassTopAppBar` 同样把 actions 垂直居中在「收起高度」（52dp）里，而大标题排在 52dp 之下，所以栏内按钮**默认停在大标题上方**。要和大标题对齐就得补一段 `Modifier.offset`：按 `MiuixTheme.textStyles.title1` 量一次标题行高，下移 `CollapsedHeight / 2 + 行高 / 2`，再按 `scrollBehavior.state.collapsedFraction` 收回原位（offset 的 lambda 在布局阶段读，不订阅重组）。这段补正**不能**省。
  - 这段 `Modifier.offset` 必须套在**外层 `Box`** 上，**不能**写进 `GlassIconButton` 自己的 modifier 链（哪怕写在 `glassPopupAnchor` 前面）。`glassPopupAnchor` 用 `boundsInRoot()` 上报锚点，而它取的是该节点**最外层布局修饰符**的位置——同一条链里的 `offset` 只挪链内内容、不改这个位置，锚点于是少算整段位移。症状：点「+」按钮先消失、玻璃胶囊在真实位置上方约一个位移处冒出来，关菜单时又在上面停一下才跳回。
  - 弹窗内用 **`GlassSegmentedTabRow`** 切「扫码登录 / 凭据登录」：扫码页生成二维码，凭据页是输入框 + 导入。WorkBuddy 的凭据 Tab 是 Token 粘贴。
  - 开合由调用方的 `show` 状态驱动，只有 `QrState.Confirmed` 才自动关；过期/失败保持打开以露出「重新获取」。凭据导入成功（`onImportCredential` 返回 true）也自动关。
  - 弹窗的**挂载**由调用方的 `mounted` 标志控制（不是 `qrState`）：切到凭据 Tab 会立刻把 `qrState` 归零，只按 `qrState` 判断会让关闭动画演一半就消失。关闭动画是 `GlassDialog` 的缩放淡出；它**没有** `onDismissFinished`，所以 `mounted=false` + 取消轮询放在 `LaunchedEffect(show*)` 里 `delay(200)` 之后。
  - 二维码申请以「弹窗开合 + Tab」为 key 的 `LaunchedEffect` 驱动，弹出动画结束（`delay(450)`）后才申请；切走再切回扫码 Tab 会重新申请一张新码，过期/失败后重开能自愈。
- 实时任务通知 ongoing，完成后再提升；自动消失跟 `overviewAutoDismiss` / `overviewHoldSeconds`。

### 玻璃材质（miuix-glass）

- 首页 / 账号 / 设置三页顶栏都是 `GlassTopAppBar`，底栏悬浮态是 `GlassNavigationBar`。**材质、滚动遮罩、按压反馈、指示器跟随全部交给库**，页面不要再套 `BlurredBar` / `textureBlur`，也不要再自己算 `barColor`。
- **折射的真实范围**（读 `internal/GlassShader.kt` 得出，别按「整块玻璃弯折内容」去理解或去调）：`shadeMaterial` 里 `isFlat = shaped >= 1.0`，只有**边缘带**内才跑 `refract()` 与 `reflect()`；带外直接早退，只剩模糊 + `GlassMaterial` 的色彩层 + tint。默认 `GlassDefaults.Style`（`CommonMediumRegularLowLight`）的 `GlassEdge.width = 60` 是**源像素**（`SourceDensity = 3`），即 20dp；折射偏移 `thickness * 2`（`thickness = 80` → 约 53dp @3x），比带本身宽，所以看到的是边缘一圈把内容「吸」进来一点，不是整块位移。
- **`shading` 的取值是上游刻意定的，不要改**：库内**所有**组件调用点（`GlassNavigationBar` / `GlassTopAppBar` 的按钮与 tab / `GlassPopupSurface` / `GlassTabRow` / `GlassSurface`）都传 `shading = false`，只有调用方直接用 `glassPanel`/`glass`（默认 `true`，如库自己的 `GlassDialog`）才有边缘明暗。理由写在 KDoc 里：源系统把表面**要么**声明成 material（一条栏/一个菜单：模糊 + 色彩层 + 描边 + 阴影），**要么**声明成 glass（一个控件：折射 + 明暗），不混用。所以「底栏/顶栏/按钮没有折射」是**上游的设计**，不是我们接错——真机上要判断有没有玻璃，看的是模糊 + 色彩层 + 那圈 bloom 描边 + 边缘带里轻微的内容位移。
- 顶栏用带 `isContentScrolled` 的重载，传 `listState.canScrollBackward`；不要用靠 `scrollBehavior.state.contentOffset` 推的那版——列表回顶后大标题可能仍处于收起态，材质会留着不走。
- `backdrop` 来自 `rememberBlurBackdrop()`（无参数、非空，见上一条），**录制的子树必须挂 `Modifier.layerBackdrop(backdrop)`**，否则玻璃采样不到内容。玻璃表面本身要放在该子树**外面**，否则自引用。
- 已删除自写的 `ui/component/FloatingBottomBar.kt`、`ui/component/liquid/`（Kyant0/AndroidLiquidGlass 移植）、`ui/component/miuix/animation/`、`ui/component/miuix/modifier/`。**不要加回来**，这些是 PR #423 落地前的临时实现。
- 主题页「液态玻璃」开关只切**材质**（`LocalFloatingBottomBarGlass`）：开 = `GlassNavigationBar`（miuix-glass 材质），关 = miuix 自带的 `FloatingNavigationBar`（悬浮胶囊，无玻璃材质）。悬浮底栏本身仍由「悬浮底栏」开关控制。
- **「模糊效果」开关已删除**（`enableBlur` / `LocalEnableBlur` / `enable_blur` prefs / 两条 strings 全没了）。理由：模糊是 miuix-glass 的硬前提，关掉等于玻璃全失效。`rememberBlurBackdrop()` **不再返回可空类型**——应用 minSdk 31，而 `isRenderEffectSupported()` 就是 `SDK_INT >= 31`，判断恒真。所以别再加回 `!= null` 判断（编译器会报 `Condition is always 'true'`）。真正的分档只有 AGSL（API 33+），由 miuix-glass 内部各自再判。
- 已玻璃化的清单（照做即可，不要再自造）：顶栏三页 = `GlassTopAppBar`；底栏 = `GlassNavigationBar`；账号页「+」= `GlassIconButton`（actions 槽）+ `GlassTransformPopup`（Scaffold 同级）；**全部三个弹窗**（AddAccountDialog / ScaleDialog / OverviewHoldCustomDialog）= `GlassDialog`；**AddAccountDialog 内的登录方式切换框** = `GlassSegmentedTabRow`。
- **主题页那排「跟随系统 / 浅色 / 深色」是刻意的例外，保持非玻璃的 `TabRowWithContour`。** 它长在 `LazyColumn` 里，也就是 `layerBackdrop` 的录制子树内，却要采样同一个 backdrop —— 玻璃表面在录制层内会自引用。真机实测：进主题页必崩，`Fatal signal 11 (SIGSEGV)` in RenderThread，`Cause: stack pointer is close to top of stack; likely stack overflow`，栈里全是 `RenderNode::prepareTreeImpl` 在无限递归。库自己的做法是把玻璃 Tab 放进顶栏 `bottomContent`（见 example `GlassPage`），**不在滚动内容里**。不要「顺手统一」。
- `GlassNavigationBar` 的面板靠 RuntimeShader（AGSL，API 33+）；31/32 上 `drawBackdrop` 会被 `isRuntimeShaderSupported()` 整条关掉，只剩描边和阴影，在深色页面上读起来是「内容被挖了个洞」。所以底栏用 `isRuntimeShaderSupported()` 分流，低版本退 `FloatingNavigationBar`。`GlassTopAppBar` 不用分流：它的 `bandBrush` 遮罩是纯 Compose 绘制，材质失效时会露出纯色 `fill`。
- `GlassIconButton` 的按压反馈由库给，不要再自己包 `IconButton`。
- 账号页图标一律走 **miuix 图标集**（`MiuixIcons.*` + `Modifier.size(24.dp)`）：行内删除用 `MiuixIcons.Delete`（配 miuix `IconButton`），顶栏「+」用 `MiuixIcons.AddCircle`。不要再从 `androidx.compose.material.icons` 取；账号页已不 import 后者。
- **`GlassDialog` 与 `WindowDialog` / `OverlayDialog` 完全不是一回事，换过来有三处必须自己补**（前两条是踩过的坑，别再犯）：
  1. **它是 inline 的 `Box(fillMaxSize)`，不走 `Scaffold.popupHost`。** 所以它必须**排在 `Scaffold` 之后**，否则被不透明的页面整个盖住、什么都看不见。三个调用点都为此调整过：`AccountMiuix` 把页面与弹窗包进同一个 `Box`，`SettingsMiuix` / `ColorPaletteScreenMiuix` 把弹窗挪到 `Scaffold` 收尾之后。
  2. **它不能待在 `Modifier.layerBackdrop(backdrop)` 的录制子树里。** 玻璃表面自己要在录制层外面，否则「层录制一个会画层的表面」，渲染线程一路递归到爆栈（`Glass.kt` 的 KDoc 明确写了这条）。上面那条的挪动同时解决了这一点——挪之前三个弹窗里有两个正好在录制层内。改动这类弹窗时**必须同时检查**：弹窗位置是否在 `layerBackdrop` 之后。
  3. 它**没有** `title` / `summary` 槽，也**没有** `onDismissFinished` 回调。标题要自己画（`MiuixTheme.textStyles.title4`），关闭后的收尾（卸载弹窗、停轮询）要在调用方按 `show` 变化自己等一段（见 `AccountMiuix` 里的 `LaunchedEffect(showMhyDialog)` + `delay(200)`，对应 `GlassMotion.fadeOut()` 的 150ms）。
- **`GlassTransformPopup` 不能放进 `TopAppBar.actions`。** `TopAppBar` 结尾有 `.clipToBounds()`，面板一长出 52dp 的栏高就被裁掉——真机表现是「点加号菜单直接消失」。它必须挂在 `Scaffold` 的**同级**（外层 `Box` 里），只有触发按钮留在 actions 槽。example 的 `GlassPage` 就是这么摆的（popup 在 243 行的外层 Box，不在 topBar 里）。
- **每个 page 的 `Scaffold` 与 `GlassDialog`/`GlassTransformPopup` 必须包进同一个 `Box(Modifier.fillMaxSize())`。** pager 的 page 槽和 `NavDisplay` 的 entry 槽都只接受**一个**子节点，两个平级兄弟会被裁掉——真机表现同样是「弹窗永远不出现」。三个页面（Account / Settings / ColorPalette）都是这个结构。
- 菜单的接线**四件套**：`rememberGlassPopupAnchor()` 持有 anchor → 触发按钮加 `Modifier.glassPopupAnchor(anchor, cornerRadius = ButtonSize / 2)` → **按钮里的图标再加 `Modifier.glassPopupAnchorContent(anchor)`** → `GlassTransformPopup(anchor = …, backdrop = …, anchorContent = { 按钮里的那个图标 })`，行用 `GlassPopupItem`。`anchorContent` 是给「面板长出来时复制按钮内容」用的，要传图标本身，不是整个按钮。
- **`glassPopupAnchorContent` 不能省**（漏掉就是「点加号图标瞬移」的真因）。锚点有两个矩形：`glassPopupAnchor` 报的 `containerBounds`（整个控件）与 `glassPopupAnchorContent` 报的 `contentBounds`（图标那一小块）。不给 `contentBounds` 时 `GlassTransformPopup` 用 `anchorContent = startRect`（整个圆钮）来摆副本，而副本内容是按该矩形的 **TopStart** 放的 —— 图标于是落到圆钮左上角：锚点左上 `(1075.5, 311.5)` + 半个图标盒 `(42, 42)` = `(1117.5, 353.5)`，与真机逐帧实测的 `(1117, 354)` 完全吻合。症状：点「+」图标先跳到左上方、菜单展开、关菜单时同一处再闪一下才回到按钮上。
- 源系统里**列表卡片本来就不是玻璃材质**，别去把首页状态卡 / 任务卡 / 账号卡玻璃化。同样，下拉选择器（`WindowSpinnerPreference` / `OverlayDropdownPreference`）**没有**对应的玻璃组件，不要用 `glassPanel` 手搓。

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
- 自写的液态玻璃（`ui/component/FloatingBottomBar.kt`、`ui/component/liquid/`、`ui/component/miuix/animation/`、`ui/component/miuix/modifier/`）——已由 miuix-glass 取代
- 未使用的 strings（预测返回开关、定时、导航徽标、签到游戏/分区文案）

## 抓 UI 轨迹（纯视觉 BUG 用）

logcat 看不到这类问题（App 自己不打帧级日志），要看的是**帧时间线**：`bash scripts/ui-trace.sh [输出目录]`。

- 链路：唤醒屏幕 → 从 uiautomator 树里找控件坐标（不写死）→ `screenrecord` 录一段 → `ffmpeg` 抽成灰度 raw → 逐帧量控件亮像素的包围盒。
- 输出 `timeline.txt`（逐帧中心/bbox/尺寸）与 `keyframes.png`（关键帧拼图）。
- 踩过的坑，改脚本时别踩回去：屏幕休眠时 `screenrecord` 只报 `UNASSIGNED_LAYER_STACK` 并写出 0 字节；不等 `--time-limit` 走完就 `adb pull` 会拿到 `moov atom not found` 的半个文件；上一轮残留的 `screenrecord` 会让新一轮写不出 moov（脚本里先 `pkill`）；`screenrecord` 是变帧率，`r_frame_rate` 会谎报 60，真实帧率得用「总帧数 / 容器时长」；抽帧必须带 `-fps_mode passthrough`，否则补帧会让帧号与时间对不上；底栏 tab 要按「text 恰好等于 账号」+ 屏幕下半部找，子串匹配会命中 `content-description` 里的「添加账号」。
- 判定玻璃菜单动画好坏的两个硬指标：副本 bbox 宽度在整段动画里恒定（≈68.6px，坏的时候会拉到 247.5px）；按钮 bbox 在按下/松开时中心不位移（坏的时候左移 27px）。

## 测试

`./gradlew :app:testDebugUnitTest`，CI：`.github/workflows/test.yml`。

改了下面这些必须有/更新 JVM 测试：DS 签名、cookie 解析与 `buildFetchedWebCookie`、米游社已签判定、WorkBuddy `interpretDailyCheckin`、WorkBuddy 扫码协议（`WorkBuddyLoginTest`：信封 code 优先、11217 视为等待、驼峰字段、缺 uid 未就绪、缺 code 不算成功）、`ConfigTransferParser`、`SIGN_GAME_KEYS` / act_id / 域名。头像 hydrator 的「要不要拉」用 `MihoyoProfileHydratorTest` 钉，JWT 的 uid/exp 用 `WorkBuddyLabelTest` 钉。

## 提交

版本向提交跟现有风格：`v<versionName>: 一句话说明用户能感知的变化`。测试向：`test: …`。文档向：`docs: …`。不要把 `scripts/stutter_ab.py`（未跟踪的对照脚本）塞进提交。不要改 git config。不要在没要求时 push。
