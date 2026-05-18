package pl.mobilki.steambrowser.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.mobilki.steambrowser.ReviewPulseSummary
import pl.mobilki.steambrowser.ReviewPulseUiState
import pl.mobilki.steambrowser.ReviewViewModel

@Composable
fun ReviewPulseCard(viewModel: ReviewViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        ReviewPulseUiState.Idle -> Unit
        ReviewPulseUiState.Loading -> ReviewPulseShimmer()
        is ReviewPulseUiState.Error -> ReviewPulseError(message = state.message)
        is ReviewPulseUiState.Content -> ReviewPulseContent(summary = state.summary)
    }
}

@Composable
private fun ReviewPulseContent(summary: ReviewPulseSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AutoGraph,
                    contentDescription = null,
                    tint = Color(0xFF00D4FF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Puls ostatnich recenzji",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF152234), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${summary.positivePercent}%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = sentimentColor(summary.sentiment)
                        )
                        Text(
                            text = "pozytywnych",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    VerdictBadge(verdict = summary.verdict)
                    SentimentBadge(sentiment = summary.sentiment)
                }
            }

            if (summary.commonPros.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                PulseSection(
                    title = "Gracze chwalą",
                    icon = Icons.Filled.ThumbUp,
                    iconTint = Color(0xFF00D4FF),
                    items = summary.commonPros
                )
            }

            if (summary.commonCons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                PulseSection(
                    title = "Gracze narzekają",
                    icon = Icons.Filled.ThumbDown,
                    iconTint = Color(0xFFFFC107),
                    items = summary.commonCons
                )
            }

            if (summary.redFlags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                PulseSection(
                    title = "Czerwone flagi",
                    icon = Icons.Filled.Flag,
                    iconTint = Color(0xFFFF6B6B),
                    items = summary.redFlags
                )
            }

            if (summary.conclusion.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF152234), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = summary.conclusion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun VerdictBadge(verdict: String) {
    val (label, bg, fg) = when (verdict) {
        "buy"   -> Triple("Kup teraz",  Color(0xFF00D4FF), Color(0xFF001B22))
        "wait"  -> Triple("Poczekaj",   Color(0xFFFFC107), Color(0xFF1A1200))
        "avoid" -> Triple("Unikaj",     Color(0xFFFF6B6B), Color(0xFF2D0000))
        else    -> Triple("Obserwuj",   Color(0xFF7A9AB5), Color(0xFF001B22))
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
private fun SentimentBadge(sentiment: String) {
    val (label, icon, color) = when (sentiment) {
        "positive" -> Triple("Pozytywny",  Icons.Filled.SentimentSatisfied,      Color(0xFF00D4FF))
        "negative" -> Triple("Negatywny",  Icons.Filled.SentimentVeryDissatisfied, Color(0xFFFF6B6B))
        else       -> Triple("Mieszany",   Icons.Filled.SentimentDissatisfied,    Color(0xFFFFC107))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun PulseSection(title: String, icon: ImageVector, iconTint: Color, items: List<String>) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(13.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        items.forEach { item ->
            Row(modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)) {
                Text(
                    text = "• ",
                    style = MaterialTheme.typography.bodySmall,
                    color = iconTint
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ReviewPulseShimmer() {
    val transition = rememberInfiniteTransition(label = "review_shimmer")
    val x by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )
    val brush = Brush.linearGradient(
        colors = listOf(Color(0xFF0E1825), Color(0xFF152234), Color(0xFF0E1825)),
        start = Offset(x, 0f),
        end = Offset(x + 300f, 0f)
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.fillMaxWidth(0.5f).height(16.dp).background(brush, RoundedCornerShape(4.dp)))
            Box(modifier = Modifier.fillMaxWidth(0.3f).height(48.dp).background(brush, RoundedCornerShape(8.dp)))
            Box(modifier = Modifier.fillMaxWidth().height(12.dp).background(brush, RoundedCornerShape(4.dp)))
            Box(modifier = Modifier.fillMaxWidth(0.8f).height(12.dp).background(brush, RoundedCornerShape(4.dp)))
            Box(modifier = Modifier.fillMaxWidth().height(12.dp).background(brush, RoundedCornerShape(4.dp)))
            Box(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp).background(brush, RoundedCornerShape(4.dp)))
        }
    }
}

@Composable
private fun ReviewPulseError(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = "Puls recenzji niedostępny",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun sentimentColor(sentiment: String): Color = when (sentiment) {
    "positive" -> Color(0xFF00D4FF)
    "negative" -> Color(0xFFFF6B6B)
    else       -> Color(0xFFFFC107)
}
