package com.ledgeryaar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgeryaar.ui.theme.AppColors
import com.ledgeryaar.util.compactAmount
import com.ledgeryaar.util.formatAmount

val ChartPalette = listOf(
    Color(0xFF1F5C4D), Color(0xFF8B1E1E), Color(0xFFB08D3F), Color(0xFF2E4159),
    Color(0xFF6B8E7F), Color(0xFFC2704F), Color(0xFF7A5C99), Color(0xFF4F8FBF), Color(0xFF9B8F7F)
)

/** نمودار میله‌ای دوتایی (درآمد سبز / هزینه قرمز) برای چند ماه؛ جهت چارت همیشه چپ‌به‌راست است */
@Composable
fun DualBarChart(
    labels: List<String>,
    income: List<Double>,
    expense: List<Double>,
    modifier: Modifier = Modifier
) {
    val axisColor = MaterialTheme.colorScheme.outline
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier.fillMaxWidth()) {
            Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                val n = labels.size
                if (n == 0) return@Canvas
                val maxValue = maxOf(income.maxOrNull() ?: 0.0, expense.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
                val slot = size.width / n
                val barWidth = slot * 0.32f
                drawLine(axisColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
                for (i in 0 until n) {
                    val x = i * slot + slot * 0.14f
                    val h1 = ((income.getOrElse(i) { 0.0 } / maxValue).toFloat() * (size.height - 4f)).coerceAtLeast(0f)
                    val h2 = ((expense.getOrElse(i) { 0.0 } / maxValue).toFloat() * (size.height - 4f)).coerceAtLeast(0f)
                    drawRect(AppColors.LedgerGreen, Offset(x, size.height - h1), Size(barWidth, h1))
                    drawRect(AppColors.StampRed, Offset(x + barWidth + 2f, size.height - h2), Size(barWidth, h2))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                labels.forEach { l ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(l, fontSize = 9.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** نمودار دونات + راهنما (نام، مبلغ، درصد) */
@Composable
fun DonutWithLegend(
    slices: List<Pair<String, Double>>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val total = slices.map { it.second }.sum()
    if (total <= 0.0) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(150.dp)) {
                    val stroke = 28f
                    val diameter = size.minDimension - stroke
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    var start = -90f
                    slices.forEachIndexed { i, s ->
                        val sweep = (s.second / total * 360.0).toFloat()
                        drawArc(
                            color = ChartPalette[i % ChartPalette.size],
                            startAngle = start, sweepAngle = sweep, useCenter = false,
                            topLeft = topLeft, size = Size(diameter, diameter),
                            style = Stroke(width = stroke)
                        )
                        start += sweep
                    }
                }
            }
        }
        slices.forEachIndexed { i, s ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(ChartPalette[i % ChartPalette.size], CircleShape))
                Text(s.first, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), maxLines = 1)
                Text(formatAmount(s.second) + " " + currency, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "  " + (s.second / total * 100).toInt() + "%",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** میلهٔ افقی نسبی برای مقایسه‌ی دسته‌ها */
@Composable
fun ShareBar(label: String, value: Double, max: Double, valueText: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(valueText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { if (max > 0) (value / max).toFloat().coerceIn(0f, 1f) else 0f },
            color = color,
            modifier = Modifier.fillMaxWidth().padding(top = 3.dp)
        )
    }
}

/** خلاصهٔ کوتاه یک مبلغ برای چارت‌ها */
fun chartAmount(value: Double, fa: Boolean): String = compactAmount(value, fa)
