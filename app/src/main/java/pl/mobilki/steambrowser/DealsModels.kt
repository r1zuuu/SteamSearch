package pl.mobilki.steambrowser

data class GamePrice(
    val currency: String,
    val initialCents: Int,
    val finalCents: Int,
    val discountPercent: Int,
    val initialFormatted: String,
    val finalFormatted: String
)

data class DealItem(
    val appId: Int,
    val name: String,
    val price: GamePrice?
)

sealed interface DealsUiState {
    data object Loading : DealsUiState
    data class Content(
        val deals: List<DealItem>,
        val isRefreshing: Boolean = false
    ) : DealsUiState
    data class Error(val message: String) : DealsUiState
}
