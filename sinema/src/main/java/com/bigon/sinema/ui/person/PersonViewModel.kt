package com.bigon.sinema.ui.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bigon.core.common.AppResult
import com.bigon.core.model.PersonDetail
import com.bigon.core.tracker.AnalyticsEvent
import com.bigon.core.tracker.AnalyticsTracker
import com.bigon.core.ui.UiText
import com.bigon.core.ui.toUiText
import com.bigon.domain.movie.GetPersonUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PersonUiState(
    val personId: Long,
    val person: PersonDetail? = null,
    val isLoading: Boolean = true,
    val error: UiText? = null,
) {
    val showSkeleton: Boolean get() = isLoading && person == null
}

sealed interface PersonIntent {
    data object Retry : PersonIntent
    data class MovieClicked(val movieId: Long) : PersonIntent
}

/**
 * Person is network-only, unlike movie detail.
 *
 * The two are reached differently: a film is opened repeatedly and browsed
 * offline, whereas a person is a lookup from a cast card. Caching 92 KB of
 * filmography per tapped face would cost more storage than it saves requests.
 */
@HiltViewModel(assistedFactory = PersonViewModel.Factory::class)
class PersonViewModel @AssistedInject constructor(
    @Assisted private val personId: Long,
    private val getPerson: GetPersonUseCase,
    private val tracker: AnalyticsTracker,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(personId: Long): PersonViewModel
    }

    private val _state = MutableStateFlow(PersonUiState(personId = personId))
    val state: StateFlow<PersonUiState> = _state.asStateFlow()

    init {
        tracker.track(AnalyticsEvent.ScreenView("person"))
        load()
    }

    fun onIntent(intent: PersonIntent) {
        when (intent) {
            PersonIntent.Retry -> load()
            is PersonIntent.MovieClicked ->
                tracker.track(AnalyticsEvent.MovieOpened(intent.movieId, source = "filmography"))
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = getPerson(personId)) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoading = false, person = result.value, error = null)
                }
                is AppResult.Failure -> _state.update {
                    it.copy(isLoading = false, error = result.error.toUiText())
                }
            }
        }
    }
}
