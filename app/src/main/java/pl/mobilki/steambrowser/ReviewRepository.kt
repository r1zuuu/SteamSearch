package pl.mobilki.steambrowser

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.roundToInt

class ReviewRepository(
    private val api: SteamApiService,
    private val groq: GroqApiService
) {
    suspend fun getReviewPulse(appId: Int): Result<ReviewPulseSummary> = runCatching {
        val (allJson, recentJson) = coroutineScope {
            val allDeferred = async { api.getSteamReviews(appId, "all", 100) }
            val recentDeferred = async { api.getSteamReviews(appId, "recent", 30) }
            allDeferred.await() to recentDeferred.await()
        }

        val querySummary = parseQuerySummary(allJson)
        val allReviews = parseReviews(allJson)
        val recentReviews = parseReviews(recentJson)
        val merged = (allReviews + recentReviews).distinctBy { it.id }

        val filtered = merged.filter { it.text.length >= 20 }

        if (filtered.isEmpty()) {
            return@runCatching buildFallback(querySummary)
        }

        val scored = filtered
            .map { it to scoreReview(it) }
            .sortedByDescending { it.second }

        val positivePool = scored.filter { it.first.votedUp }.map { it.first }
        val negativePool = scored.filter { !it.first.votedUp }.map { it.first }

        val totalAll = (querySummary.totalPositive + querySummary.totalNegative).coerceAtLeast(1)
        val overallPositiveRatio = querySummary.totalPositive.toFloat() / totalAll
        val positiveQuota = (25 * overallPositiveRatio).roundToInt().coerceIn(5, 20)
        val negativeQuota = 25 - positiveQuota

        val selected = positivePool.take(positiveQuota) + negativePool.take(negativeQuota)

        if (selected.isEmpty()) {
            return@runCatching buildFallback(querySummary)
        }

        val sampleTotal = selected.size
        val samplePositiveCount = selected.count { it.votedUp }
        val sampleNegativeCount = sampleTotal - samplePositiveCount
        val samplePositivePct = samplePositiveCount * 100 / sampleTotal
        val overallPositivePct = querySummary.totalPositive * 100 / totalAll
        val isReviewBombSuspected = abs(samplePositivePct - overallPositivePct) > 40

        groq.analyzeReviews(
            reviews = selected,
            positiveCount = samplePositiveCount,
            negativeCount = sampleNegativeCount,
            positivePercent = samplePositivePct,
            reviewScoreDesc = querySummary.reviewScoreDesc,
            isReviewBombSuspected = isReviewBombSuspected
        ).getOrElse { buildFallback(querySummary) }
    }

    private fun parseQuerySummary(json: JsonObject): ReviewQuerySummary {
        val qs = (json["query_summary"] as? JsonObject) ?: return ReviewQuerySummary(0, 0, "")
        return ReviewQuerySummary(
            totalPositive = (qs["total_positive"] as? JsonPrimitive)?.intOrNull ?: 0,
            totalNegative = (qs["total_negative"] as? JsonPrimitive)?.intOrNull ?: 0,
            reviewScoreDesc = (qs["review_score_desc"] as? JsonPrimitive)?.contentOrNull ?: ""
        )
    }

    private fun parseReviews(json: JsonObject): List<SteamReview> {
        val reviewsArray = (json["reviews"] as? JsonArray) ?: return emptyList()
        return reviewsArray.mapNotNull { element ->
            runCatching {
                val obj = element.jsonObject
                val id = obj["recommendationid"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
                val text = obj["review"]?.jsonPrimitive?.contentOrNull?.trim() ?: return@runCatching null
                val votedUp = obj["voted_up"]?.jsonPrimitive?.contentOrNull == "true"
                val votesUp = obj["votes_up"]?.jsonPrimitive?.intOrNull ?: 0
                val weightedScore = obj["weighted_vote_score"]?.jsonPrimitive?.floatOrNull ?: 0f
                val createdAt = obj["timestamp_created"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
                val author = obj["author"]?.jsonObject
                val playtimeForever = author?.get("playtime_forever")?.jsonPrimitive?.intOrNull
                val playtimeAtReview = author?.get("playtime_at_review")?.jsonPrimitive?.intOrNull
                SteamReview(
                    id = id,
                    text = text,
                    votedUp = votedUp,
                    votesUp = votesUp,
                    playtimeForeverMinutes = playtimeForever,
                    playtimeAtReviewMinutes = playtimeAtReview,
                    weightedVoteScore = weightedScore,
                    createdAtUnix = createdAt
                )
            }.getOrNull()
        }
    }

    private fun scoreReview(review: SteamReview): Float {
        val qualityScore = review.weightedVoteScore * 5f
        val popularityScore = log10(review.votesUp.toFloat() + 1f) * 2f
        val credibility = when {
            (review.playtimeForeverMinutes ?: 0) >= 200 * 60 -> 1f
            (review.playtimeForeverMinutes ?: 0) >= 50 * 60  -> 0.5f
            (review.playtimeForeverMinutes ?: 0) < 5 * 60    -> -1f
            else                                              -> 0f
        }
        val nowSeconds = System.currentTimeMillis() / 1000L
        val daysSince = (nowSeconds - review.createdAtUnix) / 86400L
        val recencyBonus = when {
            daysSince < 30  -> 2f
            daysSince < 90  -> 1f
            else            -> 0f
        }
        return qualityScore + popularityScore + credibility + recencyBonus
    }

    private fun buildFallback(querySummary: ReviewQuerySummary): ReviewPulseSummary {
        val total = (querySummary.totalPositive + querySummary.totalNegative).coerceAtLeast(1)
        val pct = querySummary.totalPositive * 100 / total
        val (sentiment, verdict) = when {
            pct >= 75 -> "positive" to "buy"
            pct >= 50 -> "mixed"    to "watch"
            pct >= 30 -> "mixed"    to "wait"
            else      -> "negative" to "avoid"
        }
        return ReviewPulseSummary(
            sentiment = sentiment,
            positivePercent = pct,
            negativePercent = 100 - pct,
            commonPros = emptyList(),
            commonCons = emptyList(),
            redFlags = emptyList(),
            verdict = verdict,
            conclusion = "Analiza oparta na ogólnym ratingu gry (${querySummary.reviewScoreDesc}). Szczegółowa analiza niedostępna."
        )
    }
}
