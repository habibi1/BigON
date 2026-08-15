package com.bigon.sinema.ui

import androidx.compose.runtime.Composable
import com.bigon.core.designsystem.preview.SinemaDevicePreview
import com.bigon.core.designsystem.preview.SinemaFontScalePreview
import com.bigon.core.designsystem.preview.SinemaThemePreview
import com.bigon.core.designsystem.theme.SinemaTheme
import com.bigon.tmdb.model.Movie
import com.bigon.tmdb.model.MovieCategory
import com.bigon.sinema.ui.favorites.FavoritesScreen
import com.bigon.sinema.ui.favorites.FavoritesUiState
import com.bigon.sinema.ui.home.HomeScreen
import com.bigon.sinema.ui.home.HomeFeed
import com.bigon.sinema.ui.home.HomeUiState
import com.bigon.sinema.ui.search.SearchScreen
import com.bigon.sinema.ui.search.SearchUiState
import com.bigon.sinema.ui.settings.SettingsScreen
import com.bigon.sinema.ui.settings.SettingsUiState
import java.time.LocalDate

/**
 * Screen-level previews against the *stateless* screen composables, fed with
 * sample state — no ViewModels, no Hilt, no network. Components are proven in
 * isolation inside :core:designsystem; these check the compositions.
 */

private val sampleMovies = listOf(
    Movie(1, "Midnight Reel", "A projectionist discovers…", null, null, LocalDate.of(2026, 3, 1), 8.4, listOf("Thriller")),
    Movie(2, "The Long Take", "One continuous shot…", null, null, LocalDate.of(2025, 7, 12), 7.9, listOf("Drama")),
    Movie(3, "Silver Screen", "", null, null, null, null, listOf("Romance")),
    Movie(4, "A Very Long Movie Title That Wraps Onto Multiple Lines", "", null, null, LocalDate.of(2026, 1, 5), 9.7, listOf("Epic")),
)

// ── Home ────────────────────────────────────────────────────────────────────

@SinemaThemePreview
@Composable
private fun HomeScreenPreview() {
    SinemaTheme {
        HomeScreen(
            state = HomeUiState(feed = HomeFeed.Category(MovieCategory.Trending), movies = sampleMovies),
            onIntent = {},
        )
    }
}

@SinemaThemePreview
@Composable
private fun HomeScreenLoadingPreview() {
    SinemaTheme {
        HomeScreen(
            state = HomeUiState(feed = HomeFeed.Category(MovieCategory.Popular), isRefreshing = true),
            onIntent = {},
        )
    }
}

@SinemaDevicePreview
@Composable
private fun HomeScreenDevicePreview() {
    SinemaTheme {
        HomeScreen(state = HomeUiState(movies = sampleMovies), onIntent = {})
    }
}

@SinemaFontScalePreview
@Composable
private fun HomeScreenFontScalePreview() {
    SinemaTheme {
        HomeScreen(state = HomeUiState(movies = sampleMovies), onIntent = {})
    }
}

// ── Search ──────────────────────────────────────────────────────────────────

@SinemaThemePreview
@Composable
private fun SearchScreenPreview() {
    SinemaTheme {
        SearchScreen(
            state = SearchUiState(query = "mid", results = sampleMovies, isSearching = false),
            onIntent = {},
        )
    }
}

@SinemaThemePreview
@Composable
private fun SearchScreenEmptyPreview() {
    SinemaTheme {
        SearchScreen(
            state = SearchUiState(query = "zzzz", isSearching = false),
            onIntent = {},
        )
    }
}

// ── Favorites ───────────────────────────────────────────────────────────────

@SinemaThemePreview
@Composable
private fun FavoritesScreenPreview() {
    SinemaTheme {
        FavoritesScreen(
            state = FavoritesUiState(favorites = sampleMovies, isLoaded = true),
            onIntent = {},
        )
    }
}

@SinemaThemePreview
@Composable
private fun FavoritesScreenEmptyPreview() {
    SinemaTheme {
        FavoritesScreen(state = FavoritesUiState(isLoaded = true), onIntent = {})
    }
}

// ── Settings ────────────────────────────────────────────────────────────────

@SinemaThemePreview
@Composable
private fun SettingsScreenPreview() {
    SinemaTheme {
        SettingsScreen(
            state = SettingsUiState(themeMode = ThemeMode.Dark, cacheBytes = 134_217_728L),
            onIntent = {},
        )
    }
}

@SinemaFontScalePreview
@Composable
private fun SettingsScreenFontScalePreview() {
    SinemaTheme {
        SettingsScreen(
            state = SettingsUiState(themeMode = ThemeMode.System, cacheBytes = 42_000_000L),
            onIntent = {},
        )
    }
}
