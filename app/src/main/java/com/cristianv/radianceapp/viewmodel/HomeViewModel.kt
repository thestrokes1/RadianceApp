package com.cristianv.radianceapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristianv.radianceapp.data.FirebaseRepository
import com.cristianv.radianceapp.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ProductsState {
    object Loading : ProductsState()
    data class Success(val products: List<Product>) : ProductsState()
    data class Error(val message: String) : ProductsState()
}

class HomeViewModel : ViewModel() {

    private val repository = FirebaseRepository()

    private val _productsState = MutableStateFlow<ProductsState>(ProductsState.Loading)
    val productsState: StateFlow<ProductsState> = _productsState

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _productsState.value = ProductsState.Loading
            android.util.Log.d("HomeViewModel", "Loading products from Firebase...")
            try {
                val products = repository.getProducts()
                android.util.Log.d("HomeViewModel", "Loaded ${products.size} products")
                _productsState.value = ProductsState.Success(products)
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading products: ${e.message}")
                _productsState.value = ProductsState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            android.util.Log.d("HomeViewModel", "Refreshing products from Firebase...")
            try {
                val products = repository.getProducts()
                android.util.Log.d("HomeViewModel", "Refreshed ${products.size} products")
                _productsState.value = ProductsState.Success(products)
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error refreshing products: ${e.message}")
                _productsState.value = ProductsState.Error(e.message ?: "Unknown error")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }
}
