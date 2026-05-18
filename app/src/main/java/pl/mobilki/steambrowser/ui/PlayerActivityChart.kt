package pl.mobilki.steambrowser.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.mobilki.steambrowser.PlayerActivityPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private enum class TimeRange(val label: String, val days: Int) {
    WEEK("7 dni", 7),
    MONTH("30 dni", 30),
    HALF_YEAR("6 miesięcy", 180)
}

@Composable
fun PlayerActivityChart(appId: Int, currentPlayers: Int?) {
    val allData = remember(appId, currentPlayers) { generateMockPlayerActivity(appId, currentPlayers) }
    var selectedRange by remember { mutableStateOf(TimeRange.WEEK) }
    var tooltipIndex by remember { mutableIntStateOf(-1) }

    val visibleData = remember(allData, selectedRange) { allData.takeLast(selectedRange.days) }
    val stats = remember(visibleData) { computeStats(visibleData) }

    Column {
        Text(
            text = "Historia aktywności graczy",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeRange.entries.forEach { range ->
                FilterChip(
                    selected = selectedRange == range,
                    onClick = { selectedRange = range; tooltipIndex = -1 },
                    label = { Text(range.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            LineChart(
                data = visibleData,
                tooltipIndex = tooltipIndex,
                onTouch = { idx -> tooltipIndex = idx },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatRow("Najwyższa aktywność", formatPlayers(stats.max))
                StatRow("Najniższa aktywność", formatPlayers(stats.min))
                StatRow("Średnia aktywność", formatPlayers(stats.avg))
                Text(
                    text = stats.trendText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LineChart(
    data: List<PlayerActivityPoint>,
    tooltipIndex: Int,
    onTouch: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val maxPlayers = data.maxOf { it.players }.coerceAtLeast(1)
    val minPlayers = data.minOf { it.players }
    val range = (maxPlayers - minPlayers).coerceAtLeast(1)

    var canvasWidth by remember { mutableStateOf(0f) }

    val paddingTop = 12f
    val paddingBottom = with(density) { 24.dp.toPx() }
    val paddingHorizontal = 8f

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        if (data.isEmpty() || canvasWidth == 0f) return@detectTapGestures
                        val step = (canvasWidth - 2 * paddingHorizontal) / (data.size - 1).coerceAtLeast(1)
                        val idx = ((offset.x - paddingHorizontal) / step).roundToInt().coerceIn(0, data.lastIndex)
                        onTouch(idx)
                    }
                }
        ) {
            canvasWidth = size.width

            val chartHeight = size.height - paddingTop - paddingBottom
            val chartWidth = size.width - 2 * paddingHorizontal
            val step = if (data.size > 1) chartWidth / (data.size - 1) else chartWidth

            fun xOf(i: Int) = paddingHorizontal + i * step
            fun yOf(players: Int) = paddingTop + chartHeight * (1f - (players - minPlayers).toFloat() / range)

            // grid lines
            repeat(3) { i ->
                val y = paddingTop + chartHeight * i / 2f
                drawLine(
                    color = surfaceVariant.copy(alpha = 0.5f),
                    start = Offset(paddingHorizontal, y),
                    end = Offset(size.width - paddingHorizontal, y),
                    strokeWidth = 1f
                )
            }

            if (data.size > 1) {
                // fill path
                val fillPath = Path().apply {
                    moveTo(xOf(0), yOf(data[0].players))
                    for (i in 1..data.lastIndex) {
                        val cx = (xOf(i - 1) + xOf(i)) / 2
                        cubicTo(cx, yOf(data[i - 1].players), cx, yOf(data[i].players), xOf(i), yOf(data[i].players))
                    }
                    lineTo(xOf(data.lastIndex), size.height - paddingBottom)
                    lineTo(xOf(0), size.height - paddingBottom)
                    close()
                }
                drawPath(fillPath, color = primaryColor.copy(alpha = 0.12f))

                // line path
                val linePath = Path().apply {
                    moveTo(xOf(0), yOf(data[0].players))
                    for (i in 1..data.lastIndex) {
                        val cx = (xOf(i - 1) + xOf(i)) / 2
                        cubicTo(cx, yOf(data[i - 1].players), cx, yOf(data[i].players), xOf(i), yOf(data[i].players))
                    }
                }
                drawPath(linePath, color = primaryColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
            }

            // X axis labels
            val labelStyle = TextStyle(color = labelColor, fontSize = 9.sp)
            val fmt = DateTimeFormatter.ofPattern("dd.MM")
            val labelCount = minOf(data.size, if (data.size <= 7) data.size else 5)
            val labelIndices = if (data.size <= labelCount) data.indices.toList()
            else (0 until labelCount).map { i ->
                (i * (data.size - 1) / (labelCount - 1).coerceAtLeast(1)).coerceIn(0, data.lastIndex)
            }

            labelIndices.forEach { i ->
                val label = runCatching { LocalDate.parse(data[i].date).format(fmt) }
                    .getOrDefault(data[i].date.takeLast(5))
                val measured = textMeasurer.measure(label, style = labelStyle)
                val tx = (xOf(i) - measured.size.width / 2f).coerceIn(0f, size.width - measured.size.width)
                val ty = size.height - measured.size.height
                drawText(textMeasurer, label, topLeft = Offset(tx, ty), style = labelStyle)
            }

            // tooltip vertical line + dot
            if (tooltipIndex in data.indices) {
                val tx = xOf(tooltipIndex)
                val ty = yOf(data[tooltipIndex].players)
                drawLine(
                    color = primaryColor.copy(alpha = 0.4f),
                    start = Offset(tx, paddingTop),
                    end = Offset(tx, size.height - paddingBottom),
                    strokeWidth = 1.5f
                )
                drawCircle(color = primaryColor, radius = 5f, center = Offset(tx, ty))
                drawCircle(color = Color.White, radius = 2.5f, center = Offset(tx, ty))
            }
        }

        // tooltip bubble
        if (tooltipIndex in data.indices && canvasWidth > 0f) {
            val chartWidth = canvasWidth - 2 * paddingHorizontal
            val step = if (data.size > 1) chartWidth / (data.size - 1) else chartWidth
            val rawX = paddingHorizontal + tooltipIndex * step
            val tooltipWidthDp = 96.dp
            val tooltipWidthPx = with(density) { tooltipWidthDp.toPx() }
            val clampedX = rawX.coerceIn(tooltipWidthPx / 2, canvasWidth - tooltipWidthPx / 2)
            val offsetX = with(density) { (clampedX - tooltipWidthPx / 2).toDp() }
            val fmt = DateTimeFormatter.ofPattern("dd.MM")

            Surface(
                modifier = Modifier
                    .width(tooltipWidthDp)
                    .offset { IntOffset(offsetX.roundToPx(), 4) }
                    .wrapContentSize(),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatPlayers(data[tooltipIndex].players),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    val dateLabel = runCatching { LocalDate.parse(data[tooltipIndex].date).format(fmt) }
                        .getOrDefault(data[tooltipIndex].date.takeLast(5))
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private data class ChartStats(val min: Int, val max: Int, val avg: Int, val trendText: String)

private fun computeStats(data: List<PlayerActivityPoint>): ChartStats {
    if (data.isEmpty()) return ChartStats(0, 0, 0, "Brak danych.")
    val min = data.minOf { it.players }
    val max = data.maxOf { it.players }
    val avg = data.map { it.players }.average().roundToInt()
    val first = data.first().players.toFloat()
    val last = data.last().players.toFloat()
    val trendText = when {
        first == 0f -> "Brak danych o trendzie."
        (last - first) / first > 0.05f -> "Aktywność graczy rośnie w tym okresie."
        (last - first) / first < -0.05f -> "Aktywność graczy spada w tym okresie."
        else -> "Aktywność graczy jest względnie stabilna."
    }
    return ChartStats(min, max, avg, trendText)
}

fun generateMockPlayerActivity(appId: Int, currentPlayers: Int?): List<PlayerActivityPoint> {
    val baseline = (currentPlayers ?: 50_000).coerceAtLeast(100)
    val rng = java.util.Random(appId.toLong())
    val today = LocalDate.now()
    return (179 downTo 0).map { daysAgo ->
        val date = today.minusDays(daysAgo.toLong())
        val dayOfWeek = date.dayOfWeek.value
        val weekendBoost = if (dayOfWeek >= 6) 1.15f else 1f
        val noise = 1f + (rng.nextFloat() - 0.5f) * 0.4f
        val trend = 1f + (daysAgo / 180f) * (if (rng.nextBoolean()) 0.2f else -0.2f)
        val players = (baseline * noise * weekendBoost * trend).roundToInt().coerceAtLeast(1)
        PlayerActivityPoint(date = date.toString(), players = players)
    }
}

private fun formatPlayers(players: Int): String = when {
    players >= 1_000_000 -> "${"%.1f".format(players / 1_000_000f)}M graczy"
    players >= 1_000 -> "${"%.1f".format(players / 1_000f)}K graczy"
    else -> "$players graczy"
}
