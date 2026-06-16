package com.example.myrecipes.ui.form

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myrecipes.data.GeminiParser
import com.example.myrecipes.data.ParseResult
import com.example.myrecipes.data.Recipe
import com.example.myrecipes.data.RecipeStore
import com.example.myrecipes.data.SettingsStore
import com.example.myrecipes.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeFormScreen(
    recipeId: String?,
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val recipeStore = remember { RecipeStore(context) }
    val settingsStore = remember { SettingsStore(context) }
    val apiKey = remember { settingsStore.getApiKey() }

    val isEditing = recipeId != null
    var activeMode by remember { mutableStateOf("manual") } // "manual" or "parse"

    // Form States
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var image by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Dinner") }
    var difficulty by remember { mutableStateOf("Medium") }
    var servings by remember { mutableStateOf("4") }
    var prepTime by remember { mutableStateOf("10") }
    var cookTime by remember { mutableStateOf("20") }
    var calories by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    
    val ingredients = remember { mutableStateListOf("") }
    val steps = remember { mutableStateListOf("") }

    // Parser States
    var url by remember { mutableStateOf("") }
    var rawText by remember { mutableStateOf("") }
    var isParsing by remember { mutableStateOf(false) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var isCorsRestricted by remember { mutableStateOf(false) }

    // Load recipe if editing
    LaunchedEffect(recipeId) {
        if (recipeId != null) {
            val r = recipeStore.getById(recipeId)
            if (r != null) {
                title = r.title
                description = r.description
                image = r.image
                category = r.category
                difficulty = r.difficulty
                servings = r.servings.toString()
                prepTime = r.prepTime.toString()
                cookTime = r.cookTime.toString()
                calories = r.calories?.toString() ?: ""
                carbs = r.carbs?.toString() ?: ""
                protein = r.protein?.toString() ?: ""
                fat = r.fat?.toString() ?: ""
                tagsInput = r.tags.joinToString(", ")
                ingredients.clear()
                ingredients.addAll(r.ingredients)
                steps.clear()
                steps.addAll(r.steps)
            }
        } else {
            if (ingredients.isEmpty()) ingredients.add("")
            if (steps.isEmpty()) steps.add("")
        }
    }

    fun loadParsedRecipe(parsed: Recipe) {
        title = parsed.title
        description = parsed.description
        image = parsed.image
        category = parsed.category
        difficulty = parsed.difficulty
        servings = parsed.servings.toString()
        prepTime = parsed.prepTime.toString()
        cookTime = parsed.cookTime.toString()
        calories = parsed.calories?.toString() ?: ""
        carbs = parsed.carbs?.toString() ?: ""
        protein = parsed.protein?.toString() ?: ""
        fat = parsed.fat?.toString() ?: ""
        tagsInput = parsed.tags.joinToString(", ")
        ingredients.clear()
        ingredients.addAll(parsed.ingredients)
        steps.clear()
        steps.addAll(parsed.steps)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Edit Recipe" else "Add New Recipe",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Mode selector (Manual vs Parse) - only when adding
            if (!isEditing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    ModeTabButton(
                        text = "Manual Entry",
                        icon = "➕",
                        isSelected = activeMode == "manual",
                        onClick = { activeMode = "manual" },
                        modifier = Modifier.weight(1f)
                    )
                    ModeTabButton(
                        text = "Parse from Web / URL",
                        icon = "✨",
                        isSelected = activeMode == "parse",
                        onClick = { activeMode = "parse" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (activeMode == "parse") {
                    // PARSE MODE PANEL
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🌐", fontSize = 18.sp)
                                Text(
                                    "Import Web Recipe",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PrimaryColor
                                )
                            }

                            Text(
                                "Paste the link of any recipe web page. Gemini AI will automatically extract the ingredients, instructions, and save them in separate sections.",
                                fontSize = 13.sp,
                                color = LightTextMuted,
                                lineHeight = 18.sp
                            )

                            OutlinedTextField(
                                value = url,
                                onValueChange = { url = it },
                                placeholder = { Text("https://example.com/recipe-url") },
                                singleLine = true,
                                enabled = !isParsing,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (apiKey.isEmpty()) {
                                val warnBg = if (isSystemInDarkTheme()) DarkPrimaryLight else LightPrimaryLight
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = warnBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryColor.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "⚠️ No API Key set in Settings. Tapping parse will load a simulator demo recipe (e.g. lasagna). Add your API key in the top-right settings to run real queries.",
                                        fontSize = 12.sp,
                                        color = PrimaryColor,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isParsing = true
                                        parseError = null
                                        isCorsRestricted = false
                                        when (val result = GeminiParser.parseFromUrl(url, apiKey)) {
                                            is ParseResult.Success -> {
                                                loadParsedRecipe(result.recipe)
                                                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                                activeMode = "manual"
                                            }
                                            is ParseResult.Error -> {
                                                if (result.errorType == "CORS_RESTRICTION") {
                                                    isCorsRestricted = true
                                                }
                                                parseError = result.message
                                            }
                                        }
                                        isParsing = false
                                    }
                                },
                                enabled = !isParsing && url.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                if (isParsing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI Parsing Webpage...", color = Color.White)
                                } else {
                                    Text("✨ Extract Recipe Details", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // CORS text pasting fallback
                    AnimatedVisibility(visible = isCorsRestricted) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("📄", fontSize = 18.sp)
                                    Text(
                                        "Bypass: Paste Page Text",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = PrimaryColor
                                    )
                                }

                                Text(
                                    "The connection was blocked or timed out. You can open the recipe in a browser, copy all page text (Select All -> Copy), and paste it below!",
                                    fontSize = 13.sp,
                                    color = LightTextMuted,
                                    lineHeight = 18.sp
                                )

                                OutlinedTextField(
                                    value = rawText,
                                    onValueChange = { rawText = it },
                                    placeholder = { Text("Paste the copied recipe website content here...") },
                                    minLines = 5,
                                    enabled = !isParsing,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryColor,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            isParsing = true
                                            parseError = null
                                            when (val result = GeminiParser.parseFromText(rawText, apiKey)) {
                                                is ParseResult.Success -> {
                                                    loadParsedRecipe(result.recipe)
                                                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                                    isCorsRestricted = false
                                                    activeMode = "manual"
                                                }
                                                is ParseResult.Error -> {
                                                    parseError = result.message
                                                }
                                            }
                                            isParsing = false
                                        }
                                    },
                                    enabled = !isParsing && rawText.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    if (isParsing) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                    } else {
                                        Text("✨ Parse Copied Text", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Parse Error Message
                    if (parseError != null && !isCorsRestricted) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DangerLightColor,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DangerColor.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Error parsing: $parseError",
                                color = DangerColor,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    // MANUAL FORM ENTRY
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Basic Info", fontWeight = FontWeight.Bold, color = PrimaryColor, fontSize = 15.sp)

                            FormTextField(label = "Recipe Title", value = title, onValueChange = { title = it }, placeholder = "e.g. Grandma's Famous Lasagna")

                            FormTextField(label = "Description", value = description, onValueChange = { description = it }, placeholder = "Tell something about this recipe...", singleLine = false)

                            FormTextField(label = "Cover Image URL", value = image, onValueChange = { image = it }, placeholder = "https://images.unsplash.com/photo-...")

                            // Category & Difficulty Rows
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Category", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LightTextMuted, modifier = Modifier.padding(bottom = 6.dp))
                                    val categoriesList = listOf("Breakfast", "Lunch", "Dinner", "Dessert", "Snack", "Other")
                                    DropdownSelector(
                                        selected = category,
                                        options = categoriesList,
                                        onSelected = { category = it }
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Difficulty", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = LightTextMuted, modifier = Modifier.padding(bottom = 6.dp))
                                    val difficulties = listOf("Easy", "Medium", "Hard")
                                    DropdownSelector(
                                        selected = difficulty,
                                        options = difficulties,
                                        onSelected = { difficulty = it }
                                    )
                                }
                            }

                            // Metadata Row
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FormTextField(
                                    label = "Servings",
                                    value = servings,
                                    onValueChange = { servings = it },
                                    placeholder = "4",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                FormTextField(
                                    label = "Prep Time (min)",
                                    value = prepTime,
                                    onValueChange = { prepTime = it },
                                    placeholder = "10",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                FormTextField(
                                    label = "Cook Time (min)",
                                    value = cookTime,
                                    onValueChange = { cookTime = it },
                                    placeholder = "20",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Nutrition & Tags Section
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Nutrition & Tags", fontWeight = FontWeight.Bold, color = PrimaryColor, fontSize = 15.sp)

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FormTextField(
                                    label = "Calories (kcal)",
                                    value = calories,
                                    onValueChange = { calories = it },
                                    placeholder = "350",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                FormTextField(
                                    label = "Protein (g)",
                                    value = protein,
                                    onValueChange = { protein = it },
                                    placeholder = "25",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FormTextField(
                                    label = "Carbs (g)",
                                    value = carbs,
                                    onValueChange = { carbs = it },
                                    placeholder = "30",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                FormTextField(
                                    label = "Fat (g)",
                                    value = fat,
                                    onValueChange = { fat = it },
                                    placeholder = "12",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            FormTextField(
                                label = "Tags (comma-separated)",
                                value = tagsInput,
                                onValueChange = { tagsInput = it },
                                placeholder = "e.g. Vegetarian, Gluten-Free, Spicy"
                            )
                        }
                    }

                    // Ingredients Section
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ingredients", fontWeight = FontWeight.Bold, color = PrimaryColor, fontSize = 15.sp)
                                TextButton(
                                    onClick = { ingredients.add("") },
                                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryColor)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ingredients.forEachIndexed { index, ing ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = ing,
                                            onValueChange = { ingredients[index] = it },
                                            placeholder = { Text("Ingredient ${index + 1}") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryColor,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )

                                        IconButton(
                                            onClick = {
                                                if (ingredients.size > 1) {
                                                    ingredients.removeAt(index)
                                                } else {
                                                    ingredients[0] = ""
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerColor)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Instructions Section
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cooking Instructions", fontWeight = FontWeight.Bold, color = PrimaryColor, fontSize = 15.sp)
                                TextButton(
                                    onClick = { steps.add("") },
                                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryColor)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Step", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                steps.forEachIndexed { index, step ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "${index + 1}.",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LightTextLight,
                                            modifier = Modifier.padding(top = 12.dp)
                                        )

                                        OutlinedTextField(
                                            value = step,
                                            onValueChange = { steps[index] = it },
                                            placeholder = { Text("Describe step ${index + 1}...") },
                                            minLines = 2,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryColor,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )

                                        IconButton(
                                            onClick = {
                                                if (steps.size > 1) {
                                                    steps.removeAt(index)
                                                } else {
                                                    steps[0] = ""
                                                }
                                            },
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerColor)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Save Button
                    Button(
                        onClick = {
                            val cleanIngredients = ingredients.filter { it.trim().isNotEmpty() }
                            val cleanSteps = steps.filter { it.trim().isNotEmpty() }

                            if (title.trim().isEmpty()) {
                                Toast.makeText(context, "Please enter a recipe title", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (cleanIngredients.isEmpty()) {
                                Toast.makeText(context, "Please add at least one ingredient", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (cleanSteps.isEmpty()) {
                                Toast.makeText(context, "Please add at least one step", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val newRecipe = Recipe(
                                id = recipeId ?: "recipe-${System.currentTimeMillis()}",
                                title = title,
                                description = description,
                                image = if (image.trim().isEmpty()) "https://images.unsplash.com/photo-1495521821757-a1efb6729352?auto=format&fit=crop&w=800&q=80" else image.trim(),
                                servings = servings.toIntOrNull() ?: 4,
                                prepTime = prepTime.toIntOrNull() ?: 10,
                                cookTime = cookTime.toIntOrNull() ?: 20,
                                difficulty = difficulty,
                                category = category,
                                ingredients = cleanIngredients,
                                steps = cleanSteps,
                                calories = calories.toIntOrNull(),
                                carbs = carbs.toIntOrNull(),
                                protein = protein.toIntOrNull(),
                                fat = fat.toIntOrNull(),
                                tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            )

                            coroutineScope.launch {
                                if (isEditing) {
                                    recipeStore.update(newRecipe.id, newRecipe)
                                } else {
                                    recipeStore.add(newRecipe)
                                }
                                onSave()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = if (isEditing) "Save Changes" else "Create Recipe",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModeTabButton(
    text: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(top = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(icon, fontSize = 16.sp)
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) PrimaryColor else LightTextMuted
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(if (isSelected) PrimaryColor else Color.Transparent)
        )
    }
}

@Composable
fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = LightTextMuted
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
