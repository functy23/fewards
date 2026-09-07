package com.functy.fewards.core.mihoyo

import java.security.MessageDigest
import java.util.UUID

/**
 * 米游社请求签名（DS）。移植自 MiyoQian core/crypto.py。
 * 各 salt 可根据实际抓包微调。
 */
object DsSign {

    const val BBS_VERSION = "2.106.2"
    const val QR_LOGIN_VERSION = "2.102.1"
    const val PASSPORT_APP_VERSION = "2.90.1"

    const val BBS_SALT = "idMMaGYmVgPzh3wxmWudUXKUPGidO7GM"
    const val BBS_WEB_SALT = "G1ktdwFL4IyGkHuuWSmz0wUe9Db9scyK"
    const val BBS_X6_SALT = "t0qEgfub6cvueAPgR5m9aQWWVciEer7v"
    const val PASSPORT_X4_SALT = "xV8v4Qu54lUKrEYFZkJhB8cuOh9Asafs"
    const val PASSPORT_APP_SALT = "dDIQHbKOdaPaLuvQKVzUzqdeCaxjtaPV"

    const val PASSPORT_APP_ID = "bll8iq97cem8"

    fun md5(text: String): String =
        MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun randomText(length: Int): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }

    /** DS1（web=false → app salt；web=true → web salt）。 */
    fun ds(web: Boolean = false): String {
        val salt = if (web) BBS_WEB_SALT else BBS_SALT
        val t = (System.currentTimeMillis() / 1000).toString()
        val r = randomText(6)
        return "$t,$r,${md5("salt=$salt&t=$t&r=$r")}"
    }

    /** DS2（X6 salt），用于米游币社区签到等 POST。 */
    fun dsX6(query: String = "", body: String = ""): String {
        val t = (System.currentTimeMillis() / 1000).toString()
        val r = (100001..200000).random().toString()
        return "$t,$r,${md5("salt=$BBS_X6_SALT&t=$t&r=$r&b=$body&q=$query")}"
    }

    /** X4 salt，用于 passport 接口。 */
    fun dsX4(query: String = "", body: String = ""): String {
        val t = (System.currentTimeMillis() / 1000).toString()
        val r = (100000..200000).random().toString()
        return "$t,$r,${md5("salt=$PASSPORT_X4_SALT&t=$t&r=$r&b=$body&q=$query")}"
    }

    /** passport app salt。 */
    fun dsApp(body: String = "", query: String = ""): String {
        val t = (System.currentTimeMillis() / 1000).toString()
        val r = (100001..200000).random().toString()
        return "$t,$r,${md5("salt=$PASSPORT_APP_SALT&t=$t&r=$r&b=$body&q=$query")}"
    }

    /** 由种子生成稳定 device_id（uuid3 语义：MD5 + nameUUIDFromBytes）。 */
    fun deviceId(seed: String): String =
        UUID.nameUUIDFromBytes(seed.toByteArray(Charsets.UTF_8)).toString()

    fun randomDeviceId(): String = UUID.randomUUID().toString()

    /** 伪 device_fp：md5(seed) 前 8 + 32 hex（共 40）。 */
    fun deviceFp(seed: String): String {
        val base = md5(seed)
        val extra = buildString {
            repeat(32) { append("0123456789abcdef".random()) }
        }
        return (base.take(8) + extra).take(40)
    }

    const val DEFAULT_MOBILE_UA: String =
        "Mozilla/5.0 (Linux; Android 12; Unspecified Device) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 " +
            "Chrome/103.0.5060.129 Mobile Safari/537.36 miHoYoBBS/$BBS_VERSION"

    const val QR_MOBILE_UA: String =
        "Mozilla/5.0 (Linux; Android 12) Mobile miHoYoBBS/$QR_LOGIN_VERSION"

    const val PASSPORT_APP_UA: String =
        "Mozilla/5.0 miHoYoBBS/$PASSPORT_APP_VERSION Capture/2.2.0"
}
