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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melato.shop.data.ShopifyRepo
import com.melato.shop.ui.components.ProductCard
import com.melato.shop.ui.theme.*

@Composable
fun ShopScreen(onProductClick: (String) -> Unit) {
    var selectedCategory by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }
    val products = remember(selectedCategory, searchQuery) {
        val base = ShopifyRepo.getByCategory(selectedCategory)
        if (searchQuery.isBlank()) base
        else base.filter {
            it.title.lowercase().contains(searchQuery.lowercase()) ||
            it.description.lowercase().contains(searchQuery.lowercase())
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NearBlack),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text("SHOP", style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = 4.sp))
                Spacer(Modifier.height(16.dp))
                // Search bar
                Surface(
                    color = Surface1,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Divider, RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.Search, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search Melato...", style = MaterialTheme.typography.bodyMedium, color = TextMuted) },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(Icons.Outlined.Close, null, tint = TextMuted,
                                modifier = Modifier.size(18.dp).clickable { searchQuery = "" })
                        }
                    }
                }
            }
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ShopifyRepo.categories) { cat ->
                    val selected = cat.id == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Gold else Surface2)
                            .border(1.dp, if (selected) Gold else Divider, RoundedCornerShape(8.dp))
                            .clickable { selectedCategory = cat.id }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            cat.name.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.8.sp),
                            color = if (selected) Black else TextSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${products.size} pieces",
                    style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            }
        }

        if (products.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No pieces found.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
        }

        val chunked = products.chunked(2)
        items(chunked) { row ->
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
    }
}
