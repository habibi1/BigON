package com.bigon.core.network.error

import com.bigon.core.common.AppError
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * Translates Retrofit/OkHttp failures into the app's typed error model.
 * Ordering matters: the timeout and connectivity types all extend [IOException].
 */
class NetworkErrorMapper @Inject constructor() {

    fun map(throwable: Throwable): AppError = when (throwable) {
        is SocketTimeoutException -> AppError.Network.Timeout

        is UnknownHostException,
        is ConnectException,
        -> AppError.Network.NoConnection

        is HttpException -> AppError.Network.Http(
            code = throwable.code(),
            body = runCatching { throwable.response()?.errorBody()?.string() }.getOrNull(),
        )

        is SerializationException ->
            AppError.Serialization(throwable.message ?: "Malformed response")

        is IOException -> AppError.Network.NoConnection

        else -> AppError.Unknown(throwable.message ?: (throwable::class.simpleName ?: "unknown"))
    }
}
