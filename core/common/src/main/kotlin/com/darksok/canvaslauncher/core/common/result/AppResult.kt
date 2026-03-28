package com.darksok.canvaslauncher.core.common.result

sealed interface AppError {
    data object NotFound : AppError
    data object LaunchUnavailable : AppError
    data class Unknown(val message: String? = null) : AppError
}

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> {
    return when (this) {
        is AppResult.Success -> AppResult.Success(transform(value))
        is AppResult.Failure -> this
    }
}
