package pl.mobilki.steambrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DealsViewModel(private val repository: SteamRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<DealsUiState>(DealsUiState.Loading)
    val uiState: StateFlow<DealsUiState> = _uiState.asStateFlow()

    private var loaded = false

    fun loadIfNeeded() {
        if (!loaded) load()
    }

    fun refresh() {
        load()
    }

    private fun load() {
        _uiState.value = DealsUiState.Loading
        viewModelScope.launch {
            repository.getFeaturedDeals()
                .onSuccess { deals ->
                    loaded = true
                    _uiState.value = DealsUiState.Content(deals)
                }
                .onFailure { error ->
                    _uiState.value = DealsUiState.Error(error.toPolishMessage())
                }
        }
    }

    companion object {
        fun factory(repository: SteamRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DealsViewModel(repository) as T
            }
    }
}
