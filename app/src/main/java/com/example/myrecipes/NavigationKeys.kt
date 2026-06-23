package com.example.myrecipes

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data class RecipeDetail(val recipeId: String) : NavKey
@Serializable data class RecipeForm(val recipeId: String? = null, val initialUrl: String? = null) : NavKey
@Serializable data class CookMode(val recipeId: String) : NavKey
