package com.cristianv.radianceapp.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.cristianv.radianceapp.model.Product

class WishlistViewModel : ViewModel() {

    private val _wishlistItems = mutableStateListOf<Product>()
    val wishlistItems: List<Product> get() = _wishlistItems

    val totalItems: Int get() = _wishlistItems.size

    fun addToWishlist(product: Product) {
        if (!isInWishlist(product.id)) {
            _wishlistItems.add(product)
        }
    }

    fun removeFromWishlist(productId: String) {
        _wishlistItems.removeAll { it.id == productId }
    }

    fun toggleWishlist(product: Product) {
        if (isInWishlist(product.id)) {
            removeFromWishlist(product.id)
        } else {
            addToWishlist(product)
        }
    }

    fun isInWishlist(productId: String): Boolean {
        return _wishlistItems.any { it.id == productId }
    }
}