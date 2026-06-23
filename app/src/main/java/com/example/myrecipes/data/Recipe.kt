package com.example.myrecipes.data

import org.json.JSONArray
import org.json.JSONObject

data class Recipe(
    val id: String,
    val title: String,
    val description: String,
    val image: String,
    val servings: Int,
    val prepTime: Int,
    val cookTime: Int,
    val difficulty: String,
    val category: String,
    val ingredients: List<String>,
    val steps: List<String>,
    val calories: Int? = null,
    val carbs: Int? = null,
    val protein: Int? = null,
    val fat: Int? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val userId: String = "",
    val ratings: Map<String, Int> = emptyMap(),
    val averageRating: Double = 0.0,
    val numRatings: Int = 0,
    val link: String? = null,
    val ingredientsEnglish: List<String> = emptyList()
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("title", title)
        json.put("description", description)
        json.put("image", image)
        json.put("servings", servings)
        json.put("prepTime", prepTime)
        json.put("cookTime", cookTime)
        json.put("difficulty", difficulty)
        json.put("category", category)
        
        val ingredientsArray = JSONArray()
        ingredients.forEach { ingredientsArray.put(it) }
        json.put("ingredients", ingredientsArray)
        
        val stepsArray = JSONArray()
        steps.forEach { stepsArray.put(it) }
        json.put("steps", stepsArray)
        
        if (calories != null) json.put("calories", calories)
        if (carbs != null) json.put("carbs", carbs)
        if (protein != null) json.put("protein", protein)
        if (fat != null) json.put("fat", fat)
        
        val tagsArray = JSONArray()
        tags.forEach { tagsArray.put(it) }
        json.put("tags", tagsArray)
        
        json.put("createdAt", createdAt)
        json.put("userId", userId)
        
        val ratingsJson = JSONObject()
        ratings.forEach { (k, v) -> ratingsJson.put(k, v) }
        json.put("ratings", ratingsJson)
        json.put("averageRating", averageRating)
        json.put("numRatings", numRatings)
        if (link != null) {
            json.put("link", link)
        }
        
        val ingredientsEnglishArray = JSONArray()
        ingredientsEnglish.forEach { ingredientsEnglishArray.put(it) }
        json.put("ingredientsEnglish", ingredientsEnglishArray)
        
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): Recipe {
            val ingredientsArray = json.getJSONArray("ingredients")
            val ingredientsList = mutableListOf<String>()
            for (i in 0 until ingredientsArray.length()) {
                ingredientsList.add(ingredientsArray.getString(i))
            }
            
            val stepsArray = json.getJSONArray("steps")
            val stepsList = mutableListOf<String>()
            for (i in 0 until stepsArray.length()) {
                stepsList.add(stepsArray.getString(i))
            }
            
            val tagsArray = json.optJSONArray("tags")
            val tagsList = mutableListOf<String>()
            if (tagsArray != null) {
                for (i in 0 until tagsArray.length()) {
                    tagsList.add(tagsArray.getString(i))
                }
            }

            val ratingsMap = mutableMapOf<String, Int>()
            val ratingsObj = json.optJSONObject("ratings")
            if (ratingsObj != null) {
                val keys = ratingsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    ratingsMap[key] = ratingsObj.getInt(key)
                }
            }

            val ingredientsEnglishArray = json.optJSONArray("ingredientsEnglish")
            val ingredientsEnglishList = mutableListOf<String>()
            if (ingredientsEnglishArray != null) {
                for (i in 0 until ingredientsEnglishArray.length()) {
                    ingredientsEnglishList.add(ingredientsEnglishArray.getString(i))
                }
            }
            
            return Recipe(
                id = json.getString("id"),
                title = json.getString("title"),
                description = json.optString("description", ""),
                image = json.optString("image", ""),
                servings = json.optInt("servings", 4),
                prepTime = json.optInt("prepTime", 10),
                cookTime = json.optInt("cookTime", 20),
                difficulty = json.optString("difficulty", "Medium"),
                category = json.optString("category", "Dinner"),
                ingredients = ingredientsList,
                steps = stepsList,
                calories = if (json.has("calories")) json.getInt("calories") else null,
                carbs = if (json.has("carbs")) json.getInt("carbs") else null,
                protein = if (json.has("protein")) json.getInt("protein") else null,
                fat = if (json.has("fat")) json.getInt("fat") else null,
                tags = tagsList,
                createdAt = if (json.has("createdAt")) json.getLong("createdAt") else {
                    val idStr = json.getString("id")
                    if (idStr.startsWith("recipe-")) {
                        idStr.substringAfter("recipe-").toLongOrNull() ?: 0L
                    } else {
                        0L
                    }
                },
                userId = json.optString("userId", ""),
                ratings = ratingsMap,
                averageRating = json.optDouble("averageRating", 0.0),
                numRatings = json.optInt("numRatings", 0),
                link = if (json.has("link") && !json.isNull("link")) json.getString("link") else null,
                ingredientsEnglish = ingredientsEnglishList
            )
        }
    }
}
