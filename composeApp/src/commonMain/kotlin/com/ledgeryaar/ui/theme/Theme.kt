package com.ledgeryaar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ledgeryaar.data.repository.AppTheme

/** رنگ‌های معنایی (درآمد سبز، هزینه قرمز، …) که بسته به تم عوض می‌شوند */
private data class Accents(val green: Color, val red: Color, val bronze: Color, val navy: Color)

private val ClassicAccents = Accents(Color(0xFF1F5C4D), Color(0xFF8B1E1E), Color(0xFFB08D3F), Color(0xFF2E4159))
private val DarkAccents = Accents(Color(0xFF4FA98E), Color(0xFFE0675F), Color(0xFFD4B06A), Color(0xFF8FA7C4))
private val ForestAccents = Accents(Color(0xFF8BC48A), Color(0xFFE27D5F), Color(0xFFD9B46A), Color(0xFF9CC3B8))

object AppColors {
    val Paper = Color(0xFFF4EFE4)
    val PaperDeep = Color(0xFFEDE6D6)
    val Ink = Color(0xFF2B2520)
    val InkSoft = Color(0xFF6B5F52)
    val InkFaint = Color(0xFF9B8F7F)
    val LineStrong = Color(0xFFC2B59C)
    val CardColor = Color(0xFFFBF8F1)

    // رنگ‌های جنگلی (از روی عکس لوگو: سبز خزه، برنز گردنبند، قرمز موها)
    val ForestDeep = Color(0xFF0F1D14)
    val ForestBg = Color(0xFF15281C)
    val ForestCard = Color(0xFF1E3626)
    val ForestCardHigh = Color(0xFF27452F)
    val ForestLine = Color(0xFF3C5E44)
    val Moss = Color(0xFF8BC48A)
    val Parchment = Color(0xFFEDE6CC)
    val ParchmentSoft = Color(0xFFB9C2A6)

    private var accents by mutableStateOf(ClassicAccents)
    internal fun applyTheme(theme: AppTheme) {
        accents = when (theme) {
            AppTheme.LIGHT -> ClassicAccents
            AppTheme.DARK -> DarkAccents
            AppTheme.FOREST -> ForestAccents
        }
    }

    val StampRed: Color get() = accents.red
    val LedgerGreen: Color get() = accents.green
    val Bronze: Color get() = accents.bronze
    val Navy: Color get() = accents.navy

    val DarkPaper = Color(0xFF1C1814)
    val DarkCard = Color(0xFF262019)
    val DarkInk = Color(0xFFEDE6D6)
}

private val LightScheme = lightColorScheme(
    primary = ClassicAccents.red,
    onPrimary = Color.White,
    secondary = ClassicAccents.green,
    onSecondary = Color.White,
    tertiary = ClassicAccents.bronze,
    background = AppColors.Paper,
    onBackground = AppColors.Ink,
    surface = AppColors.CardColor,
    onSurface = AppColors.Ink,
    surfaceVariant = AppColors.PaperDeep,
    onSurfaceVariant = AppColors.InkSoft,
    outline = AppColors.LineStrong,
    error = ClassicAccents.red
)

private val DarkScheme = darkColorScheme(
    primary = DarkAccents.red,
    onPrimary = Color.White,
    secondary = DarkAccents.green,
    onSecondary = Color.White,
    tertiary = DarkAccents.bronze,
    background = AppColors.DarkPaper,
    onBackground = AppColors.DarkInk,
    surface = AppColors.DarkCard,
    onSurface = AppColors.DarkInk,
    error = DarkAccents.red
)

private val ForestScheme = darkColorScheme(
    primary = AppColors.Moss,
    onPrimary = AppColors.ForestDeep,
    primaryContainer = AppColors.ForestCardHigh,
    onPrimaryContainer = AppColors.Parchment,
    secondary = ForestAccents.bronze,
    onSecondary = AppColors.ForestDeep,
    secondaryContainer = Color(0xFF3B3A22),
    onSecondaryContainer = AppColors.Parchment,
    tertiary = ForestAccents.red,
    onTertiary = AppColors.ForestDeep,
    background = AppColors.ForestBg,
    onBackground = AppColors.Parchment,
    surface = AppColors.ForestCard,
    onSurface = AppColors.Parchment,
    surfaceVariant = AppColors.ForestCardHigh,
    onSurfaceVariant = AppColors.ParchmentSoft,
    surfaceContainer = AppColors.ForestCard,
    surfaceContainerHigh = AppColors.ForestCardHigh,
    surfaceContainerHighest = AppColors.ForestCardHigh,
    surfaceContainerLow = AppColors.ForestBg,
    outline = AppColors.ForestLine,
    outlineVariant = AppColors.ForestLine,
    error = ForestAccents.red
)

private val LedgerTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontSize = 11.sp)
)

@Composable
fun LedgerTheme(theme: AppTheme = AppTheme.LIGHT, content: @Composable () -> Unit) {
    // بعد از هر ترکیب، رنگ‌های معنایی هم با تم هماهنگ می‌شوند (state است، پس صفحه‌ها دوباره رسم می‌شوند)
    SideEffect { AppColors.applyTheme(theme) }
    MaterialTheme(
        colorScheme = when (theme) {
            AppTheme.LIGHT -> LightScheme
            AppTheme.DARK -> DarkScheme
            AppTheme.FOREST -> ForestScheme
        },
        typography = LedgerTypography,
        content = content
    )
}
