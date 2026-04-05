package com.cristianv.radianceapp.model

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val items: List<Map<String, Any>> = emptyList(),
    val subtotal: Double = 0.0,
    val shipping: Double = 5.99,
    val total: Double = 0.0,
    val shippingAddress: String = "",
    val status: String = "pending",
    val createdAt: Long = 0L
)
