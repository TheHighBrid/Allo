package com.melato.shop.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melato.shop.data.ShopifyRepo
import com.melato.shop.ui.theme.*

@Composable
fun FaqScreen(onBack: () -> Unit) {
    val faqItems = ShopifyRepo.faq
    val expandedIndex = remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NearBlack),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Text("FAQ", style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = 4.sp))
            }
        }

        item {
            Text(
                "FREQUENTLY ASKED QUESTIONS",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            Spacer(Modifier.height(8.dp))
        }

        itemsIndexed(faqItems) { index, item ->
            Surface(
                color = Surface1,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            expandedIndex.value = if (expandedIndex.value == index) null else index
                        }.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            item.question,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        )
                        Icon(
                            if (expandedIndex.value == index) Icons.Outlined.Remove else Icons.Outlined.Add,
                            null,
                            tint = Gold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    AnimatedVisibility(
                        visible = expandedIndex.value == index,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column {
                            HorizontalDivider(color = Divider, modifier = Modifier.padding(horizontal = 16.dp))
                            Text(
                                item.answer,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            Surface(
                color = Surface1, shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("STILL HAVE QUESTIONS?", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp))
                    Spacer(Modifier.height(8.dp))
                    Text("Reach us at support@melato.ca or call +1 (613) 209-6464. We respond within 24–48 hours.",
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }
    }
}
