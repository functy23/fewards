package com.functy.fewards.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigTransferParserTest {

    @Test
    fun parsesFewardsConfigBundle() {
        val json = """
            {
              "_format": "fewards-config",
              "version": 1,
              "mihoyos": [{"stoken":"v2_a","stuid":"1","mid":"m","cookie":"stoken=v2_a; account_id=1; mid=m"}],
              "workbuddies": [{"token":"eyJhbGciOiJub25lIn0.e30.x","label":"WB"}]
            }
        """.trimIndent()
        val parsed = ConfigTransferParser.parse(json)
        assertEquals(1, parsed.mihoyos.size)
        assertEquals("v2_a", parsed.mihoyos[0].stoken)
        assertEquals(1, parsed.workbuddies.size)
        assertEquals("WB", parsed.workbuddies[0].label)
        assertTrue(parsed.errors.isEmpty())
    }

    @Test
    fun parsesBingAccountsFromBundle() {
        val json = """
            {
              "_format": "fewards-config",
              "version": 1,
              "bings": [
                {"id":"bing_abc","label":"Bing · abc123","refreshToken":"RT-abcdefghijklmnop","country":"CN"},
                {"label":"只有 refresh_token 也算数","refreshToken":"RT-2"}
              ]
            }
        """.trimIndent()
        val parsed = ConfigTransferParser.parse(json)
        assertEquals(2, parsed.bings.size)
        assertEquals("bing_abc", parsed.bings[0].id)
        assertEquals("CN", parsed.bings[0].country)
        // 缺 id 时用序号兜底，不能整条丢掉
        assertEquals("bing_1", parsed.bings[1].id)
        assertTrue(parsed.errors.isEmpty())
    }

    @Test
    fun skipsBingEntryWithoutRefreshToken() {
        val json = """{"_format":"fewards-config","bings":[{"label":"没有凭据"}]}"""
        val parsed = ConfigTransferParser.parse(json)
        assertTrue(parsed.bings.isEmpty())
        assertEquals(1, parsed.errors.size)
    }

    @Test
    fun bundleWithoutBingSectionStillParses() {
        // 老配置（加 Bing 之前导出的）必须继续能导入
        val json = """{"_format":"fewards-config","version":1,"mihoyos":[],"workbuddies":[]}"""
        val parsed = ConfigTransferParser.parse(json)
        assertTrue(parsed.bings.isEmpty())
        assertTrue(parsed.errors.isEmpty())
    }

    @Test
    fun skipsIncompleteMihoyoEntries() {
        val json = """{"_format":"fewards-config","mihoyos":[{"stoken":"only"}],"workbuddies":[{"token":""}]}"""
        val parsed = ConfigTransferParser.parse(json)
        assertTrue(parsed.mihoyos.isEmpty())
        assertTrue(parsed.workbuddies.isEmpty())
        assertEquals(2, parsed.errors.size)
    }

    @Test
    fun bareCookieDoesNotNeedToBeJson() {
        val parsed = ConfigTransferParser.parse("stoken=v2_abc; mid=xyz; account_id=10001")
        assertEquals(1, parsed.mihoyos.size)
        assertEquals("10001", parsed.mihoyos[0].stuid)
        assertTrue(parsed.workbuddies.isEmpty())
    }

    @Test
    fun bareJwtStartsWithEyJ() {
        val parsed = ConfigTransferParser.parse("eyJhbGciOiJub25lIn0.e30.sig")
        assertEquals(1, parsed.workbuddies.size)
        assertTrue(parsed.mihoyos.isEmpty())
    }

    @Test
    fun garbageIsUnrecognized() {
        val parsed = ConfigTransferParser.parse("hello world")
        assertFalse(parsed.recognized)
        assertTrue(parsed.mihoyos.isEmpty())
    }

    @Test
    fun emptyIsUnrecognized() {
        val parsed = ConfigTransferParser.parse("   ")
        assertFalse(parsed.recognized)
    }
}
