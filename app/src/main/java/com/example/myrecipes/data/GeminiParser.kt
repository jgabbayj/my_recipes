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
        tags = listOf("Italian", "Pasta", "Beef", "Cheese"),
        ingredientsEnglish = listOf("lasagna noodles", "ground beef", "italian sausage", "onion", "garlic", "crushed tomatoes", "tomato paste", "dried oregano", "dried basil", "ricotta cheese", "egg", "parmesan cheese", "mozzarella cheese")
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
        tags = listOf("Mexican", "Tacos", "Beef", "Spicy", "Quick"),
        ingredientsEnglish = listOf("ground beef", "taco seasoning", "water", "corn tortillas", "white onion", "fresh cilantro", "lime", "salsa", "hot sauce")
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
                return@withContext ParseResult.Success(MOCK_LASAGNA.copy(link = url), "Recipe parsed successfully using simulator.")
            } else if (mockKey == "tacos") {
                return@withContext ParseResult.Success(MOCK_TACOS.copy(link = url), "Recipe parsed successfully using simulator.")
            }
            return@withContext ParseResult.Success(MOCK_LASAGNA.copy(link = url), "No Gemini API key provided. Loaded demo lasagna recipe.")
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

            val result = parseFromText(contentToParse, apiKey)
            if (result is ParseResult.Success) {
                val defaultPlaceholder = "https://images.unsplash.com/photo-1495521821757-a1efb6729352?auto=format&fit=crop&w=800&q=80"
                val extractedImage = if (!isYouTube) {
                    extractImageFromHtml(htmlContent, url)
                } else {
                    extractYouTubeThumbnail(url)
                }
                
                val finalImage = if (!extractedImage.isNullOrEmpty()) {
                    extractedImage
                } else if (result.recipe.image.isNotEmpty() && result.recipe.image != defaultPlaceholder) {
                    result.recipe.image
                } else {
                    defaultPlaceholder
                }

                return@withContext ParseResult.Success(
                    result.recipe.copy(
                        link = url,
                        image = finalImage
                    ),
                    result.message
                )
            }
            return@withContext result
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
                  ],
                  "ingredientsEnglish": [
                    "Hidden list of the exact same ingredients from 'ingredients' field, translated to English, without quantities or extra comments, just the base ingredient name in English, e.g. ['flour', 'salt', 'olive oil'] (array of strings)"
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
                
                val ingredientsEnglishArray = recipeData.optJSONArray("ingredientsEnglish")
                val ingredientsEnglish = mutableListOf<String>()
                if (ingredientsEnglishArray != null) {
                    for (i in 0 until ingredientsEnglishArray.length()) {
                        ingredientsEnglish.add(ingredientsEnglishArray.getString(i))
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
                    tags = tags,
                    ingredientsEnglish = ingredientsEnglish
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

    private fun findImageInJson(json: Any): String? {
        when (json) {
            is JSONObject -> {
                if (json.has("image")) {
                    val imgVal = json.get("image")
                    val url = extractImageUrlFromSchemaValue(imgVal)
                    if (url != null) return url
                }
                if (json.optString("@type") == "ImageObject" && json.has("url")) {
                    val urlVal = json.optString("url")
                    if (urlVal.isNotEmpty()) return urlVal
                }
                for (key in json.keys()) {
                    val subVal = json.get(key)
                    val found = findImageInJson(subVal)
                    if (found != null) return found
                }
            }
            is JSONArray -> {
                for (i in 0 until json.length()) {
                    val subVal = json.get(i)
                    val found = findImageInJson(subVal)
                    if (found != null) return found
                }
            }
        }
        return null
    }

    private fun extractImageUrlFromSchemaValue(value: Any): String? {
        when (value) {
            is String -> {
                if (value.startsWith("http") || value.startsWith("/") || value.startsWith("./") || value.startsWith("../")) {
                    return value
                }
            }
            is JSONObject -> {
                if (value.has("url")) {
                    val urlVal = value.optString("url")
                    if (urlVal.isNotEmpty()) return urlVal
                }
            }
            is JSONArray -> {
                if (value.length() > 0) {
                    val first = value.get(0)
                    return extractImageUrlFromSchemaValue(first)
                }
            }
        }
        return null
    }

    fun extractImageFromHtml(html: String, urlString: String): String? {
        try {
            val baseUri = try { java.net.URI(urlString) } catch (e: Exception) { null }

            fun resolveUrl(url: String): String {
                val cleanedUrl = url.replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'")
                    .trim()
                if (cleanedUrl.startsWith("http")) return cleanedUrl
                if (baseUri != null) {
                    try {
                        return baseUri.resolve(cleanedUrl).toString()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return cleanedUrl
            }

            // 1. Try to find og:image meta tag
            val ogImageRegex = Regex("""<meta[^>]+(?:property|name)\s*=\s*["']og:image["'][^>]+content\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            val match1 = ogImageRegex.find(html)
            if (match1 != null) {
                return resolveUrl(match1.groupValues[1])
            }

            val ogImageRegex2 = Regex("""<meta[^>]+content\s*=\s*["']([^"']+)["'][^>]+(?:property|name)\s*=\s*["']og:image["']""", RegexOption.IGNORE_CASE)
            val match2 = ogImageRegex2.find(html)
            if (match2 != null) {
                return resolveUrl(match2.groupValues[1])
            }

            // 2. Try JSON-LD schema image URL
            val jsonLdRegex = Regex("""<script[^>]+type\s*=\s*["']application/ld\+json["'][^>]*>([\s\S]*?)</script>""", RegexOption.IGNORE_CASE)
            val jsonLdMatches = jsonLdRegex.findAll(html)
            for (match in jsonLdMatches) {
                val jsonText = match.groupValues[1].trim()
                try {
                    val token = jsonText.substring(0, 1)
                    val imgUrl = if (token == "{") {
                        findImageInJson(JSONObject(jsonText))
                    } else if (token == "[") {
                        findImageInJson(JSONArray(jsonText))
                    } else {
                        null
                    }
                    if (imgUrl != null) {
                        return resolveUrl(imgUrl)
                    }
                } catch (e: Exception) {
                    // Fallback to simple regex if JSON parsing fails
                    val schemaImageRegex = Regex("""["']image["']\s*:\s*(?:["']([^"']+)["']|\[\s*["']([^"']+)["']|\{\s*["']@type["']\s*:\s*["']ImageObject["']\s*,\s*["']url["']\s*:\s*["']([^"']+)["'])""", RegexOption.IGNORE_CASE)
                    val schemaMatch = schemaImageRegex.find(jsonText)
                    if (schemaMatch != null) {
                        val url = schemaMatch.groupValues.firstOrNull { it.isNotEmpty() && (it.startsWith("http") || it.startsWith("/") || it.startsWith("./") || it.startsWith("../")) }
                        if (url != null) return resolveUrl(url)
                    }
                }
            }

            // 3. Try to find twitter:image meta tag
            val twitterImageRegex = Regex("""<meta[^>]+(?:property|name)\s*=\s*["']twitter:image["'][^>]+content\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            val match3 = twitterImageRegex.find(html)
            if (match3 != null) {
                return resolveUrl(match3.groupValues[1])
            }

            // 4. Try standard <img> tags with certain class names/attributes
            val imgRegex = Regex("""<img[^>]+src\s*=\s*["']([^"']+\.(?:jpg|jpeg|png|webp)[^"']*)["'][^>]*>""", RegexOption.IGNORE_CASE)
            val imgMatches = imgRegex.findAll(html)
            for (imgMatch in imgMatches) {
                val imgSrc = imgMatch.groupValues[1]
                val fullTag = imgMatch.value
                if (fullTag.contains("recipe", ignoreCase = true) || 
                    fullTag.contains("hero", ignoreCase = true) || 
                    fullTag.contains("cover", ignoreCase = true) || 
                    fullTag.contains("featured", ignoreCase = true) || 
                    fullTag.contains("main", ignoreCase = true) ||
                    fullTag.contains("post-image", ignoreCase = true)
                ) {
                    return resolveUrl(imgSrc)
                }
            }

            // Fallback to first image tag that is absolute or resolves, and doesn't look like icon/logo
            for (imgMatch in imgMatches) {
                val imgSrc = imgMatch.groupValues[1]
                if (!imgSrc.contains("icon", ignoreCase = true) && !imgSrc.contains("logo", ignoreCase = true) && !imgSrc.contains("avatar", ignoreCase = true) && !imgSrc.contains("pixel", ignoreCase = true)) {
                    return resolveUrl(imgSrc)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun extractYouTubeThumbnail(youtubeUrl: String): String? {
        try {
            var videoId: String? = null
            if (youtubeUrl.contains("youtu.be/")) {
                videoId = youtubeUrl.substringAfter("youtu.be/").substringBefore("?").substringBefore("/")
            } else if (youtubeUrl.contains("/shorts/")) {
                videoId = youtubeUrl.substringAfter("/shorts/").substringBefore("?").substringBefore("/")
            } else if (youtubeUrl.contains("v=")) {
                videoId = youtubeUrl.substringAfter("v=").substringBefore("&").substringBefore("/")
            } else if (youtubeUrl.contains("embed/")) {
                videoId = youtubeUrl.substringAfter("embed/").substringBefore("?").substringBefore("/")
            }
            if (!videoId.isNullOrEmpty()) {
                return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}

sealed class ParseResult {
    data class Success(val recipe: Recipe, val message: String) : ParseResult()
    data class Error(val errorType: String, val message: String) : ParseResult()
}
