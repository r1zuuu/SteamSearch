package pl.mobilki.steambrowser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GroqApiService(
    private val apiKey: String = BuildConfig.GROQ_API_KEY,
    private val model: String = BuildConfig.GROQ_MODEL,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun analyzeReviews(
        reviews: List<SteamReview>,
        positiveCount: Int,
        negativeCount: Int,
        positivePercent: Int,
        reviewScoreDesc: String,
        isReviewBombSuspected: Boolean
    ): Result<ReviewPulseSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val systemPrompt = """Jesteś ekspertem od analizy recenzji gier Steam. Piszesz po polsku dla polskich graczy.

ZASADY JĘZYKOWE — przestrzegaj bezwzględnie:
- Używaj poprawnej, naturalnej polszczyzny. Zero angielskich słów i kalek z angielskiego.
- Zamiast "gameplay jest engaging" → pisz "rozgrywka wciąga"
- Zamiast "content jest dobry" → pisz "zawartość gry jest bogata"
- Zamiast "gra ma dobry story" → pisz "gra ma dobrą fabułę"
- Zamiast "performance jest słaby" → pisz "gra działa słabo technicznie"
- Zamiast "cheaty są wszędzie" → pisz "gra jest zainfekowana oszustami"
- Używaj polskich słów: rozgrywka, grafika, fabuła, sterowanie, multiplayer (dopuszczalne jako rzeczownik).

ZASADY MERYTORYCZNE:
- Opieraj się WYŁĄCZNIE na dostarczonych recenzjach. Nie zmyślaj.
- Bądź konkretny i praktyczny: gracz chce wiedzieć czy warto kupić.
- Jeśli recenzji jest mało (mniej niż 5), napisz to wprost w conclusion.
- Zwróć WYŁĄCZNIE poprawny JSON. Zero markdowna, zero tekstu przed ani po JSON.""".trimIndent()

            val reviewLines = reviews.joinToString("\n") { r ->
                val prefix = if (r.votedUp) "[+]" else "[-]"
                "$prefix ${r.text.take(300)}"
            }

            val bombWarning = if (isReviewBombSuspected) {
                "UWAGA: Próbka recenzji może być skrzywiona przez review bomb lub kontrowersję. Uwzględnij to w conclusion."
            } else ""

            val userPrompt = buildString {
                append("Przeanalizuj ${reviews.size} recenzji gry Steam.\n\n")
                append("Ogólny rating gry: $reviewScoreDesc\n")
                append("Próbka: $positiveCount pozytywnych, $negativeCount negatywnych ($positivePercent% pozytywnych)\n")
                if (bombWarning.isNotEmpty()) append("$bombWarning\n")
                append("\nRecenzje ([+] = poleca, [-] = nie poleca):\n")
                append(reviewLines)
                append("\n\nZwróć WYŁĄCZNIE JSON:\n")
                append("""{"sentiment":"positive|mixed|negative","positivePercent":$positivePercent,"negativePercent":${100 - positivePercent},"commonPros":["max 4 krótkie punkty"],"commonCons":["max 4 krótkie punkty"],"redFlags":["poważne problemy lub pusta lista"],"verdict":"buy|wait|avoid|watch","conclusion":"Jedno lub dwa zdania po polsku."}""")
            }

            val requestBody = buildJsonBody(systemPrompt, userPrompt)
            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody.toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            response.use {
                val body = it.body?.string().orEmpty()
                if (!it.isSuccessful) throw IllegalStateException("Groq API zwróciło kod ${it.code}.")
                extractSummaryFromResponse(body)
            }
        }
    }

    private fun buildJsonBody(systemPrompt: String, userPrompt: String): String {
        fun String.esc() = replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
        return """{"model":"$model","messages":[{"role":"system","content":"${systemPrompt.esc()}"},{"role":"user","content":"${userPrompt.esc()}"}],"temperature":0.3,"max_tokens":1024}"""
    }

    private fun extractSummaryFromResponse(body: String): ReviewPulseSummary {
        val root = json.parseToJsonElement(body).jsonObject
        val content = root["choices"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("message")
            ?.jsonObject
            ?.get("content")
            ?.jsonPrimitive
            ?.content
            ?: throw IllegalStateException("Groq zwróciło niepoprawną strukturę.")

        val cleaned = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val parsed = json.parseToJsonElement(cleaned).jsonObject
        return ReviewPulseSummary(
            sentiment = parsed["sentiment"]?.jsonPrimitive?.content?.takeIf { it in validSentiments } ?: "mixed",
            positivePercent = parsed["positivePercent"]?.jsonPrimitive?.content?.toIntOrNull() ?: 50,
            negativePercent = parsed["negativePercent"]?.jsonPrimitive?.content?.toIntOrNull() ?: 50,
            commonPros = parsed["commonPros"]?.jsonArray?.mapNotNull { runCatching { it.jsonPrimitive.content }.getOrNull() } ?: emptyList(),
            commonCons = parsed["commonCons"]?.jsonArray?.mapNotNull { runCatching { it.jsonPrimitive.content }.getOrNull() } ?: emptyList(),
            redFlags = parsed["redFlags"]?.jsonArray?.mapNotNull { runCatching { it.jsonPrimitive.content }.getOrNull() } ?: emptyList(),
            verdict = parsed["verdict"]?.jsonPrimitive?.content?.takeIf { it in validVerdicts } ?: "watch",
            conclusion = parsed["conclusion"]?.jsonPrimitive?.content ?: ""
        )
    }

    companion object {
        private val validSentiments = setOf("positive", "mixed", "negative")
        private val validVerdicts = setOf("buy", "wait", "avoid", "watch")
    }
}
