package com.melato.shop.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melato.shop.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val wordmark = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }
    val sub = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        wordmark.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        delay(100)
        tagline.animateTo(1f, tween(700))
        delay(100)
        sub.animateTo(1f, tween(600))
        delay(1000)
        wordmark.animateTo(0f, tween(500))
        onDone()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(NearBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "MELATO",
                style = MaterialTheme.typography.displayLarge.copy(
                    letterSpacing = 12.sp, fontWeight = FontWeight.Black, fontSize = 40.sp
                ),
                color = White,
                modifier = Modifier.alpha(wordmark.value)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "COMME NEVER CHANGE",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 4.sp, fontSize = 10.sp),
                color = Gold,
                modifier = Modifier.alpha(tagline.value),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Cut with intent.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = TextMuted,
                modifier = Modifier.alpha(sub.value)
            )
        }
    }
}
