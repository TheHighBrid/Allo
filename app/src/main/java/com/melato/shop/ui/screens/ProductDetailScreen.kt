package com.melato.shop.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.melato.shop.data.model.Product
import com.melato.shop.ui.theme.*

@Composable
fun ProductDetailScreen(
    product: Product,
    onBack: () -> Unit,
    onAddToCart: (String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedSize by remember { mutableStateOf(product.sizes.getOrElse(2) { product.sizes.first() }) }
    var selectedColor by remember { mutableStateOf(product.colors.firstOrNull() ?: "") }
    var addedToCart by remember { mutableStateOf(false) }
    val accentColor = Color(product.accent)
    val productUrl = "https://melato.ca/products/${product.handle}"

    LazyColumn(modifier = Modifier.fillMaxSize().background(NearBlack)) {
        item {
            // Hero image
            Box(modifier = Modifier.fillMaxWidth().height(520.dp)) {
                SubcomposeAsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Surface3, accentColor.copy(alpha = 0.5f), NearBlack))
                        ), contentAlignment = Alignment.Center) {
                            Text(product.title.split(" ").take(3).joinToString("\n"),
                                style = MaterialTheme.typography.headlineMedium.copy(color = accentColor.copy(alpha = 0.6f)),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(24.dp))
                        }
                    },
                    error = {
                        Box(modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Surface3, accentColor.copy(alpha = 0.4f), NearBlack))
                        ), contentAlignment = Alignment.Center) {
                            Text(product.title.split(" ").take(3).joinToString("\n"),
                                style = MaterialTheme.typography.headlineMedium.copy(color = accentColor.copy(alpha = 0.5f)),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(24.dp))
                        }
                    }
                )
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(NearBlack.copy(alpha = 0.3f), Color.Transparent, NearBlack.copy(alpha = 0.5f))
                    )
                ))
                // Back
                Box(
                    modifier = Modifier.statusBarsPadding().padding(16.dp).size(40.dp)
                        .clip(CircleShape).background(Surface1.copy(alpha = 0.9f))
                        .clickable(onClick = onBack).align(Alignment.TopStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
                // Wishlist
                Box(
                    modifier = Modifier.statusBarsPadding().padding(16.dp).size(40.dp)
                        .clip(CircleShape).background(Surface1.copy(alpha = 0.9f))
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.FavoriteBorder, "Wishlist", tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
                // Tags
                Row(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (product.isNew) {
                        Surface(color = Gold, shape = RoundedCornerShape(4.dp)) {
                            Text("NEW DROP", style = MaterialTheme.typography.labelMedium, color = Black,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }
                    product.tags.forEach { tag ->
                        Surface(color = Surface3.copy(alpha = 0.9f), shape = RoundedCornerShape(4.dp)) {
                            Text(tag, style = MaterialTheme.typography.labelMedium, color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
                // Title + price
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(product.title, style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(product.category.replace("-", " ").split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                            style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$${String.format("%.2f", product.price)}",
                            style = MaterialTheme.typography.headlineMedium, color = Gold)
                        Text("CAD", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text("✈  Free complimentary delivery", style = MaterialTheme.typography.bodyMedium, color = TextMuted)

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = Divider)
                Spacer(Modifier.height(24.dp))

                // Color
                if (product.colors.isNotEmpty()) {
                    Text("COLOUR", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp), color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        product.colors.forEach { color ->
                            val isSel = color == selectedColor
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Surface3 else Surface2)
                                    .border(if (isSel) 1.5.dp else 1.dp, if (isSel) Gold else Divider, RoundedCornerShape(8.dp))
                                    .clickable { selectedColor = color }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(color, style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSel) TextPrimary else TextSecondary)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // Size
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("SIZE", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp), color = TextSecondary)
                    Text("Size Guide", style = MaterialTheme.typography.bodyMedium, color = Gold,
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://melato.ca/pages/faq")))
                        })
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    product.sizes.forEach { size ->
                        val isSel = size == selectedSize
                        Box(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Gold else Surface2)
                                .border(if (isSel) 0.dp else 1.dp, Divider, RoundedCornerShape(8.dp))
                                .clickable { selectedSize = size },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(size, style = MaterialTheme.typography.labelLarge,
                                color = if (isSel) Black else TextSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = Divider)
                Spacer(Modifier.height(20.dp))

                // Description
                Text("DESCRIPTION", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp), color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                Text(product.description, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = Divider)
                Spacer(Modifier.height(20.dp))

                // Care
                Text("CARE", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp), color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                Text("Wash cold on a gentle cycle, inside out. Avoid bleach. Hang dry. Do not iron directly over embroidery, prints, or branded details.",
                    style = MaterialTheme.typography.bodyLarge, color = TextSecondary)

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = Divider)
                Spacer(Modifier.height(20.dp))

                // Shipping
                Text("SHIPPING & RETURNS", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp), color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("✈  Free complimentary delivery on all orders", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("🌍  Ships worldwide", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("📦  1–3 business day processing", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("↩  30-day returns on eligible unworn items", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }

                Spacer(Modifier.height(32.dp))

                // Add to Cart
                Button(
                    onClick = {
                        onAddToCart(selectedSize, selectedColor.ifEmpty { "Default" })
                        addedToCart = true
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (addedToCart) Surface3 else Gold,
                        contentColor = if (addedToCart) TextPrimary else Black
                    )
                ) {
                    Text(if (addedToCart) "✓  ADDED TO CART" else "ADD TO CART",
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp))
                }

                Spacer(Modifier.height(12.dp))

                // Buy on website
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(productUrl)))
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder(true),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Icon(Icons.Outlined.OpenInBrowser, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("VIEW ON MELATO.CA", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp))
                }

                Spacer(Modifier.height(48.dp))
            }
        }
    }
}
