package com.example.myrecipes.data

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject

class RecipeStore(private val context: Context) {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val userId: String
        get() = auth.currentUser?.uid ?: "anonymous"

    private val prefs
        get() = context.getSharedPreferences("recipe_prefs_${userId}", Context.MODE_PRIVATE)

    private fun saveRecipesToCache(recipes: List<Recipe>) {
        val array = JSONArray()
        for (recipe in recipes) {
            array.put(recipe.toJsonObject())
        }
        prefs.edit().putString("cached_recipes", array.toString()).apply()
    }

    private fun getRecipesFromCache(): List<Recipe> {
        val jsonStr = prefs.getString("cached_recipes", null) ?: return emptyList()
        try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<Recipe>()
            for (i in 0 until array.length()) {
                list.add(Recipe.fromJsonObject(array.getJSONObject(i)))
            }
            return list.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    fun clearCache() {
        prefs.edit()
            .remove("cached_recipes")
            .remove("has_synced")
            .apply()
    }

    suspend fun getAll(): List<Recipe> {
        val currentUid = auth.currentUser?.uid ?: return emptyList()
        
        val hasSynced = prefs.getBoolean("has_synced", false)
        if (hasSynced) {
            return getRecipesFromCache()
        }
        
        try {
            val userDoc = db.collection("users").document(currentUid).get().await()
            val savedIds = (userDoc.get("savedRecipes") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            
            if (savedIds.isEmpty()) {
                saveRecipesToCache(emptyList())
                prefs.edit().putBoolean("has_synced", true).apply()
                return emptyList()
            }
            
            val list = mutableListOf<Recipe>()
            coroutineScope {
                val deferreds = savedIds.map { recipeId ->
                    async {
                        try {
                            val doc = db.collection("recipes").document(recipeId).get().await()
                            if (doc.exists()) {
                                docToRecipe(doc.id, doc.data ?: emptyMap())
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }
                }
                list.addAll(deferreds.awaitAll().filterNotNull())
            }
            
            val sortedList = list.sortedByDescending { it.createdAt }
            saveRecipesToCache(sortedList)
            
            prefs.edit()
                .putBoolean("is_initialized", true)
                .putBoolean("has_synced", true)
                .apply()
            
            return sortedList
        } catch (e: Exception) {
            e.printStackTrace()
            return getRecipesFromCache()
        }
    }

    suspend fun getGlobalRecipes(): List<Recipe> {
        try {
            val snapshot = db.collection("recipes")
                .get()
                .await()
            val list = mutableListOf<Recipe>()
            for (doc in snapshot.documents) {
                list.add(docToRecipe(doc.id, doc.data ?: emptyMap()))
            }
            return list.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    suspend fun getById(id: String): Recipe? {
        val cachedRecipe = getRecipesFromCache().find { it.id == id }
        if (cachedRecipe != null) {
            return cachedRecipe
        }

        try {
            val doc = db.collection("recipes")
                .document(id)
                .get()
                .await()
            if (doc.exists()) {
                val data = doc.data ?: return null
                return docToRecipe(doc.id, data)
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
            userId = currentUid,
            createdAt = if (recipe.createdAt == 0L || recipe.id.isEmpty() || recipe.id == "null") timestamp else recipe.createdAt
        )
        
        val currentCached = getRecipesFromCache().toMutableList()
        currentCached.removeAll { it.id == newId }
        currentCached.add(0, newRecipe)
        saveRecipesToCache(currentCached.sortedByDescending { it.createdAt })
        
        val map = recipeToMap(newRecipe, currentUid)
        try {
            db.collection("recipes")
                .document(newId)
                .set(map)
                .await()
            
            db.collection("users")
                .document(currentUid)
                .update("savedRecipes", com.google.firebase.firestore.FieldValue.arrayUnion(newId))
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                db.collection("users")
                    .document(currentUid)
                    .set(
                        mapOf("savedRecipes" to listOf(newId)),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                    .await()
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
        return newRecipe
    }

    suspend fun update(id: String, updatedRecipe: Recipe): Recipe? {
        val currentUid = auth.currentUser?.uid ?: return null
        val finalRecipe = updatedRecipe.copy(userId = updatedRecipe.userId.ifEmpty { currentUid })
        
        val currentCached = getRecipesFromCache().toMutableList()
        val index = currentCached.indexOfFirst { it.id == id }
        if (index != -1) {
            currentCached[index] = finalRecipe
        } else {
            currentCached.add(finalRecipe)
        }
        saveRecipesToCache(currentCached.sortedByDescending { it.createdAt })

        val map = recipeToMap(finalRecipe, finalRecipe.userId)
        try {
            db.collection("recipes")
                .document(id)
                .set(map)
                .await()
            return finalRecipe
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun delete(id: String): List<Recipe> {
        val currentUid = auth.currentUser?.uid ?: return emptyList()
        
        val currentCached = getRecipesFromCache().toMutableList()
        currentCached.removeAll { it.id == id }
        saveRecipesToCache(currentCached.sortedByDescending { it.createdAt })

        try {
            db.collection("users")
                .document(currentUid)
                .update("savedRecipes", com.google.firebase.firestore.FieldValue.arrayRemove(id))
                .await()
            
            val recipe = getById(id)
            if (recipe != null && recipe.userId == currentUid) {
                db.collection("recipes")
                    .document(id)
                    .delete()
                    .await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return currentCached
    }

    suspend fun isSaved(recipeId: String): Boolean {
        val currentUid = auth.currentUser?.uid ?: return false
        return try {
            val userDoc = db.collection("users").document(currentUid).get().await()
            val savedIds = (userDoc.get("savedRecipes") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            savedIds.contains(recipeId)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveRecipe(recipeId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        try {
            db.collection("users")
                .document(currentUid)
                .update("savedRecipes", com.google.firebase.firestore.FieldValue.arrayUnion(recipeId))
                .await()
            
            val recipe = getById(recipeId)
            if (recipe != null) {
                val currentCached = getRecipesFromCache().toMutableList()
                currentCached.removeAll { it.id == recipeId }
                currentCached.add(0, recipe)
                saveRecipesToCache(currentCached.sortedByDescending { it.createdAt })
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                db.collection("users")
                    .document(currentUid)
                    .set(
                        mapOf("savedRecipes" to listOf(recipeId)),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                    .await()
                val recipe = getById(recipeId)
                if (recipe != null) {
                    val currentCached = getRecipesFromCache().toMutableList()
                    currentCached.removeAll { it.id == recipeId }
                    currentCached.add(0, recipe)
                    saveRecipesToCache(currentCached.sortedByDescending { it.createdAt })
                }
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    suspend fun unsaveRecipe(recipeId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        try {
            db.collection("users")
                .document(currentUid)
                .update("savedRecipes", com.google.firebase.firestore.FieldValue.arrayRemove(recipeId))
                .await()
            
            val currentCached = getRecipesFromCache().toMutableList()
            currentCached.removeAll { it.id == recipeId }
            saveRecipesToCache(currentCached.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun rateRecipe(recipeId: String, rating: Int) {
        val currentUid = auth.currentUser?.uid ?: return
        if (rating < 1 || rating > 5) return

        try {
            db.collection("users")
                .document(currentUid)
                .set(
                    mapOf("ratings" to mapOf(recipeId to rating)),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()

            val docRef = db.collection("recipes").document(recipeId)
            val doc = docRef.get().await()
            if (doc.exists()) {
                val data = doc.data ?: emptyMap()
                @Suppress("UNCHECKED_CAST")
                val ratings = (data["ratings"] as? Map<String, Long>)?.mapValues { it.value.toInt() }?.toMutableMap() ?: mutableMapOf()
                
                ratings[currentUid] = rating
                val numRatings = ratings.size
                val averageRating = ratings.values.sum().toDouble() / numRatings

                docRef.update(
                    mapOf(
                        "ratings" to ratings,
                        "averageRating" to averageRating,
                        "numRatings" to numRatings
                    )
                ).await()

                val currentCached = getRecipesFromCache().toMutableList()
                val index = currentCached.indexOfFirst { it.id == recipeId }
                if (index != -1) {
                    val updatedRecipe = currentCached[index].copy(
                        ratings = ratings,
                        averageRating = averageRating,
                        numRatings = numRatings
                    )
                    currentCached[index] = updatedRecipe
                    saveRecipesToCache(currentCached.sortedByDescending { it.createdAt })
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

        val userId = data["userId"] as? String ?: ""
        @Suppress("UNCHECKED_CAST")
        val ratings = (data["ratings"] as? Map<String, Long>)?.mapValues { it.value.toInt() } ?: emptyMap()
        val averageRating = (data["averageRating"] as? Number)?.toDouble() ?: 0.0
        val numRatings = (data["numRatings"] as? Number)?.toInt() ?: 0

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
            createdAt = createdAt,
            userId = userId,
            ratings = ratings,
            averageRating = averageRating,
            numRatings = numRatings
        )
    }

    private fun recipeToMap(recipe: Recipe, userId: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId.ifEmpty { recipe.userId },
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
            "createdAt" to recipe.createdAt,
            "ratings" to recipe.ratings,
            "averageRating" to recipe.averageRating,
            "numRatings" to recipe.numRatings
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

    fun getTheme(): String {
        return prefs.getString("theme", "system") ?: "system"
    }

    fun saveTheme(theme: String) {
        prefs.edit().putString("theme", theme).apply()
    }

    fun registerListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
