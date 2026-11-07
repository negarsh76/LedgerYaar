package com.ledgeryaar.ui.screens.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgeryaar.i18n.AppStrings
import com.ledgeryaar.resources.Res
import com.ledgeryaar.resources.splash_bg
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

private val Parchment = Color(0xFFEDE6CC)
private val Moss = Color(0xFF8BC48A)

/** صفحهٔ لودینگ: عکس جنگلی تمام‌صفحه، اسم اپ و نوار پیشرفت */
@Composable
fun SplashScreen(strings: AppStrings, onFinished: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (started) 1f else 0f, tween(600))
    val zoom by animateFloatAsState(if (started) 1.0f else 1.08f, tween(2200))
    val progress by animateFloatAsState(if (started) 1f else 0f, tween(1900, easing = LinearEasing))

    LaunchedEffect(Unit) {
        started = true
        delay(2000)
        onFinished()
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0F1D14))) {
        Image(
            painter = painterResource(Res.drawable.splash_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().scale(zoom).alpha(alpha)
        )
        // سایه‌ی پایین برای خوانا شدن متن
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.45f to Color.Transparent,
                    1f to Color(0xEE0F1D14)
                )
            )
        )
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 56.dp).alpha(alpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(strings.appName, color = Parchment, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text(strings.dashboardSubtitle, color = Parchment.copy(alpha = 0.75f), fontSize = 13.sp)
            LinearProgressIndicator(
                progress = { progress },
                color = Moss,
                trackColor = Parchment.copy(alpha = 0.2f),
                modifier = Modifier.padding(top = 18.dp).width(160.dp)
            )
        }
    }
}
