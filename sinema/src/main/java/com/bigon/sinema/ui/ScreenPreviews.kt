package com.bigon.sinema.ui

import androidx.compose.runtime.Composable
import com.bigon.core.designsystem.preview.BigonDevicePreview
import com.bigon.core.designsystem.preview.BigonFontScalePreview
import com.bigon.core.designsystem.preview.BigonTelevisionPreview
import com.bigon.core.designsystem.preview.BigonThemePreview
import com.bigon.core.designsystem.theme.BigonTheme
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

@BigonThemePreview
@Composable
private fun HomeScreenPreview() {
    BigonTheme {
        HomeScreen(
            state = HomeUiState(feed = HomeFeed.Category(MovieCategory.Trending), movies = sampleMovies),
            onIntent = {},
        )
    }
}

@BigonThemePreview
@Composable
private fun HomeScreenLoadingPreview() {
    BigonTheme {
        HomeScreen(
            state = HomeUiState(feed = HomeFeed.Category(MovieCategory.Popular), isRefreshing = true),
            onIntent = {},
        )
    }
}

@BigonDevicePreview
@Composable
private fun HomeScreenDevicePreview() {
    BigonTheme {
        HomeScreen(state = HomeUiState(movies = sampleMovies), onIntent = {})
    }
}

@BigonFontScalePreview
@Composable
private fun HomeScreenFontScalePreview() {
    BigonTheme {
        HomeScreen(state = HomeUiState(movies = sampleMovies), onIntent = {})
    }
}

// ── Search ──────────────────────────────────────────────────────────────────

@BigonThemePreview
@Composable
private fun SearchScreenPreview() {
    BigonTheme {
        SearchScreen(
            state = SearchUiState(query = "mid", results = sampleMovies, isSearching = false),
            onIntent = {},
        )
    }
}

@BigonThemePreview
@Composable
private fun SearchScreenEmptyPreview() {
    BigonTheme {
        SearchScreen(
            state = SearchUiState(query = "zzzz", isSearching = false),
            onIntent = {},
        )
    }
}

/**
 * Home on a television: the rail instead of a bottom bar, the larger type
 * scale, overscan margins, and a grid that stops at four wide cells rather
 * than eight small ones. Every one of those comes from the configuration, so
 * this preview exercises the real switch rather than a parallel TV layout.
 */
@BigonTelevisionPreview
@Composable
private fun HomeScreenTelevisionPreview() {
    BigonTheme {
        HomeScreen(state = HomeUiState(movies = sampleMovies), onIntent = {})
    }
}

/** Search on a television — the filter row is where focus lands first. */
@BigonTelevisionPreview
@Composable
private fun SearchScreenTelevisionPreview() {
    BigonTheme {
        SearchScreen(
            state = SearchUiState(results = sampleMovies, isSearching = false),
            onIntent = {},
        )
    }
}

// ── Favorites ───────────────────────────────────────────────────────────────

@BigonThemePreview
@Composable
private fun FavoritesScreenPreview() {
    BigonTheme {
        FavoritesScreen(
            state = FavoritesUiState(favorites = sampleMovies, isLoaded = true),
            onIntent = {},
        )
    }
}

@BigonThemePreview
@Composable
private fun FavoritesScreenEmptyPreview() {
    BigonTheme {
        FavoritesScreen(state = FavoritesUiState(isLoaded = true), onIntent = {})
    }
}

// ── Settings ────────────────────────────────────────────────────────────────

@BigonThemePreview
@Composable
private fun SettingsScreenPreview() {
    BigonTheme {
        SettingsScreen(
            state = SettingsUiState(themeMode = ThemeMode.Dark, cacheBytes = 134_217_728L),
            onIntent = {},
        )
    }
}

@BigonFontScalePreview
@Composable
private fun SettingsScreenFontScalePreview() {
    BigonTheme {
        SettingsScreen(
            state = SettingsUiState(themeMode = ThemeMode.System, cacheBytes = 42_000_000L),
            onIntent = {},
        )
    }
}
