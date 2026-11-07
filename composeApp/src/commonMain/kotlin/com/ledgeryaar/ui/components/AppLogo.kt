package com.ledgeryaar.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ledgeryaar.resources.Res
import com.ledgeryaar.resources.app_logo
import org.jetbrains.compose.resources.painterResource

/** لوگوی اپ (عکس گرد) */
@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.app_logo),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.tertiary, CircleShape)
    )
}
