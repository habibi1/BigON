package com.bigon.sinema.ui.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigon.core.common.AppResult
import com.bigon.core.model.TvDetail
import com.bigon.core.tracker.AnalyticsEvent
import com.bigon.core.tracker.AnalyticsTracker
import com.bigon.core.ui.UiText
import com.bigon.core.ui.toUiText
import com.bigon.domain.movie.GetTvDetailUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TvDetailUiState(
    val tvId: Long,
    val show: TvDetail? = null,
    val isLoading: Boolean = true,
    val error: UiText? = null,
) {
    val showSkeleton: Boolean get() = isLoading && show == null
}

@HiltViewModel(assistedFactory = TvDetailViewModel.Factory::class)
class TvDetailViewModel @AssistedInject constructor(
    @Assisted private val tvId: Long,
    private val getTvDetail: GetTvDetailUseCase,
    tracker: AnalyticsTracker,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(tvId: Long): TvDetailViewModel
    }

    private val _state = MutableStateFlow(TvDetailUiState(tvId = tvId))
    val state: StateFlow<TvDetailUiState> = _state.asStateFlow()

    init {
        tracker.track(AnalyticsEvent.ScreenView("tv_detail"))
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = getTvDetail(tvId)) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoading = false, show = result.value, error = null)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.error.toUiText())
                }
            }
        }
    }
}
