package com.functy.fewards.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 内存环形日志。tag = WB / MHY / SYS。
 * 严禁把 cookie / token 值写入日志（调用方负责打码）。
 */
object AppLog {

    enum class Level { DEBUG, INFO, WARN, ERROR }

    data class LogEntry(
        val time: Long,
        val tag: String,
        val level: Level,
        val message: String,
    ) {
        fun format(): String {
            val timeText = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date(time))
            return "$timeText [$tag] ${level.name}: $message"
        }
    }

    private const val MAX_ENTRIES = 500

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun i(tag: String, message: String) = log(tag, Level.INFO, message)
    fun w(tag: String, message: String) = log(tag, Level.WARN, message)
    fun e(tag: String, message: String) = log(tag, Level.ERROR, message)
    fun d(tag: String, message: String) = log(tag, Level.DEBUG, message)

    private fun log(tag: String, level: Level, message: String) {
        val entry = LogEntry(System.currentTimeMillis(), tag, level, message)
        val updated = _logs.value + entry
        _logs.value = if (updated.size > MAX_ENTRIES) updated.takeLast(MAX_ENTRIES) else updated
    }

    fun clear() {
        _logs.value = emptyList()
    }

    fun dumpText(): String = _logs.value.joinToString("\n") { it.format() }
}
