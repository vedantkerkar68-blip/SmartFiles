package com.smartfiles.core.common

/**
 * Minimal, dependency-free Result wrapper. Used across domain boundaries to
 * keep error handling explicit without leaking platform exceptions.
 */
sealed interface Result<out T> {
    data class Success<T>(val value: T) : Result<T>
    data class Failure(val error: Throwable, val message: String? = null) : Result<Nothing>
}

inline fun <T> runCatchingResult(block: () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: Throwable) {
    Result.Failure(e)
}

fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> value
    is Result.Failure -> null
}

fun <T> Result<T>.getOrElse(default: () -> T): T = when (this) {
    is Result.Success -> value
    is Result.Failure -> default()
}

suspend inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(value)
    return this
}
