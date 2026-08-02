package com.bigon.core.ui

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.runtime.snapshotFlow

/**
 * Fires [onLoadMore] when scrolling brings the user within [buffer] items of
 * the end of a lazy grid. Derived-state + snapshotFlow keeps this from
 * recomposing per frame; dedupe against repeat firing is the caller's job
 * (an `isAppending` guard in the ViewModel).
 */
@Composable
fun LazyGridState.LoadMoreEffect(
    buffer: Int = 6,
    enabled: Boolean = true,
    onLoadMore: () -> Unit,
) {
    val shouldLoad by remember(this, buffer) {
        derivedStateOf {
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            val total = layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 1 - buffer
        }
    }

    LaunchedEffect(this, enabled) {
        if (!enabled) return@LaunchedEffect
        snapshotFlow { shouldLoad }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }
}
