package com.ledgeryaar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/**
 * لوگوی دفتر یار — یک دفتر کل با مهر موم قرمز، دقیقاً هم‌راستا با آیکون اپ.
 * با Canvas کشیده می‌شود، پس نیازی به asset یا resource pipeline جداگانه ندارد
 * و روی اندروید و iOS یکسان نمایش داده می‌شود.
 */
@Composable
fun LedgerLogo(modifier: Modifier = Modifier, size: Dp) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 420f // مقیاس نسبت به مختصات طراحی (تقریبا 420x420 مفید)
        val ox = (this.size.width - 420f * s) / 2f
        val oy = (this.size.height - 420f * s) / 2f
        fun px(x: Float) = ox + x * s
        fun py(y: Float) = oy + y * s

        // جلد کتاب
        drawRoundRect(
            color = Color(0xFFB08D3F),
            topLeft = Offset(px(6f), py(4f)),
            size = Size(272f * s, 300f * s),
            cornerRadius = CornerRadius(14f * s, 14f * s)
        )
        // صفحات
        drawRoundRect(
            color = Color(0xFFF4EFE4),
            topLeft = Offset(px(20f), py(18f)),
            size = Size(244f * s, 272f * s),
            cornerRadius = CornerRadius(8f * s, 8f * s)
        )
        // شیرازه
        drawLine(
            color = Color(0xFFC2B59C),
            start = Offset(px(136f), py(18f)),
            end = Offset(px(136f), py(290f)),
            strokeWidth = 3f * s
        )
        // خط‌های دفتر — سمت راست
        val lineY = listOf(62f, 90f, 118f)
        lineY.forEachIndexed { idx, y ->
            drawLine(
                color = Color(0xFFC2B59C),
                start = Offset(px(44f), py(y)),
                end = Offset(px(if (idx == 2) 108f else 122f), py(y)),
                strokeWidth = 5f * s,
                cap = StrokeCap.Round
            )
        }
        // خط‌های دفتر — سمت چپ
        lineY.forEachIndexed { idx, y ->
            drawLine(
                color = Color(0xFFC2B59C),
                start = Offset(px(150f), py(y)),
                end = Offset(px(if (idx == 2) 226f else 240f), py(y)),
                strokeWidth = 5f * s,
                cap = StrokeCap.Round
            )
        }
        // مهر موم قرمز
        drawCircle(
            color = Color(0xFF8B1E1E),
            radius = 56f * s,
            center = Offset(px(220f), py(216f))
        )
        // نماد داخل مهر (یک منحنی ساده به‌جای حرف)
        val stampCenter = Offset(px(220f), py(216f))
        drawArc(
            color = Color(0xFFF4EFE4),
            startAngle = -150f,
            sweepAngle = 210f,
            useCenter = false,
            topLeft = Offset(stampCenter.x - 26f * s, stampCenter.y - 26f * s),
            size = Size(52f * s, 52f * s),
            style = Stroke(width = 7f * s, cap = StrokeCap.Round)
        )
        drawLine(
            color = Color(0xFFF4EFE4),
            start = Offset(stampCenter.x - 12f * s, stampCenter.y + 20f * s),
            end = Offset(stampCenter.x + 16f * s, stampCenter.y + 20f * s),
            strokeWidth = 7f * s,
            cap = StrokeCap.Round
        )
    }
}
