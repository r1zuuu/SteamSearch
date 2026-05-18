package pl.mobilki.steambrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val repository: ReviewRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<ReviewPulseUiState>(ReviewPulseUiState.Idle)
    val uiState: StateFlow<ReviewPulseUiState> = _uiState.asStateFlow()

    private val cache = mutableMapOf<Int, Pair<ReviewPulseSummary, Long>>()
    private val cacheTtlMs = 30 * 60 * 1000L
    private var currentAppId: Int? = null

    fun loadForGame(appId: Int) {
        if (currentAppId == appId && _uiState.value is ReviewPulseUiState.Content) return

        val cached = cache[appId]
        if (cached != null && System.currentTimeMillis() - cached.second < cacheTtlMs) {
            currentAppId = appId
            _uiState.value = ReviewPulseUiState.Content(cached.first)
            return
        }

        currentAppId = appId
        _uiState.value = ReviewPulseUiState.Loading
        viewModelScope.launch {
            repository.getReviewPulse(appId)
                .onSuccess { summary ->
                    cache[appId] = summary to System.currentTimeMillis()
                    if (currentAppId == appId) {
                        _uiState.value = ReviewPulseUiState.Content(summary)
                    }
                }
                .onFailure { error ->
                    if (currentAppId == appId) {
                        _uiState.value = ReviewPulseUiState.Error(error.toPolishMessage())
                    }
                }
        }
    }

    companion object {
        fun factory(repository: ReviewRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ReviewViewModel(repository) as T
            }
    }
}
