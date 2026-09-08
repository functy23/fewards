package com.functy.fewards.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavKey

// kang from InstallerX-Revived（其源自 KernelSU manager，含防抖修改）
class Navigator(val backStack: NavBackStack) {

    // Push a key onto the back stack.
    fun push(key: NavKey) {
        if (backStack.lastOrNull() == key) {
            return
        }
        backStack.add(key)
    }

    // Pop the top key if present.
    fun pop() {
        if (backStackSize() <= 1) return
        backStack.removeLastOrNull()
    }

    // Get current NavKey on the back stack.
    fun current(): NavKey? = backStack.lastOrNull()

    // Get current size of back stack.
    fun backStackSize(): Int = backStack.size
}

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("LocalNavigator not provided")
}
