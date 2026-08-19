package com.bigon.tmdb.data.movie

import app.cash.turbine.test
import com.bigon.core.common.DispatcherProvider
import com.bigon.tmdb.database.FavoriteDao
import com.bigon.tmdb.database.FavoriteEntity
import com.bigon.tmdb.model.Movie
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFavoritesRepositoryTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private class FakeFavoriteDao : FavoriteDao {
        val rows = MutableStateFlow<Map<Long, FavoriteEntity>>(emptyMap())
        override fun observeAll(): Flow<List<FavoriteEntity>> =
            rows.map { it.values.sortedByDescending(FavoriteEntity::addedAt) }
        override fun observeIsFavorite(movieId: Long): Flow<Boolean> =
            rows.map { movieId in it }
        override suspend fun upsert(favorite: FavoriteEntity) {
            rows.value = rows.value + (favorite.id to favorite)
        }
        override suspend fun delete(movieId: Long) {
            rows.value = rows.value - movieId
        }
    }

    private fun repository(dao: FakeFavoriteDao, clock: () -> Long = { 0L }) =
        DefaultFavoritesRepository(
            favoriteDao = dao,
            dispatchers = object : DispatcherProvider {
                override val io: CoroutineDispatcher = dispatcher
                override val default: CoroutineDispatcher = dispatcher
                override val main: CoroutineDispatcher = StandardTestDispatcher()
            },
        ).apply { this.clock = clock }

    private fun movie(id: Long, title: String = "Movie $id") = Movie(
        id = id, title = title, overview = "…",
        posterUrl = "https://image.tmdb.org/t/p/w342/p$id.jpg", backdropUrl = null,
        releaseDate = LocalDate.of(2026, 1, 1), voteAverage = 8.0,
        genres = listOf("Science Fiction", "Action"),
    )

    @Test
    fun `favouriting stores a snapshot that round-trips intact`() = runTest(dispatcher) {
        val repo = repository(FakeFavoriteDao())

        repo.setFavorite(movie(1, "Dune"), favorite = true)

        repo.observeFavorites().test {
            val stored = awaitItem().single()
            assertEquals("Dune", stored.title)
            assertEquals(listOf("Science Fiction", "Action"), stored.genres)
            assertEquals(LocalDate.of(2026, 1, 1), stored.releaseDate)
            assertEquals(8.0, stored.voteAverage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unfavouriting removes and isFavorite tracks both directions`() = runTest(dispatcher) {
        val dao = FakeFavoriteDao()
        val repo = repository(dao)

        repo.observeIsFavorite(1).test {
            assertFalse(awaitItem())
            repo.setFavorite(movie(1), favorite = true)
            assertTrue(awaitItem())
            repo.setFavorite(movie(1), favorite = false)
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun `favourites are ordered newest first`() = runTest(dispatcher) {
        var now = 0L
        val repo = repository(FakeFavoriteDao(), clock = { now })

        repo.setFavorite(movie(1, "First"), true)
        now = 10
        repo.setFavorite(movie(2, "Second"), true)

        repo.observeFavorites().test {
            assertEquals(listOf("Second", "First"), awaitItem().map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `corrupt stored date degrades to null instead of crashing`() = runTest(dispatcher) {
        val dao = FakeFavoriteDao()
        dao.rows.value = mapOf(
            9L to FavoriteEntity(9, "Broken", "", null, null, "not-a-date", null, emptyList(), 0),
        )
        repository(dao).observeFavorites().test {
            assertNull(awaitItem().single().releaseDate)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
