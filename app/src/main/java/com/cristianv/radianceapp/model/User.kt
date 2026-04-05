package com.cristianv.radianceapp.model

data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
