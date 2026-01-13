package org.example.project

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Assignment(
    val id: UInt,
    val articleId: UInt,
    val locationId: UInt,
    val amount: UInt,
    val addedDate: String? = null,
    val expirationDate: String? = null,
    @SerialName("removedDate") // TODO: Remove this annotation in a future version when all data has been migrated from "removedDate" to "consumedDate"
    val consumedDate: String? = null
)
