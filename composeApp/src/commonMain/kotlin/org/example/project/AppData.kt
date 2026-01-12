package org.example.project

import kotlinx.serialization.Serializable

@Serializable
data class AppData(
    val articles: MutableList<Article> = mutableListOf(),
    val locations: MutableList<Location> = mutableListOf(),
    val assignments: MutableList<Assignment> = mutableListOf()
)
