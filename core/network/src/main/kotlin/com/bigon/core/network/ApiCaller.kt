package com.bigon.core.network

import com.bigon.core.common.AppResult
import com.bigon.core.network.error.NetworkErrorMapper
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Wraps a Retrofit suspend call so no exception escapes the network boundary:
 * transport, HTTP and parsing failures all come back as [AppResult.Failure]
 * with a typed [com.bigon.core.common.AppError].
 *
 * Repositories call it like:
 * ```
 * apiCaller.execute { movieApi.popular(page = 1) }
 * ```
 */
class ApiCaller @Inject constructor(
    private val errorMapper: NetworkErrorMapper,
) {

    suspend fun <T> execute(call: suspend () -> T): AppResult<T> = try {
        AppResult.Success(call())
    } catch (e: CancellationException) {
        throw e // never swallow cancellation
    } catch (e: Throwable) {
        AppResult.Failure(errorMapper.map(e))
    }
}
