package com.smartfiles.core.common

/**
 * Logger abstraction to keep logging policy centralized without leaking an
 * Android logging dependency into pure modules. Implementations must never log
 * personal document contents or secrets (spec §24).
 */
interface AppLogger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
