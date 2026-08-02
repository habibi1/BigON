package com.bigon.core.ui

import com.bigon.core.common.AppError

fun AppError.toUiText(): UiText = UiText.Dynamic(
    when (this) {
        AppError.Network.NoConnection -> "No internet connection"
        AppError.Network.Timeout -> "The request timed out — try again"
        is AppError.Network.Http -> "Server error ($code)"
        is AppError.Serialization -> "Something went wrong while reading data"
        is AppError.Unknown -> "Something went wrong"
    },
)
