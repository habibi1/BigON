package com.bigon.data.movie

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * TMDB endpoint contract. Lives beside its DTOs in :data because Retrofit
 * services are wire-shaped, not domain-shaped — each feature owns its own.
 */
interface MovieApi {

    @GET("trending/movie/day")
    suspend fun trending(@Query("page") page: Int = 1): MovieListResponse

    @GET("movie/popular")
    suspend fun popular(@Query("page") page: Int = 1): MovieListResponse

    @GET("movie/now_playing")
    suspend fun nowPlaying(@Query("page") page: Int = 1): MovieListResponse

    @GET("movie/top_rated")
    suspend fun topRated(@Query("page") page: Int = 1): MovieListResponse

    @GET("movie/upcoming")
    suspend fun upcoming(@Query("page") page: Int = 1): MovieListResponse

    /**
     * Free-text title search. TMDB's search endpoint takes no genre filter —
     * genre browsing is [discover]'s job.
     */
    @GET("search/movie")
    suspend fun search(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
    ): MovieListResponse

    /** Filtered browsing; drives the Search tab's idle state and genre chips. */
    @GET("discover/movie")
    suspend fun discover(
        @Query("with_genres") withGenres: String? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1,
    ): MovieListResponse

    /** Cast and trailer are appended so detail costs one request, not three. */
    @GET("movie/{id}")
    suspend fun detail(
        @Path("id") id: Long,
        @Query("append_to_response") append: String = "credits,videos",
    ): MovieDetailResponse

    @GET("genre/movie/list")
    suspend fun genres(): GenreListResponse
}
