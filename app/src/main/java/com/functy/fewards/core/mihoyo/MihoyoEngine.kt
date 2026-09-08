package com.functy.fewards.core.mihoyo

import com.functy.fewards.core.AppLog
import com.functy.fewards.data.repository.AccountRepository
import com.functy.fewards.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/**
 * 米游社签到引擎：游戏社区签到（luna）+ 米游币社区任务。
 * 逻辑移植自 MiyoQian（tasks/games.py、tasks/bbs.py）。
 * retcode 语义：-5003/已签 → 幂等成功；1034 → 验证码（默认跳过并记录）；-100 → 刷 cookie_token 重试一次。
 */
class MihoyoEngine(
    private val api: MihoyoApi,
    private val settings: SettingsRepository,
    private val accounts: List<AccountRepository.MihoyoAccount>,
) {

    private fun emit(message: String) = AppLog.i("MHY", message)

    private suspend fun sleep() {
        delay(Random.nextLong(1000, 3000))
    }

    // ==================== 请求头 ====================

    private fun appHeaders(account: AccountRepository.MihoyoAccount, deviceId: String, deviceFp: String): Map<String, String> =
        buildMap {
            put("DS", DsSign.ds(web = false))
            put("cookie", runCatching { MihoyoApi.stokenCookie(account) }.getOrDefault(""))
            put("x-rpc-client_type", "2")
            put("x-rpc-app_version", DsSign.BBS_VERSION)
            put("x-rpc-sys_version", "12")
            put("x-rpc-channel", "miyousheluodi")
            put("x-rpc-device_id", deviceId)
            put("x-rpc-device_name", "Xiaomi MI 6")
            put("x-rpc-device_model", "Mi 6")
            put("x-rpc-h265_supported", "1")
            put("Referer", "https://app.mihoyo.com")
            put("Content-Type", "application/json; charset=UTF-8")
            put("x-rpc-verify_key", DsSign.PASSPORT_APP_ID)
            put("x-rpc-csm_source", "home")
            put("User-Agent", "okhttp/4.9.3")
            if (deviceFp.isNotEmpty()) put("x-rpc-device_fp", deviceFp)
        }

    private fun webHeaders(account: AccountRepository.MihoyoAccount, deviceId: String): Map<String, String> = mapOf(
        "Accept" to "application/json, text/plain, */*",
        "Origin" to "https://webstatic.mihoyo.com",
        "User-Agent" to DsSign.DEFAULT_MOBILE_UA,
        "Referer" to "https://webstatic.mihoyo.com",
        "Accept-Language" to "zh-CN,en-US;q=0.8",
        "X-Requested-With" to "com.mihoyo.hyperion",
        "Cookie" to MihoyoApi.webCookie(account),
        "x-rpc-device_id" to deviceId,
    )

    private fun gameHeaders(account: AccountRepository.MihoyoAccount, game: MihoyoConstants.Game, deviceId: String): Map<String, String> =
        buildMap {
            put("Accept", "application/json, text/plain, */*")
            put("DS", DsSign.ds(web = true))
            put("x-rpc-channel", "miyousheluodi")
            put("Origin", "https://act.mihoyo.com")
            put("x-rpc-app_version", DsSign.BBS_VERSION)
            put("User-Agent", DsSign.DEFAULT_MOBILE_UA)
            put("x-rpc-client_type", "5")
            put("Referer", "https://act.mihoyo.com/")
            put("Accept-Language", "zh-CN,en-US;q=0.8")
            put("X-Requested-With", "com.mihoyo.hyperion")
            put("Cookie", MihoyoApi.webCookie(account))
            put("x-rpc-device_id", deviceId)
            putAll(game.extraHeaders)
        }

    // ==================== 对外入口 ====================

    /** 执行全部已启用的米游社任务（遍历多账号）。返回是否整体成功。 */
    suspend fun runAll(onAccountDone: (Int, Int) -> Unit = { _, _ -> }): Boolean =
        withContext(Dispatchers.IO) {
            var allOk = true
            for ((index, account) in accounts.withIndex()) {
                emit("── 账号 ${account.nickname} ──")
                val ok = runAccount(account)
                if (!ok) allOk = false
                onAccountDone(index + 1, accounts.size)
            }
            allOk
        }

    private suspend fun runAccount(account: AccountRepository.MihoyoAccount): Boolean {
        var ok = true
        val deviceId = DsSign.deviceId(account.stoken + account.stuid)
        val deviceFp = DsSign.deviceFp(deviceId)

        if (settings.mhyGameSign) {
            if (!runGameSign(account, deviceId)) ok = false
        }
        if (settings.mhyBbsSign || settings.mhyRead || settings.mhyLike || settings.mhyShare) {
            if (!runBbsTasks(account, deviceId, deviceFp)) ok = false
        }
        return ok
    }

    // ==================== 游戏社区签到（luna） ====================

    private suspend fun runGameSign(account: AccountRepository.MihoyoAccount, deviceId: String): Boolean {
        var ok = true
        val enabledGames = settings.mhySignGames.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        emit("== 游戏社区签到 ==")
        for (gameKey in enabledGames) {
            val game = MihoyoConstants.GAMES[gameKey] ?: run {
                emit("[跳过] 未知游戏配置: $gameKey")
                continue
            }
            val gameOk = signGame(account, game, deviceId)
            if (!gameOk) ok = false
            sleep()
        }
        return ok
    }

    private suspend fun signGame(
        account: AccountRepository.MihoyoAccount,
        game: MihoyoConstants.Game,
        deviceId: String,
        retried: Boolean = false,
    ): Boolean {
        emit("正在获取${game.name}绑定角色")
        val rolesData = api.getJson(
            MihoyoConstants.ACCOUNT_ROLES_URL,
            gameHeaders(account, game, deviceId) + mapOf("DS" to DsSign.ds(web = true)),
            params = mapOf("game_biz" to game.gameBiz)
        ).let { data ->
            if (data.optInt("retcode") == MihoyoConstants.RET_COOKIE_EXPIRED && !retried) {
                val newCookie = api.refreshCookieToken(account)
                if (newCookie != null) {
                    api.getJson(
                        MihoyoConstants.ACCOUNT_ROLES_URL,
                        gameHeaders(account.copy(cookie = newCookie), game, deviceId),
                        params = mapOf("game_biz" to game.gameBiz)
                    )
                } else data
            } else data
        }
        if (rolesData.optInt("retcode") != 0) {
            emit("${game.name} 获取角色失败: ${rolesData.optString("message")}(${rolesData.optInt("retcode")})")
            return false
        }
        val roles = rolesData.optJSONObject("data")?.optJSONArray("list") ?: JSONArray()
        if (roles.length() == 0) {
            emit("${game.name} 未找到绑定角色")
            return true // 无角色不算失败
        }

        emit("正在获取${game.name}签到奖励列表")
        val awardsData = api.getJson(
            game.homeUrl,
            gameHeaders(account, game, deviceId),
            params = mapOf("act_id" to game.actId)
        )
        val awards = if (awardsData.optInt("retcode") == 0) {
            awardsData.optJSONObject("data")?.optJSONArray("awards") ?: JSONArray()
        } else JSONArray()

        var ok = true
        for (i in 0 until roles.length()) {
            val role = roles.getJSONObject(i)
            val uid = role.optString("game_uid")
            val nickname = role.optString("nickname").ifEmpty { uid }
            val region = role.optString("region")
            val label = "${game.name} $nickname($uid)"

            val infoData = api.getJson(
                game.infoUrl,
                gameHeaders(account, game, deviceId),
                params = mapOf(
                    "act_id" to game.actId,
                    "region" to region,
                    "uid" to uid,
                    "lang" to "zh-cn",
                )
            )
            if (infoData.optInt("retcode") != 0) {
                emit("$label 查询签到状态失败: ${infoData.optString("message")}")
                ok = false
                continue
            }
            val info = infoData.optJSONObject("data") ?: JSONObject()
            if (info.optBoolean("first_bind")) {
                emit("$label 首次绑定，请先手动签到一次")
                continue
            }
            val signed = info.optBoolean("is_sign")
            val dayIndex = (info.optInt("total_sign_day", 1) - 1).coerceAtLeast(0)
            if (signed) {
                emit("$label 今日已签到，奖励 ${describeAward(awards, dayIndex)}")
                continue
            }

            emit("正在为${label}签到")
            val signData = api.postJson(
                game.signUrl,
                JSONObject()
                    .put("act_id", game.actId)
                    .put("region", region)
                    .put("uid", uid)
                    .toString(),
                gameHeaders(account, game, deviceId)
            )
            when {
                signData.optInt("retcode") == MihoyoConstants.RET_ALREADY_SIGNED -> {
                    emit("$label 今日已签到，奖励 ${describeAward(awards, dayIndex)}")
                }
                signData.optInt("retcode") != 0 -> {
                    emit("$label 签到失败: ${signData.optString("message")}(${signData.optInt("retcode")})")
                    ok = false
                }
                (signData.optJSONObject("data") ?: JSONObject()).optInt("success") == 1 -> {
                    // 触发验证码：默认跳过并记录；可选打码接口（可根据实际抓包微调）
                    if (settings.mhyCaptchaPolicy == 1 && settings.mhyCaptchaApiUrl.isNotEmpty()) {
                        val solved = solveGameCaptcha(signData.optJSONObject("data") ?: JSONObject())
                        if (solved != null) {
                            emit("$label 验证码已处理，重试签到")
                            val retry = api.postJson(
                                game.signUrl,
                                JSONObject()
                                    .put("act_id", game.actId)
                                    .put("region", region)
                                    .put("uid", uid)
                                    .toString(),
                                gameHeaders(account, game, deviceId) + mapOf(
                                    "x-rpc-challenge" to solved.first,
                                    "x-rpc-validate" to solved.second,
                                    "x-rpc-seccode" to "${solved.second}|jordan",
                                )
                            )
                            if (retry.optInt("retcode") == 0) {
                                emit("$label 签到成功，奖励 ${describeAward(awards, dayIndex + 1)}")
                            } else {
                                emit("$label 验证码处理后仍失败: ${retry.optString("message")}")
                                ok = false
                            }
                        } else {
                            emit("$label 触发验证码且打码失败，本次跳过")
                            ok = false
                        }
                    } else {
                        emit("$label 触发验证码，已跳过并记录")
                        ok = false
                    }
                }
                else -> emit("$label 签到成功，奖励 ${describeAward(awards, dayIndex + 1)}")
            }
            sleep()
        }
        return ok
    }

    private suspend fun solveGameCaptcha(data: JSONObject): Pair<String, String>? {
        val gt = data.optString("gt")
        val challenge = data.optString("challenge")
        if (gt.isEmpty() || challenge.isEmpty()) return null
        return callCaptchaApi(gt, challenge)
    }

    /** 打码接口约定：POST {gt, challenge} → {validate}（可根据实际抓包微调）。 */
    private suspend fun callCaptchaApi(gt: String, challenge: String): Pair<String, String>? =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject().put("gt", gt).put("challenge", challenge).toString()
                val req = okhttp3.Request.Builder()
                    .url(settings.mhyCaptchaApiUrl)
                    .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
                com.functy.fewards.fewardsApp.okhttpClient.newCall(req).execute().use { resp ->
                    val json = JSONObject(resp.body?.string() ?: "{}")
                    val validate = json.optString("validate").ifEmpty { json.optJSONObject("data")?.optString("validate") ?: "" }
                    if (validate.isNotEmpty()) challenge to validate else null
                }
            }.getOrNull()
        }

    private fun describeAward(awards: JSONArray, index: Int): String {
        if (awards.length() == 0) return "未知"
        val i = index.coerceIn(0, awards.length() - 1)
        val award = awards.getJSONObject(i)
        return "「${award.optString("name", "未知")}」x${award.optString("cnt", "?")}"
    }

    // ==================== 米游币社区任务 ====================

    private suspend fun runBbsTasks(
        account: AccountRepository.MihoyoAccount,
        deviceId: String,
        deviceFp: String,
    ): Boolean {
        emit("== 米游币社区任务 ==")
        var stokenCookieStr = runCatching { MihoyoApi.stokenCookie(account) }.getOrNull()
        if (stokenCookieStr == null) {
            emit("缺少 stuid/stoken，无法执行米游币社区任务")
            return false
        }

        var state = taskState(account, deviceId)
        if (state == null) {
            emit("任务状态获取失败，请检查 cookie/stoken")
            return false
        }
        var canGet = state.optInt("can_get_points", 0)
        var received = state.optInt("already_received_points", 0)
        val total = state.optInt("total_points", 0)
        var flags = taskFlags(state)
        val possibleToday = received + canGet
        emit("米游币今日进度：已获得 $received，还可获得 $canGet")
        if (canGet == 0) {
            emit("今日任务已完成，今日已得 $received，当前总计 $total")
            return true
        }

        var ok = true
        val headers = appHeaders(account, deviceId, deviceFp)

        // 社区签到
        if (settings.mhyBbsSign && !flags.optBoolean("sign")) {
            val forumGids = settings.mhyForums.split(",").mapNotNull { it.trim().toIntOrNull() }
            for (gid in forumGids) {
                val forum = MihoyoConstants.BBS_FORUMS[gid] ?: continue
                emit("正在进行${forum.name}社区签到")
                val body = JSONObject().put("gids", forum.gids).toString()
                val data = api.postJson(
                    MihoyoConstants.BBS_SIGN_URL, body,
                    headers + mapOf("DS" to DsSign.dsX6(body = body))
                )
                when (data.optInt("retcode")) {
                    MihoyoConstants.RET_OK -> emit("${forum.name} 社区签到成功")
                    MihoyoConstants.RET_CAPTCHA -> emit("${forum.name} 社区签到触发验证码，已跳过")
                    else -> {
                        emit("${forum.name} 社区签到失败: ${data.optString("message")}")
                        ok = false
                    }
                }
                sleep()
            }
        } else if (settings.mhyBbsSign) {
            emit("社区签到已完成，跳过")
        }

        // 帖子任务
        val needPosts = (settings.mhyRead && !flags.optBoolean("read")) ||
            (settings.mhyLike && !flags.optBoolean("like")) ||
            (settings.mhyShare && !flags.optBoolean("share"))
        var posts: List<Triple<String, String, String>> = emptyList()
        if (needPosts) {
            emit("正在获取帖子列表")
            posts = fetchPosts(account, deviceId)
            if (posts.isEmpty()) emit("获取帖子列表失败，无法执行看帖/点赞/分享")
        }

        if (settings.mhyRead && !flags.optBoolean("read")) {
            val limit = flags.optInt("read_num", MihoyoConstants.READ_COUNT).coerceAtLeast(1)
            for ((postId, title, _) in posts.take(limit)) {
                emit("正在浏览: $title")
                val data = api.getJson(
                    MihoyoConstants.BBS_DETAIL_URL,
                    appHeaders(account, deviceId, deviceFp),
                    params = mapOf("post_id" to postId)
                )
                if (data.optString("message") == "OK") emit("阅读成功: $title") else {
                    emit("阅读失败: $title (${data.optString("message")})")
                    ok = false
                }
                sleep()
            }
        }

        if (settings.mhyLike && !flags.optBoolean("like")) {
            val limit = flags.optInt("like_num", MihoyoConstants.LIKE_COUNT).coerceAtLeast(1)
            for ((postId, title, gids) in posts.take(limit)) {
                emit("正在点赞: $title")
                val body = JSONObject()
                    .put("post_id", postId)
                    .put("is_cancel", false)
                    .put("gids", gids)
                    .toString()
                val data = api.postJson(MihoyoConstants.BBS_LIKE_URL, body, appHeaders(account, deviceId, deviceFp))
                if (data.optString("message") == "OK") {
                    emit("点赞成功: $title")
                    if (settings.mhyCancelLike) {
                        sleep()
                        emit("正在取消点赞: $title")
                        api.postJson(
                            MihoyoConstants.BBS_LIKE_URL,
                            JSONObject(body).put("is_cancel", true).toString(),
                            appHeaders(account, deviceId, deviceFp)
                        )
                    }
                } else if (data.optInt("retcode") == MihoyoConstants.RET_CAPTCHA) {
                    emit("点赞触发验证码，已跳过: $title")
                } else {
                    emit("点赞失败: $title (${data.optString("message")})")
                    ok = false
                }
                sleep()
            }
        }

        if (settings.mhyShare && !flags.optBoolean("share")) {
            for ((postId, title, _) in posts.take(MihoyoConstants.SHARE_COUNT)) {
                emit("正在分享: $title")
                // web 通道优先，403/失败退避后 app 通道重试一次（可根据实际抓包微调）
                var data = api.getJson(
                    MihoyoConstants.BBS_SHARE_URL,
                    webHeaders(account, deviceId),
                    params = mapOf("entity_id" to postId, "entity_type" to "1")
                )
                if (data.optString("message") != "OK") {
                    delay(800)
                    data = api.getJson(
                        MihoyoConstants.BBS_SHARE_URL,
                        appHeaders(account, deviceId, deviceFp),
                        params = mapOf("entity_id" to postId, "entity_type" to "1")
                    )
                }
                if (data.optString("message") == "OK") emit("分享成功: $title") else {
                    emit("分享失败: $title (${data.optString("message")})")
                    ok = false
                }
                sleep()
            }
        }

        // 汇总
        state = taskState(account, deviceId) ?: state
        val finalReceived = state.optInt("already_received_points", received)
        emit("社区任务结束：今日已得 $finalReceived，当前总计 ${state.optInt("total_points", total)}")
        return ok
    }

    /** 任务状态（web 裸头无 DS；-100 刷 cookie_token 重试一次）。 */
    private suspend fun taskState(account: AccountRepository.MihoyoAccount, deviceId: String): JSONObject? {
        val web = webHeaders(account, deviceId)
        var data = api.getJson(MihoyoConstants.BBS_TASKS_URL, web, params = mapOf("point_sn" to "myb"))
        if (data.optInt("retcode") == MihoyoConstants.RET_COOKIE_EXPIRED) {
            val newCookie = api.refreshCookieToken(account) ?: return null
            account.cookie.let { }
            data = api.getJson(
                MihoyoConstants.BBS_TASKS_URL,
                webHeaders(account.copy(cookie = newCookie), deviceId),
                params = mapOf("point_sn" to "myb")
            )
        }
        return if (data.optInt("retcode") == 0) data.optJSONObject("data") else null
    }

    private fun taskFlags(state: JSONObject): JSONObject {
        val flags = JSONObject()
            .put("sign", false)
            .put("read", false)
            .put("read_num", MihoyoConstants.READ_COUNT)
            .put("like", false)
            .put("like_num", MihoyoConstants.LIKE_COUNT)
            .put("share", false)
        val missions = state.optJSONArray("states") ?: return flags
        for (i in 0 until missions.length()) {
            val mission = missions.getJSONObject(i)
            val id = mission.optInt("mission_id")
            val done = mission.optBoolean("is_get_award")
            val happened = mission.optInt("happened_times", 0)
            when (id) {
                MihoyoConstants.MISSION_SIGN -> flags.put("sign", done)
                MihoyoConstants.MISSION_READ -> {
                    if (done) flags.put("read", true)
                    else flags.put("read_num", (flags.optInt("read_num") - happened).coerceAtLeast(0))
                }
                MihoyoConstants.MISSION_LIKE -> {
                    if (done) flags.put("like", true)
                    else flags.put("like_num", (flags.optInt("like_num") - happened).coerceAtLeast(0))
                }
                MihoyoConstants.MISSION_SHARE -> flags.put("share", done)
            }
        }
        return flags
    }

    private suspend fun fetchPosts(
        account: AccountRepository.MihoyoAccount,
        deviceId: String,
    ): List<Triple<String, String, String>> {
        val forumGids = settings.mhyForums.split(",").mapNotNull { it.trim().toIntOrNull() }
        val forum = MihoyoConstants.BBS_FORUMS[forumGids.firstOrNull() ?: 5] ?: return emptyList()
        val data = api.getJson(
            MihoyoConstants.BBS_POST_LIST_URL,
            appHeaders(account, deviceId, DsSign.deviceFp(deviceId)),
            params = mapOf(
                "forum_id" to forum.forumId,
                "is_good" to "false",
                "is_hot" to "false",
                "page_size" to "20",
                "sort_type" to "1",
            )
        )
        if (data.optInt("retcode") != 0) return emptyList()
        val list = data.optJSONObject("data")?.optJSONArray("list") ?: return emptyList()
        val posts = mutableListOf<Triple<String, String, String>>()
        for (i in 0 until list.length()) {
            val post = list.getJSONObject(i).optJSONObject("post") ?: continue
            val postId = post.optString("post_id")
            val title = post.optString("subject").ifEmpty { postId }
            if (postId.isNotEmpty()) posts.add(Triple(postId, title, forum.gids))
        }
        posts.shuffle()
        return posts.take(5)
    }
}

