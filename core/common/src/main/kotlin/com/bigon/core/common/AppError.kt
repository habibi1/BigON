package com.bigon.core.common

sealed interface AppError {

    sealed interface Network : AppError {
        data object NoConnection : Network
        data object Timeout : Network
        data class Http(val code: Int, val body: String?) : Network
    }

    data class Serialization(val cause: String) : AppError

    data class Unknown(val cause: String) : AppError
}
