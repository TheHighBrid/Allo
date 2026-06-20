package com.melato.shop.data.model

data class Product(
    val id: String,
    val handle: String,
    val title: String,
    val price: Double,
    val currency: String = "CAD",
    val description: String,
    val imageUrl: String,
    val category: String,
    val sizes: List<String> = listOf("XS", "S", "M", "L", "XL"),
    val colors: List<String> = emptyList(),
    val isFeatured: Boolean = false,
    val isNew: Boolean = false,
    val tags: List<String> = emptyList(),
    val accent: Long = 0xFF1E1E1E
)

data class CartItem(
    val product: Product,
    val size: String,
    val color: String,
    var quantity: Int = 1
)

data class Category(
    val id: String,
    val name: String
)

data class FaqItem(
    val question: String,
    val answer: String
)

data class PopupEvent(
    val date: String,
    val note: String
)
