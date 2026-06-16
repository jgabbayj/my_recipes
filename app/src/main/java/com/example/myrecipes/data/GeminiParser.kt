package com.example.myrecipes.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object GeminiParser {

    private val MOCK_LASAGNA = Recipe(
        id = "lasagna",
        title = "Best Ever Homemade Lasagna",
        description = "Classic lasagna made with a rich meat sauce, creamy ricotta mixture, and gooey mozzarella cheese. Layered to perfection and baked until bubbly.",
        image = "https://images.unsplash.com/photo-1574894709920-11b28e7367e3?auto=format&fit=crop&w=800&q=80",
        servings = 8,
        prepTime = 30,
        cookTime = 50,
        difficulty = "Hard",
        category = "Dinner",
        ingredients = listOf(
            "12 Lasagna Noodles",
            "500g Ground Beef",
            "250g Italian Sausage",
            "1 Medium Onion (chopped)",
            "2 Cloves Garlic (minced)",
            "800g Crushed Tomatoes",
            "2 tbsp Tomato Paste",
            "2 tsp Dried Oregano",
            "2 tsp Dried Basil",
            "450g Ricotta Cheese",
            "1 Egg",
            "50g Parmesan Cheese (grated)",
            "400g Mozzarella Cheese (shredded)"
        ),
        steps = listOf(
            "Cook the lasagna noodles in a large pot of boiling salted water according to package directions. Drain and rinse with cold water.",
            "In a large pan, cook the ground beef, sausage, onion, and garlic over medium-high heat until browned. Drain excess fat.",
            "Stir in the crushed tomatoes, tomato paste, dried oregano, dried basil, salt, and pepper. Simmer uncovered for 15-20 minutes, stirring occasionally.",
            "In a bowl, mix together the ricotta cheese, egg, half of the Parmesan cheese, and a pinch of salt.",
            "Preheat oven to 190°C (375°F).",
            "To assemble, spread 1 cup of meat sauce in a 9x13 inch baking dish. Arrange 4 noodles on top. Spread half of the ricotta mixture, then sprinkle with 1/3 of the mozzarella cheese. Repeat layering with sauce, noodles, ricotta, and mozzarella.",
            "Finish with a final layer of noodles, the remaining meat sauce, and the rest of the mozzarella and Parmesan cheese.",
            "Cover with foil (raised slightly so it doesn't touch the cheese) and bake for 25 minutes. Remove foil and bake for an additional 25 minutes until bubbly and golden brown."
        ),
        calories = 650,
        carbs = 42,
        protein = 35,
        fat = 38,
        tags = listOf("Italian", "Pasta", "Beef", "Cheese")
    )

    private val MOCK_TACOS = Recipe(
        id = "tacos",
        title = "Street-Style Beef Tacos",
        description = "Quick and flavorful street tacos with seasoned ground beef, fresh onions, cilantro, and lime juice on warm corn tortillas.",
        image = "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?auto=format&fit=crop&w=800&q=80",
        servings = 4,
        prepTime = 10,
        cookTime = 10,
        difficulty = "Easy",
        category = "Lunch",
        ingredients = listOf(
            "500g Ground Beef",
            "1 packet Taco Seasoning",
            "1/2 cup Water",
            "12 Small Corn Tortillas",
            "1 small White Onion (finely diced)",
            "1/2 cup Fresh Cilantro (chopped)",
            "2 Limes (cut into wedges)",
            "Salsa or Hot Sauce to taste"
        ),
        steps = listOf(
            "Brown the ground beef in a large skillet over medium-high heat until fully cooked. Drain excess fat.",
            "Add the taco seasoning and 1/2 cup water. Bring to a simmer and cook for 3-5 minutes until the sauce thickens and coats the beef.",
            "Warm the corn tortillas on a dry skillet or griddle over medium heat for about 30 seconds on each side until pliable.",
            "Assemble the tacos by placing 2-3 tablespoons of beef in each tortilla.",
            "Top with diced onion, fresh cilantro, a squeeze of fresh lime juice, and salsa. Serve warm."
        ),
        calories = 380,
        carbs = 28,
        protein = 24,
        fat = 18,
        tags = listOf("Mexican", "Tacos", "Beef", "Spicy", "Quick")
    )

    suspend fun parseFromUrl(url: String, apiKey: String): ParseResult = withContext(Dispatchers.IO) {
        val urlLower = url.lowercase()
        var mockKey: String? = null
        if (urlLower.contains("lasagna")) {
            mockKey = "lasagna"
        } else if (urlLower.contains("taco")) {
            mockKey = "tacos"
        }

        // If it's a mock URL or we don't have an API key, use mock parsing for demo
        if (mockKey != null || apiKey.isEmpty()) {
            // Simulate network delay
            kotlinx.coroutines.delay(2000)
            if (mockKey == "lasagna") {
                return@withContext ParseResult.Success(MOCK_LASAGNA, "Recipe parsed successfully using simulator.")
            } else if (mockKey == "tacos") {
                return@withContext ParseResult.Success(MOCK_TACOS, "Recipe parsed successfully using simulator.")
            }
            return@withContext ParseResult.Success(MOCK_LASAGNA, "No Gemini API key provided. Loaded demo lasagna recipe.")
        }

        try {
            // Fetch HTML content directly (Android has no CORS restriction)
            val htmlContent = try {
                fetchHtmlFromUrl(url)
            } catch (e: Exception) {
                // If direct fetch fails (e.g. timeout or blocked), fallback to manual text paste
                return@withContext ParseResult.Error(
                    "CORS_RESTRICTION",
                    "Unable to fetch the webpage directly (connection failed: ${e.message}). Please copy the recipe page content and paste it in the text box below!"
                )
            }

            val isYouTube = urlLower.contains("youtube.com") || urlLower.contains("youtu.be")
            val contentToParse = if (isYouTube) {
                val desc = extractYouTubeDescription(htmlContent)
                if (desc.isEmpty()) {
                    htmlContent
                } else {
                    desc
                }
            } else {
                htmlContent
            }

            return@withContext parseFromText(contentToParse, apiKey)
        } catch (error: Exception) {
            error.printStackTrace()
            return@withContext ParseResult.Error(
                "API_ERROR",
                "Failed to parse recipe: ${error.message ?: "Unknown error"}. Please make sure the URL is correct and contains a recipe."
            )
        }
    }

    fun extractYouTubeDescription(html: String): String {
        // Try shortDescription first
        val marker = "\"shortDescription\":\""
        var index = html.indexOf(marker)
        if (index != -1) {
            val start = index + marker.length
            return parseEscapedJsonString(html, start)
        }
        
        // Try description simpleText fallback
        val simpleTextMarker = "\"description\":{\"simpleText\":\""
        index = html.indexOf(simpleTextMarker)
        if (index != -1) {
            val start = index + simpleTextMarker.length
            return parseEscapedJsonString(html, start)
        }
        
        return ""
    }

    fun parseEscapedJsonString(html: String, start: Int): String {
        val sb = java.lang.StringBuilder()
        var i = start
        while (i < html.length) {
            val c = html[i]
            if (c == '"') {
                // Check if it's escaped
                if (i > start && html[i - 1] == '\\') {
                    if (sb.isNotEmpty() && sb.last() == '\\') {
                        sb.deleteCharAt(sb.length - 1)
                    }
                    sb.append(c)
                } else {
                    break
                }
            } else {
                sb.append(c)
            }
            i++
        }
        return sb.toString()
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    private fun cleanHtml(html: String): String {
        var clean = html
        // remove scripts
        clean = clean.replace(Regex("<script\\b[^<]*(?:(?!</script>)<[^<]*)*</script>", RegexOption.IGNORE_CASE), "")
        // remove styles
        clean = clean.replace(Regex("<style\\b[^<]*(?:(?!</style>)<[^<]*)*</style>", RegexOption.IGNORE_CASE), "")
        // remove headers
        clean = clean.replace(Regex("<header\\b[^<]*(?:(?!</header>)<[^<]*)*</header>", RegexOption.IGNORE_CASE), "")
        // remove footers
        clean = clean.replace(Regex("<footer\\b[^<]*(?:(?!</footer>)<[^<]*)*</footer>", RegexOption.IGNORE_CASE), "")
        // remove navs
        clean = clean.replace(Regex("<nav\\b[^<]*(?:(?!</nav>)<[^<]*)*</nav>", RegexOption.IGNORE_CASE), "")
        // strip HTML tags
        clean = clean.replace(Regex("<[^>]+>"), " ")
        // collapse whitespace
        clean = clean.replace(Regex("\\s+"), " ")
        return clean.trim()
    }

    suspend fun parseFromText(text: String, apiKey: String): ParseResult = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) {
            return@withContext ParseResult.Error(
                "NO_API_KEY",
                "Please enter your Gemini API Key in Settings to use the automatic parser."
            )
        }

        try {
            val isHtml = text.contains("<html", ignoreCase = true) ||
                         text.contains("<body", ignoreCase = true) ||
                         text.contains("<div", ignoreCase = true) ||
                         text.contains("<p>", ignoreCase = true)

            val cleanedText = if (isHtml) cleanHtml(text) else text.replace(Regex("\\s+"), " ").trim()

            // Limit text to 30k characters to prevent token overflow
            val truncatedText = if (cleanedText.length > 30000) cleanedText.substring(0, 30000) else cleanedText

            val prompt = """
                You are an expert recipe parser. Your task is to extract recipe information from the provided text or HTML content and return it as a structured JSON object.
                
                The text or HTML content is:
                -----------------
                $truncatedText
                -----------------
        
                Please extract the following fields and return exactly this JSON schema:
                {
                  "title": "Title of the recipe (string)",
                  "description": "A brief summary of the recipe (string)",
                  "image": "URL of the recipe main image if found, otherwise leave empty or use a placeholder (string)",
                  "servings": "Number of servings as an integer, default to 4 if not found (number)",
                  "prepTime": "Preparation time in minutes as an integer, default to 10 if not found (number)",
                  "cookTime": "Cooking time in minutes as an integer, default to 20 if not found (number)",
                  "difficulty": "One of: 'Easy', 'Medium', 'Hard'",
                  "category": "One of: 'Breakfast', 'Lunch', 'Dinner', 'Dessert', 'Snack', 'Other'",
                  "ingredients": [
                    "List of ingredients with quantities, e.g., '2 cups flour', '1 tsp salt' (array of strings)"
                  ],
                  "steps": [
                    "Step-by-step instructions. Break down long paragraphs into clear, single actions. Ensure they are in sequential order (array of strings)"
                  ],
                  "calories": "Estimated total calories per serving as an integer. If not present in text, estimate based on ingredients (number, optional)",
                  "carbs": "Estimated grams of carbohydrates per serving as an integer. Estimate if not present (number, optional)",
                  "protein": "Estimated grams of protein per serving as an integer. Estimate if not present (number, optional)",
                  "fat": "Estimated grams of fat per serving as an integer. Estimate if not present (number, optional)",
                  "tags": [
                    "List of relevant tags/labels, e.g. 'Gluten-Free', 'Vegetarian', 'Quick', 'Spicy' (array of strings)"
                  ]
                }
        
                Make sure you only return the raw JSON object, no Markdown wrappers, no comments.
            """.trimIndent()

            // Call Gemini REST API
            val responseText = callGeminiApi(prompt, apiKey)
            
            try {
                // Find start and end of JSON in case Gemini included markdown wrappers despite instructions
                val cleanedText = cleanJsonString(responseText)
                val recipeData = JSONObject(cleanedText)
                
                val title = recipeData.optString("title", "Parsed Recipe")
                val description = recipeData.optString("description", "Parsed using Gemini AI")
                val image = recipeData.optString("image", "https://images.unsplash.com/photo-1495521821757-a1efb6729352?auto=format&fit=crop&w=800&q=80")
                val servings = recipeData.optInt("servings", 4)
                val prepTime = recipeData.optInt("prepTime", 10)
                val cookTime = recipeData.optInt("cookTime", 20)
                
                val diff = recipeData.optString("difficulty", "Medium")
                val difficulty = if (listOf("Easy", "Medium", "Hard").contains(diff)) diff else "Medium"
                
                val cat = recipeData.optString("category", "Dinner")
                val category = if (listOf("Breakfast", "Lunch", "Dinner", "Dessert", "Snack", "Other").contains(cat)) cat else "Dinner"
                
                val ingredientsArray = recipeData.optJSONArray("ingredients")
                val ingredients = mutableListOf<String>()
                if (ingredientsArray != null) {
                    for (i in 0 until ingredientsArray.length()) {
                        ingredients.add(ingredientsArray.getString(i))
                    }
                }
                
                val stepsArray = recipeData.optJSONArray("steps")
                val steps = mutableListOf<String>()
                if (stepsArray != null) {
                    for (i in 0 until stepsArray.length()) {
                        steps.add(stepsArray.getString(i))
                    }
                }
                
                val calories = if (recipeData.has("calories")) recipeData.getInt("calories") else null
                val carbs = if (recipeData.has("carbs")) recipeData.getInt("carbs") else null
                val protein = if (recipeData.has("protein")) recipeData.getInt("protein") else null
                val fat = if (recipeData.has("fat")) recipeData.getInt("fat") else null
                
                val tagsArray = recipeData.optJSONArray("tags")
                val tags = mutableListOf<String>()
                if (tagsArray != null) {
                    for (i in 0 until tagsArray.length()) {
                        tags.add(tagsArray.getString(i))
                    }
                }
                
                val sanitizedRecipe = Recipe(
                    id = "recipe-${System.currentTimeMillis()}",
                    title = title,
                    description = description,
                    image = if (image.isEmpty()) "https://images.unsplash.com/photo-1495521821757-a1efb6729352?auto=format&fit=crop&w=800&q=80" else image,
                    servings = servings,
                    prepTime = prepTime,
                    cookTime = cookTime,
                    difficulty = difficulty,
                    category = category,
                    ingredients = ingredients,
                    steps = steps,
                    calories = calories,
                    carbs = carbs,
                    protein = protein,
                    fat = fat,
                    tags = tags
                )

                return@withContext ParseResult.Success(sanitizedRecipe, "Recipe parsed successfully using Gemini AI!")
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext ParseResult.Error(
                    "JSON_PARSE_ERROR",
                    "The AI returned a response, but it could not be parsed as structured recipe data. Please try copy-pasting the text instead."
                )
            }
        } catch (error: Exception) {
            error.printStackTrace()
            return@withContext ParseResult.Error(
                "API_ERROR",
                "Gemini API error: ${error.message ?: "Please check your API key and network connection."}"
            )
        }
    }

    private fun fetchHtmlFromUrl(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            throw Exception("Server returned code $responseCode")
        }
    }

    private fun callGeminiApi(prompt: String, apiKey: String): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")

        // Build request body
        val requestBody = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        val partObj = JSONObject()
        partObj.put("text", prompt)
        partsArray.put(partObj)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        requestBody.put("contents", contentsArray)

        val genConfig = JSONObject()
        genConfig.put("responseMimeType", "application/json")
        requestBody.put("generationConfig", genConfig)

        connection.outputStream.use { os ->
            os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
        }

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val responseJson = JSONObject(responseText)
            val candidate = responseJson.getJSONArray("candidates").getJSONObject(0)
            return candidate.getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        } else {
            val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
            throw Exception("API call failed (code $responseCode): $errorText")
        }
    }

    private fun cleanJsonString(rawText: String): String {
        var clean = rawText.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }
}

sealed class ParseResult {
    data class Success(val recipe: Recipe, val message: String) : ParseResult()
    data class Error(val errorType: String, val message: String) : ParseResult()
}
