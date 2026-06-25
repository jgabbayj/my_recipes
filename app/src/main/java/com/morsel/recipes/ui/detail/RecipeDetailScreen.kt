package com.morsel.recipes.ui.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morsel.recipes.data.Recipe
import com.morsel.recipes.data.RecipeStore
import com.morsel.recipes.data.IngredientConverter
import com.morsel.recipes.theme.*
import com.morsel.recipes.ui.dashboard.NetworkImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

@Composable
fun RecipeDetailScreen(
    recipeId: String,
    onBack: () -> Unit,
    onEditRecipe: (String) -> Unit,
    onStartCooking: (String) -> Unit,
    onDeleteRecipe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recipeStore = remember { RecipeStore(context) }
    val coroutineScope = rememberCoroutineScope()
    val isDark = isAppInDarkTheme()

    var recipe by remember { mutableStateOf<Recipe?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("ingredients") }
    var creatorUsername by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(recipeId) {
        recipe = recipeStore.getById(recipeId)
    }

    LaunchedEffect(recipe) {
        val r = recipe
        if (r != null && r.userId.isNotEmpty()) {
            try {
                val doc = Firebase.firestore.collection("users").document(r.userId).get().await()
                creatorUsername = doc.getString("username")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val currentRecipe = recipe ?: return
    val totalTime = currentRecipe.prepTime + currentRecipe.cookTime
    val bgColor = if (isDark) DarkBg else LightBg
    val cardBg = if (isDark) DarkCardBg else LightCardBg
    val borderColor = if (isDark) DarkBorder else LightBorder

    Box(modifier = modifier.fillMaxSize().background(bgColor)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            // ── Hero Image ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                NetworkImage(
                    url = currentRecipe.image,
                    modifier = Modifier.fillMaxSize()
                )

                // Bottom gradient for title legibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )

                val currentUid = Firebase.auth.currentUser?.uid ?: ""
                val isCreator = currentRecipe.userId == currentUid || currentRecipe.userId.isEmpty()
                var isSaved by remember { mutableStateOf(false) }

                LaunchedEffect(currentRecipe.id) {
                    isSaved = recipeStore.isSaved(currentRecipe.id)
                }

                // Top bar: back + edit + delete OR save
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back
                    HeroButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    if (isCreator) {
                        // Edit + Delete
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HeroButton(onClick = { onEditRecipe(currentRecipe.id) }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            HeroButton(
                                onClick = { showDeleteConfirm = true },
                                bgColor = Color.Red.copy(alpha = 0.85f)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        // Save / Bookmark button
                        HeroButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (isSaved) {
                                        recipeStore.unsaveRecipe(currentRecipe.id)
                                        isSaved = false
                                    } else {
                                        recipeStore.saveRecipe(currentRecipe.id)
                                        isSaved = true
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = if (isSaved) "❤️" else "🤍",
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                // Category + Title overlay at bottom of image
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category pill
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PrimaryColor)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = currentRecipe.category,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Origin badge
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isCreator) SuccessColor else SecondaryColor)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isCreator) "My Recipe" else "Discovered",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Added by badge
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "By: ${creatorUsername ?: "Anonymous"}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentRecipe.title,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 30.sp
                    )
                }
            }

            // ── Stats Row ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg)
                    .border(width = 0.dp, color = Color.Transparent)
                    .padding(vertical = 16.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatBubble(emoji = "⏱", label = "TOTAL", value = "$totalTime min", isDark = isDark)
                StatDivider(isDark)
                StatBubble(emoji = "🔪", label = "PREP", value = "${currentRecipe.prepTime} min", isDark = isDark)
                StatDivider(isDark)
                StatBubble(emoji = "🍳", label = "COOK", value = "${currentRecipe.cookTime} min", isDark = isDark)
                StatDivider(isDark)
                StatBubble(emoji = "👥", label = "SERVES", value = "${currentRecipe.servings}", isDark = isDark)
                StatDivider(isDark)
                val diffColor = when (currentRecipe.difficulty) {
                    "Easy" -> SuccessColor
                    "Hard" -> DangerColor
                    else -> PrimaryColor
                }
                StatBubble(emoji = "🔥", label = "LEVEL", value = currentRecipe.difficulty, isDark = isDark, valueColor = diffColor)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Tags ────────────────────────────────────────────────────
            if (currentRecipe.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentRecipe.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E1C1B) else Color(0xFFF4F1EC))
                                .border(1.dp, borderColor, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "# $tag",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryColor
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // ── Description / Creator ───────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) DarkCardBg else Color(0xFFFFF4EE))
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("💬", fontSize = 18.sp)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (currentRecipe.description.isNotEmpty()) {
                            Text(
                                text = currentRecipe.description,
                                fontSize = 14.sp,
                                color = if (isDark) DarkTextMuted else LightTextMuted,
                                fontStyle = FontStyle.Italic,
                                lineHeight = 22.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = "Added by: ${creatorUsername ?: "Anonymous"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryColor
                        )
                    }
                }
            }

            // ── Link ────────────────────────────────────────────────────
            val recipeLink = currentRecipe.link
            if (!recipeLink.isNullOrEmpty()) {
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) DarkCardBg else Color(0xFFFFF4EE))
                        .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                        .clickable {
                            try {
                                uriHandler.openUri(recipeLink)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔗", fontSize = 18.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recipe Link",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) DarkTextMuted else LightTextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = recipeLink,
                                fontSize = 14.sp,
                                color = PrimaryColor,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Open Link",
                            tint = PrimaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── Nutrition ───────────────────────────────────────────────
            if (currentRecipe.calories != null || currentRecipe.protein != null ||
                currentRecipe.carbs != null || currentRecipe.fat != null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBg)
                        .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Nutrition Per Serving",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = if (isDark) DarkTextMain else LightTextMain
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        NutritionItem("🔥", if (currentRecipe.calories != null) "${currentRecipe.calories} kcal" else "—", "Calories", Color(0xFFFF7043))
                        NutritionItem("🥩", if (currentRecipe.protein != null) "${currentRecipe.protein}g" else "—", "Protein", Color(0xFF4CAF50))
                        NutritionItem("🌾", if (currentRecipe.carbs != null) "${currentRecipe.carbs}g" else "—", "Carbs", Color(0xFFFFA726))
                        NutritionItem("🥑", if (currentRecipe.fat != null) "${currentRecipe.fat}g" else "—", "Fat", Color(0xFF29B6F6))
                    }
                }
            }

            // ── Interactive Rating Section ───────────────────────────────
            val currentUid2 = Firebase.auth.currentUser?.uid ?: ""
            var userRating by remember(currentRecipe.id) {
                mutableStateOf(currentRecipe.ratings[currentUid2] ?: 0)
            }
            LaunchedEffect(currentRecipe.ratings) {
                userRating = currentRecipe.ratings[currentUid2] ?: 0
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Community Rating",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = if (isDark) DarkTextMain else LightTextMain
                    )
                    if (currentRecipe.numRatings > 0) {
                        Text(
                            text = "★ ${String.format(java.util.Locale.US, "%.1f", currentRecipe.averageRating)} (${currentRecipe.numRatings} ${if (currentRecipe.numRatings == 1) "rating" else "ratings"})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryColor
                        )
                    } else {
                        Text(
                            text = "No ratings yet",
                            fontSize = 12.sp,
                            color = if (isDark) DarkTextMuted else LightTextMuted
                        )
                    }
                }

                HorizontalDivider(color = borderColor)

                Text(
                    text = if (userRating > 0) "Your Rating: $userRating ${if (userRating == 1) "Star" else "Stars"}" else "Tap to rate this recipe!",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) DarkTextMuted else LightTextMuted
                )

                // 5 Stars Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (star in 1..5) {
                        val isFilled = star <= userRating
                        Text(
                            text = if (isFilled) "★" else "☆",
                            fontSize = 32.sp,
                            color = if (isFilled) PrimaryColor else (if (isDark) DarkTextMuted else LightTextMuted),
                            modifier = Modifier
                                .clickable {
                                    userRating = star
                                    coroutineScope.launch {
                                        recipeStore.rateRecipe(currentRecipe.id, star)
                                        // Refresh current recipe details to update stats
                                        val updated = recipeStore.getById(currentRecipe.id)
                                        if (updated != null) {
                                            recipe = updated
                                        }
                                    }
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Ingredients / Steps Tabs ─────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Tab row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) DarkCardBg else Color(0xFFF0EDE8))
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        PillTab(
                            text = "Ingredients",
                            count = currentRecipe.ingredients.size,
                            isSelected = activeTab == "ingredients",
                            isDark = isDark,
                            onClick = { activeTab = "ingredients" },
                            modifier = Modifier.weight(1f)
                        )
                        PillTab(
                            text = "Instructions",
                            count = currentRecipe.steps.size,
                            isSelected = activeTab == "steps",
                            isDark = isDark,
                            onClick = { activeTab = "steps" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (activeTab == "ingredients") {
                    val isRecipeEnglish = !currentRecipe.title.any { it in '\u0590'..'\u05FF' }
                    var isImperial by remember { mutableStateOf(false) }
                    
                    if (isRecipeEnglish) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Unit System",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) DarkTextMuted else LightTextMuted
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Metric",
                                    fontSize = 12.sp,
                                    fontWeight = if (!isImperial) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!isImperial) PrimaryColor else if (isDark) DarkTextMuted else LightTextMuted,
                                    modifier = Modifier.clickable { isImperial = false }
                                )
                                Switch(
                                    checked = isImperial,
                                    onCheckedChange = { isImperial = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = PrimaryColor,
                                        uncheckedThumbColor = if (isDark) DarkTextMuted else LightTextMuted,
                                        uncheckedTrackColor = (if (isDark) DarkBorder else LightBorder).copy(alpha = 0.5f)
                                    )
                                )
                                Text(
                                    text = "Imperial",
                                    fontSize = 12.sp,
                                    fontWeight = if (isImperial) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isImperial) PrimaryColor else if (isDark) DarkTextMuted else LightTextMuted,
                                    modifier = Modifier.clickable { isImperial = true }
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        currentRecipe.ingredients.forEach { ingredient ->
                            val convertedText = if (isRecipeEnglish) {
                                val system = if (isImperial) IngredientConverter.UnitSystem.IMPERIAL else IngredientConverter.UnitSystem.METRIC
                                IngredientConverter.convertIngredient(ingredient, system)
                            } else {
                                ingredient
                            }
                            IngredientRow(text = convertedText, isDark = isDark, borderColor = borderColor)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        currentRecipe.steps.forEachIndexed { index, step ->
                            StepRow(index = index + 1, text = step, isDark = isDark, borderColor = borderColor)
                        }
                    }
                }
            }
        }

        // ── Sticky Bottom Bar ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, bgColor, bgColor)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PrimaryColor)
                    .clickable { onStartCooking(currentRecipe.id) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Text(
                        "Start Cooking",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        val isDarkLocal = isDark
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDarkLocal) DarkCardBg else LightCardBg,
            title = { Text("Delete Recipe?", fontWeight = FontWeight.Black) },
            text = {
                Text(
                    "This action cannot be undone. The recipe will be permanently deleted.",
                    color = if (isDarkLocal) DarkTextMuted else LightTextMuted,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DangerColor)
                        .clickable {
                            coroutineScope.launch {
                                recipeStore.delete(currentRecipe.id)
                                showDeleteConfirm = false
                                onDeleteRecipe()
                            }
                        }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, if (isDarkLocal) DarkBorder else LightBorder, RoundedCornerShape(10.dp))
                        .clickable { showDeleteConfirm = false }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold,
                        color = if (isDarkLocal) DarkTextMain else LightTextMain)
                }
            }
        )
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun HeroButton(
    onClick: () -> Unit,
    bgColor: Color = Color.Black.copy(alpha = 0.45f),
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun StatBubble(
    emoji: String,
    label: String,
    value: String,
    isDark: Boolean,
    valueColor: Color = if (isDark) DarkTextMain else LightTextMain
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Text(
            label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) DarkTextLight else LightTextLight,
            letterSpacing = 0.5.sp
        )
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
    }
}

@Composable
private fun StatDivider(isDark: Boolean) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(if (isDark) DarkBorder else LightBorder)
    )
}

@Composable
private fun PillTab(
    text: String,
    count: Int,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryColor else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White
        else if (isDark) DarkTextMuted else LightTextMuted,
        label = "tabText"
    )
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$text ($count)",
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun IngredientRow(text: String, isDark: Boolean, borderColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) DarkCardBg else LightCardBg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(PrimaryColor)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDark) DarkTextMain else LightTextMain,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StepRow(index: Int, text: String, isDark: Boolean, borderColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) DarkCardBg else LightCardBg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Step number badge
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(PrimaryColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index.toString(),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (isDark) DarkTextMain else LightTextMain,
            lineHeight = 22.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
fun NutritionItem(emoji: String, value: String, label: String, color: Color) {
    val isDark = isAppInDarkTheme()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 18.sp)
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) DarkTextMuted else LightTextMuted
        )
    }
}


