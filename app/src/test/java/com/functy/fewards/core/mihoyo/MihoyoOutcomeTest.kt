package com.functy.fewards.core.mihoyo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MihoyoOutcomeTest {

    @Test
    fun alreadySignedRetcodeIsSuccess() {
        assertTrue(MihoyoEngine.isAlreadyDoneRet(MihoyoConstants.RET_ALREADY_SIGNED, ""))
    }

    @Test
    fun alreadySignedMessageIsSuccess() {
        assertTrue(MihoyoEngine.isAlreadyDoneRet(-1, "已签到"))
        assertTrue(MihoyoEngine.isAlreadyDoneRet(-1, "already signed"))
    }

    @Test
    fun okRetcodeIsNotAlreadyDone() {
        assertFalse(MihoyoEngine.isAlreadyDoneRet(MihoyoConstants.RET_OK, "ok"))
    }

    @Test
    fun cookieExpiredAndCaptchaFlags() {
        assertTrue(MihoyoEngine.isCookieExpired(MihoyoConstants.RET_COOKIE_EXPIRED))
        assertFalse(MihoyoEngine.isCookieExpired(0))
        assertTrue(MihoyoEngine.isCaptcha(MihoyoConstants.RET_CAPTCHA))
        assertFalse(MihoyoEngine.isCaptcha(0))
    }

    @Test
    fun bbsIdleWhenCanGetPointsZero() {
        assertTrue(MihoyoEngine.isBbsIdle(0))
        assertFalse(MihoyoEngine.isBbsIdle(1))
    }
}
