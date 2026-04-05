package com.cristianv.radianceapp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristianv.radianceapp.data.FirebaseRepository
import com.cristianv.radianceapp.model.Order
import com.cristianv.radianceapp.model.Product
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val repository = FirebaseRepository()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders

    private val _isLoadingOrders = MutableStateFlow(false)
    val isLoadingOrders: StateFlow<Boolean> = _isLoadingOrders

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = db.collection("products").get().await()
                _products.value = snapshot.documents.mapNotNull { doc ->
                    Product(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        brand = doc.getString("brand") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        originalPrice = doc.getDouble("originalPrice"),
                        imageUrl = doc.getString("imageUrl") ?: "",
                        category = doc.getString("category") ?: "",
                        sizes = (doc.get("sizes") as? List<*>)?.map { it.toString() } ?: emptyList(),
                        colors = (doc.get("colors") as? List<*>)?.map { it.toString() } ?: emptyList(),
                        colorImages = (doc.get("colorImages") as? Map<*, *>)
                            ?.mapKeys { it.key.toString() }
                            ?.mapValues { it.value.toString() } ?: emptyMap(),
                        rating = doc.getDouble("rating") ?: 0.0,
                        reviewCount = doc.getLong("reviewCount") ?: 0L,
                        isNew = doc.getBoolean("isNew") ?: false,
                        description = doc.getString("description") ?: ""
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load products"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addProduct(
        name: String,
        brand: String,
        price: String,
        category: String,
        imageUrl: String,
        description: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val productData = mapOf(
                    "name" to name,
                    "brand" to brand,
                    "price" to (price.toDoubleOrNull() ?: 0.0),
                    "category" to category,
                    "imageUrl" to imageUrl,
                    "description" to description,
                    "sizes" to emptyList<String>(),
                    "colors" to emptyList<String>(),
                    "colorImages" to emptyMap<String, String>(),
                    "rating" to 0.0,
                    "reviewCount" to 0L,
                    "isNew" to true
                )
                db.collection("products").add(productData).await()
                _successMessage.value = "Product added successfully"
                loadProducts()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to add product"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProduct(
        productId: String,
        name: String,
        brand: String,
        price: String,
        originalPrice: String,
        category: String,
        imageUrl: String,
        description: String,
        isNew: Boolean,
        sizes: String,
        colors: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = mutableMapOf<String, Any>(
                    "name" to name,
                    "brand" to brand,
                    "price" to (price.toDoubleOrNull() ?: 0.0),
                    "category" to category,
                    "imageUrl" to imageUrl,
                    "description" to description,
                    "isNew" to isNew,
                    "sizes" to sizes.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    "colors" to colors.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                )
                val oprice = originalPrice.trim().toDoubleOrNull()
                if (oprice != null) {
                    data["originalPrice"] = oprice
                } else {
                    data["originalPrice"] = FieldValue.delete()
                }
                repository.updateProduct(productId, data)
                _successMessage.value = "Product updated"
                loadProducts()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update product"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            try {
                db.collection("products").document(productId).delete().await()
                _successMessage.value = "Product deleted"
                _products.value = _products.value.filter { it.id != productId }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to delete product"
            }
        }
    }

    fun loadOrders() {
        viewModelScope.launch {
            _isLoadingOrders.value = true
            try {
                _orders.value = repository.getOrders()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load orders"
            } finally {
                _isLoadingOrders.value = false
            }
        }
    }

    fun markOrderCompleted(orderId: String) {
        viewModelScope.launch {
            try {
                repository.updateOrderStatus(orderId, "completed")
                _orders.value = _orders.value.map {
                    if (it.orderId == orderId) it.copy(status = "completed") else it
                }
                _successMessage.value = "Order marked as completed"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update order"
            }
        }
    }

    fun uploadProductImage(uri: Uri, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _isUploadingImage.value = true
            try {
                val url = repository.uploadProductImage(uri)
                onResult(url)
            } catch (e: Exception) {
                _errorMessage.value = "Image upload failed: ${e.message}"
                onResult(null)
            } finally {
                _isUploadingImage.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
