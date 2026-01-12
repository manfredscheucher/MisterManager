package org.example.project

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Article(
    val id: Int,
    val name: String,
    val brand: String? = null,
    val storageLocationId: Int? = null,
    val abbreviation: String? = null,
    val minimumAmount: Int = 0,
    val notes: String? = null,
    val modified: String? = null,
    val added: String? = null,
    val imageIds: List<Int> = emptyList(),
    @Transient val imagesChanged: Boolean = false
)
