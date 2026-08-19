package com.bigon.core.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bigon.core.designsystem.theme.BigonTheme

/**
 * Every preview renders inside this: the real [BigonTheme] over the real
 * ground colour, so what the canvas shows is what the app draws. Dark/light is
 * driven by the preview's `uiMode`, which [BigonTheme] picks up through
 * `isSystemInDarkTheme()`.
 */
@Composable
fun BigonPreviewSurface(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    BigonTheme {
        Column(
            modifier = modifier
                .background(BigonTheme.colors.background)
                .padding(padding),
        ) {
            content()
        }
    }
}

// ── Sample content ──────────────────────────────────────────────────────────
// Realistic values, including the awkward ones: missing ratings, long titles,
// and non-Latin text. Previews that only ever show tidy data hide real bugs.

data class PreviewMovie(
    val title: String,
    val meta: String,
    val rating: Double?,
)

object BigonSampleData {
    val movies = listOf(
        PreviewMovie("Midnight Reel", "2026 · Thriller", 8.4),
        PreviewMovie("The Long Take", "2025 · Drama", 7.9),
        PreviewMovie("Silver Screen", "2024 · Romance", 7.2),
        PreviewMovie("Final Cut", "2026 · Horror", 8.9),
    )

    val cast = listOf(
        "Zendaya" to "Chani",
        "T. Chalamet" to "Paul Atreides",
        "R. Ferguson" to "Lady Jessica",
    )

    val genres = listOf("All", "Sci-Fi", "Drama", "Romance", "Horror", "Comedy")
}

/**
 * Feeds [BigonMovieCardStatesPreview] the edge cases a movie card must survive:
 * a normal entry, no rating, a title that wraps past two lines, and a
 * non-Latin title.
 */
class MoviePreviewParameterProvider : PreviewParameterProvider<PreviewMovie> {
    override val values = sequenceOf(
        PreviewMovie("Midnight Reel", "2026 · Thriller", 8.4),
        PreviewMovie("Frame by Frame", "2023 · Documentary", null),
        PreviewMovie(
            "A Very Long Movie Title That Will Certainly Wrap Onto Several Lines",
            "2026 · Epic",
            9.7,
        ),
        PreviewMovie("千と千尋の神隠し", "2001 · Animation", 8.6),
    )
}
