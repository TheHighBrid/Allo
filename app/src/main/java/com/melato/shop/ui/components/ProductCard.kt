package com.melato.shop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.melato.shop.data.model.Product
import com.melato.shop.ui.theme.*

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageHeight: Dp = 240.dp
) {
    val accentColor = Color(product.accent)
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = Surface2,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(imageHeight)) {
                SubcomposeAsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Surface3, accentColor.copy(alpha = 0.3f), Surface2))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = product.title.take(2).uppercase(),
                                style = MaterialTheme.typography.displaySmall.copy(color = accentColor.copy(alpha = 0.6f))
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Surface3, accentColor.copy(alpha = 0.25f), Surface2))),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = product.title.split(" ").take(2).joinToString("\n"),
                                    style = MaterialTheme.typography.labelLarge.copy(color = accentColor.copy(alpha = 0.7f)),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Surface2.copy(alpha = 0.25f)),
                            startY = imageHeight.value * 0.6f
                        )
                    )
                )
                Row(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (product.isNew) {
                        Surface(color = Gold, shape = RoundedCornerShape(4.dp)) {
                            Text("NEW", style = MaterialTheme.typography.labelMedium, color = Black,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(product.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text("$${String.format("%.2f", product.price)} CAD", style = MaterialTheme.typography.bodyMedium, color = Gold)
            }
        }
    }
}
