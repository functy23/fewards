package com.functy.fewards.core.mihoyo

/**
 * 米游社接口常量与业务映射。移植自 MiyoQian constants.py。
 * 接口地址 / act_id 可根据实际抓包微调。
 */
object MihoyoConstants {

    const val TAKUMI_API = "https://api-takumi.mihoyo.com"
    const val BBS_API = "https://bbs-api.miyoushe.com"
    const val PASSPORT_API = "https://passport-api.mihoyo.com"
    const val ZZZ_ACT_API = "https://act-nap-api.mihoyo.com"

    const val ACCOUNT_ROLES_URL = "$TAKUMI_API/binding/api/getUserGameRolesByCookie"

    const val GAME_HOME_URL = "$TAKUMI_API/event/luna/home?lang=zh-cn"
    const val GAME_INFO_URL = "$TAKUMI_API/event/luna/info?lang=zh-cn"
    const val GAME_SIGN_URL = "$TAKUMI_API/event/luna/sign"

    const val ZZZ_HOME_URL = "$ZZZ_ACT_API/event/luna/zzz/home?lang=zh-cn"
    const val ZZZ_INFO_URL = "$ZZZ_ACT_API/event/luna/zzz/info?lang=zh-cn"
    const val ZZZ_SIGN_URL = "$ZZZ_ACT_API/event/luna/zzz/sign"

    const val BBS_TASKS_URL = "$BBS_API/apihub/wapi/getUserMissionsState"
    const val BBS_SIGN_URL = "$BBS_API/apihub/app/api/signIn"
    const val BBS_POST_LIST_URL = "$BBS_API/post/api/getForumPostList"
    const val BBS_DETAIL_URL = "$BBS_API/post/api/getPostFull"
    const val BBS_SHARE_URL = "$BBS_API/apihub/api/getShareConf"
    const val BBS_LIKE_URL = "$BBS_API/post/api/post/upvote"
    const val BBS_CREATE_VERIFICATION_URL = "$BBS_API/misc/api/createVerification?is_high=true"
    const val BBS_VERIFY_VERIFICATION_URL = "$BBS_API/misc/api/verifyVerification"

    const val LTOKEN_BY_STOKEN_URL = "$PASSPORT_API/account/auth/api/getLTokenBySToken"
    const val COOKIE_TOKEN_BY_STOKEN_URL = "$PASSPORT_API/account/auth/api/getCookieAccountInfoBySToken"
    const val COOKIE_TOKEN_REFRESH_URL = "$TAKUMI_API/auth/api/getCookieAccountInfoBySToken"

    const val QRCODE_FETCH_URL = "$PASSPORT_API/account/ma-cn-passport/app/createQRLogin"
    const val QRCODE_QUERY_URL = "$PASSPORT_API/account/ma-cn-passport/app/queryQRLoginStatus"

    // 六游戏常量表（MiyoQian 原值）
    data class Game(
        val key: String,
        val name: String,
        val role: String,
        val gameBiz: String,
        val actId: String,
        val homeUrl: String,
        val infoUrl: String,
        val signUrl: String,
        val extraHeaders: Map<String, String>,
    )

    val GAMES: Map<String, Game> = listOf(
        Game(
            "genshin", "原神", "旅行者", "hk4e_cn", "e202311201442471",
            GAME_HOME_URL, GAME_INFO_URL, GAME_SIGN_URL, mapOf("x-rpc-signgame" to "hk4e")
        ),
        Game(
            "starrail", "崩坏：星穹铁道", "开拓者", "hkrpg_cn", "e202304121516551",
            GAME_HOME_URL, GAME_INFO_URL, GAME_SIGN_URL, emptyMap()
        ),
        Game(
            "zzz", "绝区零", "绳匠", "nap_cn", "e202406242138391",
            ZZZ_HOME_URL, ZZZ_INFO_URL, ZZZ_SIGN_URL, mapOf("x-rpc-signgame" to "zzz")
        ),
        Game(
            "honkai3rd", "崩坏3", "舰长", "bh3_cn", "e202306201626331",
            GAME_HOME_URL, GAME_INFO_URL, GAME_SIGN_URL, emptyMap()
        ),
        Game(
            "tears", "未定事件簿", "律师", "nxx_cn", "e202202251749321",
            GAME_HOME_URL, GAME_INFO_URL, GAME_SIGN_URL, emptyMap()
        ),
        Game(
            "honkai2", "崩坏学园2", "玩家", "bh2_cn", "e202203291431091",
            GAME_HOME_URL, GAME_INFO_URL, GAME_SIGN_URL, emptyMap()
        ),
    ).associateBy { it.key }

    val GAME_CHOICES: List<String> =
        GAMES.values.map { "${it.name} (${it.role})" }

    fun gameKeyByChoice(choice: String): String =
        GAMES.values.firstOrNull { "${it.name} (${it.role})" == choice }?.key ?: "genshin"

    fun gameChoiceByKey(key: String): String =
        GAMES[key]?.let { "${it.name} (${it.role})" } ?: GAME_CHOICES.first()

    // 分区表：gids（社区签到）↔ forum_id（帖子列表）
    data class Forum(val gids: String, val forumId: String, val name: String)

    val BBS_FORUMS: Map<Int, Forum> = mapOf(
        1 to Forum("1", "1", "崩坏3"),
        2 to Forum("2", "26", "原神"),
        3 to Forum("3", "30", "崩坏2"),
        4 to Forum("4", "37", "未定事件簿"),
        5 to Forum("5", "34", "大别野"),
        6 to Forum("6", "52", "崩坏：星穹铁道"),
        8 to Forum("8", "57", "绝区零"),
    )

    val FORUM_CHOICES: List<String> =
        BBS_FORUMS.values.map { "${it.name} (gids=${it.gids})" }

    fun forumGidsByChoice(choice: String): Int =
        BBS_FORUMS.values.firstOrNull { "${it.name} (gids=${it.gids})" == choice }?.gids?.toIntOrNull() ?: 5

    // mission 映射：58=社区签到 59=看帖 60=点赞 61=分享
    const val MISSION_SIGN = 58
    const val MISSION_READ = 59
    const val MISSION_LIKE = 60
    const val MISSION_SHARE = 61

    const val READ_COUNT = 3
    const val LIKE_COUNT = 5
    const val SHARE_COUNT = 1

    // retcode
    const val RET_OK = 0
    const val RET_COOKIE_EXPIRED = -100
    const val RET_ALREADY_SIGNED = -5003
    const val RET_CAPTCHA = 1034

    const val STOKEN_COOKIE_URL = "https://api-takumi.mihoyo.com/auth/api/getCookieAccountInfoBySToken"
}
