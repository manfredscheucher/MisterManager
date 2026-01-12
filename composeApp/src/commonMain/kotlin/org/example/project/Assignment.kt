package org.example.project

import kotlinx.serialization.Serializable

@Serializable
data class Assignment(
    val articleId: Int,
    val locationId: Int,
    val amount: Int,
    val addedDate: String? = null,
    val expirationDate: String? = null,
    val removedDate: String? = null
)
