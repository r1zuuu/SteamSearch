package pl.mobilki.steambrowser

data class SteamReview(
    val id: String,
    val text: String,
    val votedUp: Boolean,
    val votesUp: Int,
    val playtimeForeverMinutes: Int?,
    val playtimeAtReviewMinutes: Int?,
    val weightedVoteScore: Float,
    val createdAtUnix: Long
)

data class ReviewQuerySummary(
    val totalPositive: Int,
    val totalNegative: Int,
    val reviewScoreDesc: String
)

data class ReviewPulseSummary(
    val sentiment: String,
    val positivePercent: Int,
    val negativePercent: Int,
    val commonPros: List<String>,
    val commonCons: List<String>,
    val redFlags: List<String>,
    val verdict: String,
    val conclusion: String
)

sealed interface ReviewPulseUiState {
    data object Idle : ReviewPulseUiState
    data object Loading : ReviewPulseUiState
    data class Content(val summary: ReviewPulseSummary) : ReviewPulseUiState
    data class Error(val message: String) : ReviewPulseUiState
}
