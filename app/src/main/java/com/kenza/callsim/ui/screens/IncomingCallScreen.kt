package com.kenza.callsim.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenza.callsim.call.CallUiState
import com.kenza.callsim.ui.theme.IOSColors
import kotlin.math.roundToInt

private val Ios26Top = Color(0xFF0B2933)
private val Ios26Mid = Color(0xFF0C5755)
private val Ios26Bottom = Color(0xFF0A3F49)
private val GlassStroke = Color.White.copy(alpha = 0.18f)
private val GlassFill = Color.White.copy(alpha = 0.11f)

/**
 * iOS 26-style locked incoming call.
 *
 * The primary action deliberately requires a horizontal drag. There is no
 * tappable green Accept button on this presentation, matching iPhone's locked
 * incoming-call interaction.
 */
@Composable
fun IncomingCallScreen(
    state: CallUiState,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Ios26Top, Ios26Mid, Ios26Bottom)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))

            Text(
                text = "Incoming call",
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = state.contactName,
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.8).sp,
                maxLines = 1
            )

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 300.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                GlassShortcut(
                    label = "Message",
                    icon = Icons.Filled.Message,
                    onClick = null
                )
                GlassShortcut(
                    label = "Voicemail",
                    icon = Icons.Filled.Voicemail,
                    onClick = onDecline
                )
            }

            Spacer(Modifier.height(32.dp))
            SlideToAnswer(onAccept = onAccept)
            Spacer(Modifier.height(18.dp))
        }
    }
}

/**
 * iOS 26-style compact incoming call shown while the phone is already unlocked.
 * The active app remains visible beneath this glass banner.
 */
@Composable
fun IncomingCallBanner(
    state: CallUiState,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val shape = RoundedCornerShape(30.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .shadow(elevation = 22.dp, shape = shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xE83B3B40),
                        Color(0xE2212125)
                    )
                )
            )
            .border(0.7.dp, GlassStroke, shape)
            .padding(start = 12.dp, end = 10.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name = state.contactName, size = 46)
        Spacer(Modifier.size(11.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = state.contactName,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                maxLines = 1
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = "Incoming call",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }

        BannerCallButton(
            background = IOSColors.Red,
            icon = Icons.Filled.CallEnd,
            contentDescription = "Decline",
            onClick = onDecline
        )
        Spacer(Modifier.size(8.dp))
        BannerCallButton(
            background = IOSColors.Green,
            icon = Icons.Filled.Call,
            contentDescription = "Answer",
            onClick = onAccept
        )
    }
}

@Composable
private fun SlideToAnswer(onAccept: () -> Unit) {
    val density = LocalDensity.current
    val knobSize = 62.dp
    val inset = 6.dp
    val knobSizePx = with(density) { knobSize.toPx() }
    val insetPx = with(density) { inset.toPx() }

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    val maxDragPx = (trackWidthPx - knobSizePx - insetPx * 2f).coerceAtLeast(0f)
    val progress = if (maxDragPx > 0f) (dragOffsetPx / maxDragPx).coerceIn(0f, 1f) else 0f
    val dragState = rememberDraggableState { delta ->
        dragOffsetPx = (dragOffsetPx + delta).coerceIn(0f, maxDragPx)
    }

    val shape = RoundedCornerShape(38.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp)
            .height(74.dp)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0x4C65D9CC),
                        Color(0x385AB6C8),
                        Color(0x3457AAB8)
                    )
                )
            )
            .border(0.8.dp, Color.White.copy(alpha = 0.20f), shape)
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                enabled = maxDragPx > 0f,
                onDragStopped = { _ ->
                    if (maxDragPx > 0f && dragOffsetPx >= maxDragPx * 0.74f) {
                        dragOffsetPx = maxDragPx
                        onAccept()
                    } else {
                        animate(
                            initialValue = dragOffsetPx,
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { value, _ ->
                            dragOffsetPx = value
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "slide to answer",
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.alpha(1f - progress * 0.78f)
        )

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (insetPx + dragOffsetPx).roundToInt(),
                        y = 0
                    )
                }
                .align(Alignment.CenterStart)
                .size(knobSize)
                .shadow(8.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(Color(0xFFF8F8F8)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Call,
                contentDescription = "Slide to answer",
                tint = IOSColors.Green,
                modifier = Modifier.size(29.dp)
            )
        }
    }
}

@Composable
private fun GlassShortcut(
    label: String,
    icon: ImageVector,
    onClick: (() -> Unit)?,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(GlassFill)
                .border(0.7.dp, GlassStroke, CircleShape)
                .then(clickModifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(23.dp)
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.86f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun BannerCallButton(
    background: Color,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun Avatar(name: String, size: Int = 112) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF8E8E93), Color(0xFF48484A)))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontSize = (size / 2.2).sp,
            fontWeight = FontWeight.Medium
        )
    }
}
