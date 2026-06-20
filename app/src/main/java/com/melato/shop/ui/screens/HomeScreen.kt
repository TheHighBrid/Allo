package com.melato.shop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.melato.shop.data.ShopifyRepo
import com.melato.shop.ui.components.ProductCard
import com.melato.shop.ui.theme.*

@Composable
fun HomeScreen(
    cartCount: Int,
    onProductClick: (String) -> Unit,
    onCartClick: () -> Unit,
    onShopClick: () -> Unit,
    onCategoryClick: (String) -> Unit
) {
    val featured = ShopifyRepo.getFeatured()
    val newArrivals = ShopifyRepo.getNew()
    val hero = featured.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NearBlack),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Top bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "MELATO",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        letterSpacing = 6.sp, fontWeight = FontWeight.Black
                    )
                )
                BadgedBox(badge = {
                    if (cartCount > 0) {
                        Badge(containerColor = Gold, contentColor = Black) {
                            Text(cartCount.toString(), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }) {
                    IconButton(onClick = onCartClick) {
                        Icon(Icons.Outlined.ShoppingBag, "Cart", tint = TextPrimary, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }

        // Hero banner
        if (hero != null) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(520.dp)
                        .padding(horizontal = 20.dp).clip(RoundedCornerShape(20.dp))
                        .clickable { onProductClick(hero.id) }
                ) {
                    val accentColor = Color(hero.accent)
                    SubcomposeAsyncImage(
                        model = hero.imageUrl,
                        contentDescription = hero.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(modifier = Modifier.fillMaxSize().background(
                                Brush.verticalGradient(listOf(Surface3, accentColor.copy(alpha = 0.4f), NearBlack))
                            ))
                        },
                        error = {
                            Box(modifier = Modifier.fillMaxSize().background(
                                Brush.verticalGradient(listOf(Surface3, accentColor.copy(alpha = 0.35f), NearBlack))
                            ))
                        }
                    )
                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, NearBlack.copy(alpha = 0.9f)),
                            startY = 220f
                        )
                    ))
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)
                    ) {
                        if (hero.isNew) {
                            Surface(color = Gold, shape = RoundedCornerShape(4.dp)) {
                                Text("NEW DROP", style = MaterialTheme.typography.labelMedium, color = Black,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                        Text(hero.title, style = MaterialTheme.typography.displaySmall, color = White)
                        Spacer(Modifier.height(4.dp))
                        Text("$${String.format("%.2f", hero.price)} CAD",
                            style = MaterialTheme.typography.titleLarge, color = Gold)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { onProductClick(hero.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
                        ) {
                            Text("SHOP NOW", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp))
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }

        // Brand motto
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(color = Divider)
                Spacer(Modifier.height(20.dp))
                Text(
                    "COMME NEVER CHANGE",
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 4.sp, fontSize = 11.sp),
                    color = Gold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Cut with intent. Worn anywhere.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Divider)
            }
            Spacer(Modifier.height(32.dp))
        }

        // Pop-up events
        item {
            Text("POP-UP SHOWS", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(14.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ShopifyRepo.popupEvents) { event ->
                    Surface(
                        color = Surface1,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.width(160.dp)
                            .border(1.dp, Divider, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("🗓", fontSize = 22.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(event.date, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(event.note, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }

        // New Arrivals
        if (newArrivals.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("NEW ARRIVALS", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp))
                    Text("See all", style = MaterialTheme.typography.bodyMedium, color = Gold,
                        modifier = Modifier.clickable(onClick = onShopClick))
                }
                Spacer(Modifier.height(14.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(newArrivals) { p ->
                        ProductCard(p, onClick = { onProductClick(p.id) },
                            modifier = Modifier.width(200.dp), imageHeight = 270.dp)
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }

        // Collections grid
        item {
            Text("COLLECTIONS", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(14.dp))
        }

        val collectionRows = listOf(
            listOf("tracksuits" to "TRACKSUITS", "denim" to "DENIM"),
            listOf("accessories" to "ACCESSORIES", "eves-wardrobe" to "EVE'S WARDROBE")
        )
        items(collectionRows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { (catId, label) ->
                    val catProducts = ShopifyRepo.getByCategory(catId)
                    val sampleProduct = catProducts.firstOrNull { it.isFeatured } ?: catProducts.firstOrNull()
                    Box(
                        modifier = Modifier.weight(1f).height(140.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onCategoryClick(catId) }
                    ) {
                        if (sampleProduct != null) {
                            val accentColor = Color(sampleProduct.accent)
                            SubcomposeAsyncImage(
                                model = sampleProduct.imageUrl,
                                contentDescription = label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                loading = {
                                    Box(modifier = Modifier.fillMaxSize().background(
                                        Brush.verticalGradient(listOf(Surface3, accentColor.copy(alpha = 0.5f)))
                                    ))
                                },
                                error = {
                                    Box(modifier = Modifier.fillMaxSize().background(
                                        Brush.verticalGradient(listOf(Surface3, accentColor.copy(alpha = 0.4f)))
                                    ))
                                }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Surface2))
                        }
                        Box(modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color.Transparent, NearBlack.copy(alpha = 0.85f)), startY = 50f)
                        ))
                        Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                            Text(label, style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp), color = White)
                            Text("${catProducts.size} pieces", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        item { Spacer(Modifier.height(24.dp)) }

        // Featured pieces
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("FEATURED", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp))
                Text("View all", style = MaterialTheme.typography.bodyMedium, color = Gold,
                    modifier = Modifier.clickable(onClick = onShopClick))
            }
            Spacer(Modifier.height(14.dp))
        }

        val featuredChunked = featured.drop(1).take(6).chunked(2)
        items(featuredChunked) { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { p ->
                    ProductCard(p, onClick = { onProductClick(p.id) },
                        modifier = Modifier.weight(1f), imageHeight = 220.dp)
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }

        // Free shipping banner
        item {
            Spacer(Modifier.height(20.dp))
            Surface(
                color = Surface1, shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .border(1.dp, Divider, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("✈", fontSize = 28.sp)
                    Column {
                        Text("FREE COMPLIMENTARY DELIVERY",
                            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp))
                        Spacer(Modifier.height(3.dp))
                        Text("On all orders. Worldwide. No minimum.",
                            style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
