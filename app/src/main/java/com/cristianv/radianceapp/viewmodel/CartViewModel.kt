package com.cristianv.radianceapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.cristianv.radianceapp.model.Product

data class CartItem(
    val product: Product,
    var quantity: Int,
    val selectedSize: String? = null,
    val selectedColor: String? = null
)

class CartViewModel : ViewModel() {
    private val _cartItems = mutableStateListOf<CartItem>()
    val cartItems: List<CartItem> = _cartItems

    var totalPrice by mutableStateOf(0.0)
        private set

    var totalSavings by mutableStateOf(0.0)
        private set

    val totalItems: Int
        get() = _cartItems.sumOf { it.quantity }

    fun addToCart(product: Product, quantity: Int, selectedSize: String?, selectedColor: String?) {
        val existingItem = _cartItems.find { 
            it.product.id == product.id && it.selectedSize == selectedSize && it.selectedColor == selectedColor 
        }
        
        if (existingItem != null) {
            existingItem.quantity += quantity
        } else {
            _cartItems.add(CartItem(product, quantity, selectedSize, selectedColor))
        }
        calculateTotals()
    }

    fun removeFromCart(cartItem: CartItem) {
        _cartItems.remove(cartItem)
        calculateTotals()
    }

    fun increaseQuantity(cartItem: CartItem) {
        val index = _cartItems.indexOf(cartItem)
        if (index != -1) {
            _cartItems[index] = cartItem.copy(quantity = cartItem.quantity + 1)
            calculateTotals()
        }
    }

    fun decreaseQuantity(cartItem: CartItem) {
        if (cartItem.quantity > 1) {
            val index = _cartItems.indexOf(cartItem)
            if (index != -1) {
                _cartItems[index] = cartItem.copy(quantity = cartItem.quantity - 1)
                calculateTotals()
            }
        }
    }

    fun clearCart() {
        _cartItems.clear()
        calculateTotals()
    }

    private fun calculateTotals() {
        var price = 0.0
        var savings = 0.0
        
        _cartItems.forEach { item ->
            price += item.product.price * item.quantity
            item.product.originalPrice?.let { original ->
                savings += (original - item.product.price) * item.quantity
            }
        }
        
        totalPrice = price
        totalSavings = savings
    }
}
