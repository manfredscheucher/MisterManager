package org.example.project

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val id: Int,
    val name: String,
    val notes: String? = null,
    val imageIds: List<Int> = emptyList()
)
