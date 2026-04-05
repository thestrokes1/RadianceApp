package com.cristianv.radianceapp.model

data class Product(
    val id: String = "",
    val name: String = "",
    val brand: String = "",
    val price: Double = 0.0,
    val originalPrice: Double? = null,
    val imageUrl: String = "",
    val category: String = "",
    val sizes: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val colorImages: Map<String, String> = emptyMap(),
    val rating: Double = 0.0,
    val reviewCount: Long = 0,
    val isNew: Boolean = false,
    val isFavorite: Boolean = false,
    val description: String = ""
)
