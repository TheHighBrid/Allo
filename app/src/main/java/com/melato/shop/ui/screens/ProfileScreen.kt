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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melato.shop.ui.theme.*

@Composable
fun ProfileScreen(onFaqClick: () -> Unit) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NearBlack),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.size(84.dp).clip(CircleShape)
                        .background(Surface2).border(1.5.dp, Gold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Person, null, tint = Gold, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("MELATO MEMBER", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp), color = Gold)
                Spacer(Modifier.height(4.dp))
                Text("Sign in to access your orders and wishlist",
                    style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://melato.ca/account/login"))) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Black)
                ) {
                    Text("SIGN IN", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp))
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://melato.ca/account/register"))) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text("CREATE ACCOUNT", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp))
                }
            }
        }

        // Visit store
        item {
            ProfileSection("OUR STORE") {
                ProfileRow(
                    icon = Icons.Outlined.LocationOn,
                    label = "Melatelier — Tanger Outlets Ottawa",
                    sub = "8555 Campeau Drive, Suite 880, Ottawa, ON",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://maps.google.com/?q=8555+Campeau+Drive+Ottawa+Ontario")))
                    }
                )
                ProfileRow(
                    icon = Icons.Outlined.Event,
                    label = "Pop-Up Shows",
                    sub = "June 21 · July 21 · August 20 — Location TBA 48h prior",
                    onClick = null
                )
            }
        }

        item {
            ProfileSection("SUPPORT") {
                ProfileRow(Icons.Outlined.Email, "support@melato.ca", "Orders, returns & general inquiries") {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@melato.ca")))
                }
                ProfileRow(Icons.Outlined.Phone, "+1 (613) 209-6464", "Customer care line") {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+16132096464")))
                }
                ProfileRow(Icons.Outlined.QuestionAnswer, "FAQ", "Shipping, sizing, returns and more", onClick = onFaqClick)
                ProfileRow(Icons.Outlined.LocalShipping, "Shipping & Returns", "Free delivery on all orders · 30-day returns") {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://melato.ca/pages/faq")))
                }
            }
        }

        item {
            ProfileSection("CONTACT") {
                ProfileRow(Icons.Outlined.Business, "Press & Collaborations", "press@melato.ca") {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:press@melato.ca")))
                }
                ProfileRow(Icons.Outlined.Work, "Sales & Wholesale", "sales@melato.ca") {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:sales@melato.ca")))
                }
                ProfileRow(Icons.Outlined.Campaign, "Marketing", "marketing@melato.ca") {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:marketing@melato.ca")))
                }
                ProfileRow(Icons.Outlined.Work, "Careers", "careers@melato.ca") {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:careers@melato.ca")))
                }
            }
        }

        item {
            ProfileSection("FOLLOW") {
                ProfileRow(Icons.Outlined.CameraAlt, "@melato.ca", "Instagram") {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/melato.ca")))
                }
                ProfileRow(Icons.Outlined.Language, "melato.ca", "Website") {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://melato.ca")))
                }
            }
        }

        item {
            ProfileSection("LEGAL") {
                ProfileRow(Icons.Outlined.Shield, "Privacy Policy", null) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://melato.ca/policies/privacy-policy")))
                }
                ProfileRow(Icons.Outlined.Article, "Terms of Service", null) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://melato.ca/policies/terms-of-service")))
                }
                ProfileRow(Icons.Outlined.Undo, "Returns & Refunds", null) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://melato.ca/policies/refund-policy")))
                }
            }
        }

        item {
            Spacer(Modifier.height(32.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("COMME NEVER CHANGE",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp, fontSize = 10.sp),
                    color = Gold)
                Spacer(Modifier.height(4.dp))
                Text("MELATO  ·  CANADA  ·  melato.ca",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                    color = TextMuted)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp, fontSize = 10.sp), color = TextMuted)
        Spacer(Modifier.height(10.dp))
        Surface(color = Surface1, shape = RoundedCornerShape(12.dp)) {
            Column { content() }
        }
    }
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    label: String,
    sub: String?,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            if (!sub.isNullOrEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(sub, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            }
        }
        if (onClick != null) {
            Icon(Icons.Outlined.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}
