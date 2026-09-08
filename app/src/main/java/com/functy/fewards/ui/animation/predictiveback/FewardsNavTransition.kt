package com.functy.fewards.ui.animation.predictiveback

import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitions

enum class PredictiveBackAnimation { None, MIUIX, AOSP, Scale, Classic }

fun installerNavTransition(animation: PredictiveBackAnimation): NavTransition = when (animation) {
    PredictiveBackAnimation.None -> NoPredictiveBackTransition
    PredictiveBackAnimation.MIUIX -> NavTransitions.MiuixDefault
    PredictiveBackAnimation.AOSP -> AospNavTransition
    PredictiveBackAnimation.Scale -> scaleNavTransition()
    PredictiveBackAnimation.Classic -> ClassicNavTransition
}
