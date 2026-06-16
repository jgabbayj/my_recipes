package com.example.myrecipes.data

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class RecipeStore(private val context: Context) {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val userId: String
        get() = auth.currentUser?.uid ?: "anonymous"

    private val prefs
        get() = context.getSharedPreferences("recipe_prefs_${userId}", Context.MODE_PRIVATE)

    suspend fun getAll(): List<Recipe> {
        val currentUid = auth.currentUser?.uid ?: return emptyList()
        
        try {
            val snapshot = db.collection("recipes")
                .whereEqualTo("userId", currentUid)
                .get()
                .await()
            
            val list = mutableListOf<Recipe>()
            for (doc in snapshot.documents) {
                list.add(docToRecipe(doc.id, doc.data ?: emptyMap()))
            }
            
            // Mark as initialized so we don't try to load defaults in the future, but don't add any default recipes
            val isInitialized = prefs.getBoolean("is_initialized", false)
            if (!isInitialized) {
                prefs.edit().putBoolean("is_initialized", true).apply()
            }
            
            // Sort by date added descending (newest first)
            return list.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    suspend fun getById(id: String): Recipe? {
        val currentUid = auth.currentUser?.uid ?: return null
        try {
            val doc = db.collection("recipes").document(id).get().await()
            if (doc.exists()) {
                val data = doc.data ?: return null
                if (data["userId"] == currentUid) {
                    return docToRecipe(doc.id, data)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun add(recipe: Recipe): Recipe {
        val currentUid = auth.currentUser?.uid ?: "anonymous"
        val timestamp = System.currentTimeMillis()
        val newId = if (recipe.id.isEmpty() || recipe.id == "null") "recipe-$timestamp" else recipe.id
        val newRecipe = recipe.copy(
            id = newId,
            createdAt = if (recipe.createdAt == 0L || recipe.id.isEmpty() || recipe.id == "null") timestamp else recipe.createdAt
        )
        
        val map = recipeToMap(newRecipe, currentUid)
        try {
            db.collection("recipes").document(newId).set(map).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return newRecipe
    }

    suspend fun update(id: String, updatedRecipe: Recipe): Recipe? {
        val currentUid = auth.currentUser?.uid ?: return null
        val map = recipeToMap(updatedRecipe, currentUid)
        try {
            db.collection("recipes").document(id).set(map).await()
            return updatedRecipe
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun delete(id: String): List<Recipe> {
        try {
            db.collection("recipes").document(id).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return getAll()
    }

    private fun docToRecipe(id: String, data: Map<String, Any>): Recipe {
        @Suppress("UNCHECKED_CAST")
        val ingredients = (data["ingredients"] as? List<String>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val steps = (data["steps"] as? List<String>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val tags = (data["tags"] as? List<String>) ?: emptyList()

        val createdAt = data["createdAt"] as? Long ?: run {
            if (id.startsWith("recipe-")) {
                id.substringAfter("recipe-").toLongOrNull() ?: 0L
            } else {
                0L
            }
        }

        return Recipe(
            id = id,
            title = (data["title"] as? String) ?: "",
            description = (data["description"] as? String) ?: "",
            image = (data["image"] as? String) ?: "",
            servings = (data["servings"] as? Long)?.toInt() ?: 4,
            prepTime = (data["prepTime"] as? Long)?.toInt() ?: 10,
            cookTime = (data["cookTime"] as? Long)?.toInt() ?: 20,
            difficulty = (data["difficulty"] as? String) ?: "Medium",
            category = (data["category"] as? String) ?: "Dinner",
            ingredients = ingredients,
            steps = steps,
            calories = (data["calories"] as? Long)?.toInt(),
            carbs = (data["carbs"] as? Long)?.toInt(),
            protein = (data["protein"] as? Long)?.toInt(),
            fat = (data["fat"] as? Long)?.toInt(),
            tags = tags,
            createdAt = createdAt
        )
    }

    private fun recipeToMap(recipe: Recipe, userId: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "title" to recipe.title,
            "description" to recipe.description,
            "image" to recipe.image,
            "servings" to recipe.servings,
            "prepTime" to recipe.prepTime,
            "cookTime" to recipe.cookTime,
            "difficulty" to recipe.difficulty,
            "category" to recipe.category,
            "ingredients" to recipe.ingredients,
            "steps" to recipe.steps,
            "tags" to recipe.tags,
            "createdAt" to recipe.createdAt
        )
        recipe.calories?.let { map["calories"] = it }
        recipe.carbs?.let { map["carbs"] = it }
        recipe.protein?.let { map["protein"] = it }
        recipe.fat?.let { map["fat"] = it }
        return map
    }
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun getApiKey(): String {
        return prefs.getString("gemini_api_key", "") ?: ""
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString("gemini_api_key", key).apply()
    }
}
