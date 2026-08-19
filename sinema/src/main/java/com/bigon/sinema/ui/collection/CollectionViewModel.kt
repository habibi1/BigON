package com.bigon.sinema.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigon.core.common.AppResult
import com.bigon.tmdb.model.MovieCollection
import com.bigon.core.tracker.AnalyticsEvent
import com.bigon.core.tracker.AnalyticsTracker
import com.bigon.core.ui.UiText
import com.bigon.core.ui.toUiText
import com.bigon.tmdb.domain.movie.GetCollectionUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CollectionUiState(
    val collectionId: Long,
    val collection: MovieCollection? = null,
    val isLoading: Boolean = true,
    val error: UiText? = null,
)

@HiltViewModel(assistedFactory = CollectionViewModel.Factory::class)
class CollectionViewModel @AssistedInject constructor(
    @Assisted private val collectionId: Long,
    private val getCollection: GetCollectionUseCase,
    tracker: AnalyticsTracker,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(collectionId: Long): CollectionViewModel
    }

    private val _state = MutableStateFlow(CollectionUiState(collectionId = collectionId))
    val state: StateFlow<CollectionUiState> = _state.asStateFlow()

    init {
        tracker.track(AnalyticsEvent.ScreenView("collection"))
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = getCollection(collectionId)) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoading = false, collection = result.value, error = null)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.error.toUiText())
                }
            }
        }
    }
}
