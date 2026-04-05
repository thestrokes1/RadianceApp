package com.cristianv.radianceapp.data

import android.net.Uri
import com.cristianv.radianceapp.model.Order
import com.cristianv.radianceapp.model.Product
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun getProducts(): List<Product> {
        return try {
            val snapshot = db.collection("products").get().await()
            snapshot.documents.mapNotNull { doc -> doc.toProduct() }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Error: ${e.message}")
            SampleData.products
        }
    }

    suspend fun getProductById(id: String): Product? {
        return try {
            val doc = db.collection("products").document(id).get().await()
            if (doc.exists()) doc.toProduct() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getProductsByCategory(category: String): List<Product> {
        return try {
            val snapshot = db.collection("products")
                .whereEqualTo("category", category)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc -> doc.toProduct() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateProduct(productId: String, data: Map<String, Any>) {
        db.collection("products").document(productId).update(data).await()
    }

    suspend fun saveOrder(orderData: Map<String, Any>): String {
        val docRef = db.collection("orders").add(orderData).await()
        return docRef.id
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getOrders(): List<Order> {
        return try {
            val snapshot = db.collection("orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.map { doc ->
                val addressMap = doc.get("address") as? Map<*, *>
                val shippingAddress = if (addressMap != null) {
                    val street = addressMap["street"]?.toString() ?: ""
                    val city = addressMap["city"]?.toString() ?: ""
                    val country = addressMap["country"]?.toString() ?: ""
                    listOf(street, city, country).filter { it.isNotBlank() }.joinToString(", ")
                } else ""
                Order(
                    orderId = doc.id,
                    userId = doc.getString("userId") ?: "",
                    userEmail = doc.getString("userEmail") ?: "",
                    items = (doc.get("items") as? List<Map<String, Any>>) ?: emptyList(),
                    subtotal = doc.getDouble("subtotal") ?: 0.0,
                    shipping = doc.getDouble("shipping") ?: 5.99,
                    total = doc.getDouble("total") ?: 0.0,
                    shippingAddress = shippingAddress,
                    status = doc.getString("status") ?: "pending",
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "getOrders error: ${e.message}")
            emptyList()
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        db.collection("orders").document(orderId).update("status", status).await()
    }

    suspend fun uploadProductImage(uri: Uri): String {
        val filename = "${System.currentTimeMillis()}_${uri.lastPathSegment ?: "image"}"
        val ref = storage.reference.child("products/$filename")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toProduct() = Product(
        id = id,
        name = getString("name") ?: "",
        brand = getString("brand") ?: "",
        price = getDouble("price") ?: 0.0,
        originalPrice = getDouble("originalPrice"),
        imageUrl = getString("imageUrl") ?: "",
        category = getString("category") ?: "",
        sizes = (get("sizes") as? List<*>)?.map { it.toString() } ?: emptyList(),
        colors = (get("colors") as? List<*>)?.map { it.toString() } ?: emptyList(),
        colorImages = (get("colorImages") as? Map<*, *>)
            ?.mapKeys { it.key.toString() }
            ?.mapValues { it.value.toString() } ?: emptyMap(),
        rating = getDouble("rating") ?: 0.0,
        reviewCount = getLong("reviewCount") ?: 0L,
        isNew = getBoolean("isNew") ?: false,
        description = getString("description") ?: ""
    )
}
