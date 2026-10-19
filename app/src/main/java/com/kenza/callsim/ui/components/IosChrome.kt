package com.kenza.callsim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenza.callsim.ui.theme.IOSColors

/** Large navigation title used by Settings / Memory / Schedule / Script Studio. */
@Composable
fun IosNavHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = IOSColors.Blue,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onBack),
            )
            Spacer(Modifier.size(12.dp))
        }
        Text(
            title,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Row(content = trailing)
        }
    }
}

/** Uppercase iOS Settings-style section caption above a grouped card. */
@Composable
fun IosSectionCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = IOSColors.TertiaryLabel,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
        modifier = modifier.padding(start = 16.dp, bottom = 6.dp, top = 10.dp),
    )
}

/**
 * Grouped inset list card matching the dark iOS Settings aesthetic used by the call UI.
 * Children should be full-width rows; use [IosRowSeparator] between them.
 */
@Composable
fun IosGroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IOSColors.SecondaryBackground),
        content = content,
    )
}

@Composable
fun IosGroupedRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    contentPadding: Dp = 16.dp,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = contentPadding, vertical = 12.dp),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        content = content,
    )
}

@Composable
fun IosRowSeparator(startInset: Dp = 16.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = startInset),
        thickness = 0.5.dp,
        color = IOSColors.Separator,
    )
}

@Composable
fun IosFooterNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = IOSColors.TertiaryLabel,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun IosScreenSpacer(height: Dp = 18.dp) {
    Spacer(Modifier.height(height))
}
