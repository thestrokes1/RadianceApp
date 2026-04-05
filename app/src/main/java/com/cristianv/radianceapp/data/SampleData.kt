package com.cristianv.radianceapp.data

import com.cristianv.radianceapp.model.Product

object SampleData {

    val categories = listOf(
        "All", "Women", "Men", "Kids", "Accessories", "Sale"
    )

    val products = listOf(
        Product(
            id = "1",
            name = "Oversized Linen Blazer",
            brand = "Radiance Studio",
            price = 89.99,
            originalPrice = 129.99,
            imageUrl = "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=400",
            category = "Women",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Beige", "Black", "White"),
            rating = 4.8,
            reviewCount = 124,
            isNew = true,
            description = "Effortlessly chic oversized blazer crafted from premium linen."
        ),
        Product(
            id = "2",
            name = "Slim Fit Chinos",
            brand = "Radiance Men",
            price = 59.99,
            imageUrl = "https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=400",
            category = "Men",
            sizes = listOf("28", "30", "32", "34", "36"),
            colors = listOf("Navy", "Khaki", "Olive"),
            rating = 4.6,
            reviewCount = 89,
            description = "Modern slim fit chinos perfect for any occasion."
        ),
        Product(
            id = "3",
            name = "Floral Wrap Dress",
            brand = "Radiance Studio",
            price = 74.99,
            originalPrice = 99.99,
            imageUrl = "https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=400",
            category = "Women",
            sizes = listOf("XS", "S", "M", "L"),
            colors = listOf("Blue", "Pink", "Green"),
            rating = 4.9,
            reviewCount = 203,
            isNew = true,
            description = "Elegant floral wrap dress for any summer occasion."
        ),
        Product(
            id = "4",
            name = "Classic White Sneakers",
            brand = "Radiance Footwear",
            price = 109.99,
            imageUrl = "https://images.unsplash.com/photo-1549298916-b41d501d3772?w=400",
            category = "Accessories",
            sizes = listOf("36", "37", "38", "39", "40", "41", "42"),
            colors = listOf("White", "Black"),
            rating = 4.7,
            reviewCount = 567,
            description = "Timeless white sneakers that go with everything."
        ),
        Product(
            id = "5",
            name = "Merino Wool Sweater",
            brand = "Radiance Men",
            price = 94.99,
            originalPrice = 119.99,
            imageUrl = "https://images.unsplash.com/photo-1556821840-3a63f15732ce?w=400",
            category = "Men",
            sizes = listOf("S", "M", "L", "XL", "XXL"),
            colors = listOf("Camel", "Grey", "Navy"),
            rating = 4.5,
            reviewCount = 78,
            description = "Luxuriously soft merino wool sweater for cooler days."
        ),
        Product(
            id = "6",
            name = "High Waist Yoga Pants",
            brand = "Radiance Active",
            price = 49.99,
            imageUrl = "https://images.unsplash.com/photo-1506629082955-511b1aa562c8?w=400",
            category = "Women",
            sizes = listOf("XS", "S", "M", "L", "XL"),
            colors = listOf("Black", "Grey", "Purple"),
            rating = 4.8,
            reviewCount = 445,
            isNew = false,
            description = "High performance yoga pants with 4-way stretch fabric."
        )
    )
}
