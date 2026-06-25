package com.morsel.recipes.ui.dashboard

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.morsel.recipes.data.Recipe
import com.morsel.recipes.data.RecipeStore
import com.morsel.recipes.data.SettingsStore
import com.morsel.recipes.data.ImageCache
import com.morsel.recipes.theme.*
import com.morsel.recipes.R
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.morsel.recipes.data.AuthManager
import kotlinx.coroutines.launch

// Category emoji mapping
private val categoryEmojis = mapOf(
    "All" to "✨",
    "Breakfast" to "🌅",
    "Lunch" to "🥗",
    "Dinner" to "🍽️",
    "Dessert" to "🍰",
    "Snack" to "🍿"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onSelectRecipe: (String) -> Unit,
    onAddRecipe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recipeStore = remember { RecipeStore(context) }
    val settingsStore = remember { SettingsStore(context) }
    val isDark = isAppInDarkTheme()
    val currentUid = remember { Firebase.auth.currentUser?.uid ?: "" }

    var recipes by remember { mutableStateOf(emptyList<Recipe>()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("saved") } // "saved" or "discover"

    LaunchedEffect(activeTab, showSettingsDialog) {
        recipes = if (activeTab == "saved") {
            recipeStore.getAll()
        } else {
            val global = recipeStore.getGlobalRecipes()
            val saved = recipeStore.getAll()
            val savedIds = saved.map { it.id }.toSet()
            global.filter { it.id !in savedIds }
        }
        withContext(Dispatchers.IO) {
            ImageCache.preCacheImages(context, recipes.map { it.image })
            ImageCache.deleteUnusedImages(context, recipes.map { it.image })
        }
    }

    val categories = listOf("All", "Breakfast", "Lunch", "Dinner", "Dessert", "Snack")

    val filteredRecipes = recipes.filter { recipe ->
        val matchesSearch = recipe.title.contains(searchQuery, ignoreCase = true) ||
                recipe.description.contains(searchQuery, ignoreCase = true) ||
                recipe.ingredients.any { it.contains(searchQuery, ignoreCase = true) } ||
                recipe.ingredientsEnglish.any { it.contains(searchQuery, ignoreCase = true) }
        val matchesCategory = selectedCategory == "All" || recipe.category == selectedCategory
        matchesSearch && matchesCategory
    }

    val bgColor = if (isDark) DarkBg else LightBg

    Box(modifier = modifier.fillMaxSize().background(bgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Hero Header ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isDark)
                                listOf(Color(0xFF1E1410), DarkBg)
                            else
                                listOf(Color(0xFFFFF4EE), LightBg)
                        )
                    )
                    .padding(top = 52.dp, start = 20.dp, end = 20.dp, bottom = 16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.mipmap.ic_launcher),
                                    contentDescription = "App Icon",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Text(
                                    text = "Morsel",
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PrimaryColor
                                )
                            }
                            Text(
                                text = if (activeTab == "saved") {
                                    "${recipes.size} recipe${if (recipes.size != 1) "s" else ""} saved"
                                } else {
                                    "Discover new recipes"
                                },
                                modifier = Modifier.padding(top = 2.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) DarkTextMuted else LightTextMuted
                            )
                        }
                        // Settings button - circular with subtle border
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isDark) DarkCardBg else LightCardBg)
                                .border(1.dp, if (isDark) DarkBorder else LightBorder, CircleShape)
                                .clickable { showSettingsDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = PrimaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab selector (My Book vs Discover)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) DarkCardBg else Color(0xFFF0EDE8))
                            .padding(3.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            val tabModifier = Modifier.weight(1f)
                            DashboardTab(
                                text = "My Book",
                                isSelected = activeTab == "saved",
                                isDark = isDark,
                                onClick = { activeTab = "saved" },
                                modifier = tabModifier
                            )
                            DashboardTab(
                                text = "Discover",
                                isSelected = activeTab == "discover",
                                isDark = isDark,
                                onClick = { activeTab = "discover" },
                                modifier = tabModifier
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Search bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) DarkCardBg else LightCardBg)
                            .border(1.dp, if (isDark) DarkBorder else LightBorder, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = if (isDark) DarkTextMuted else LightTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            BasicSearchField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = "Search recipes or ingredients…",
                                isDark = isDark
                            )
                        }
                    }
                }
            }

            // ── Category Chips ───────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    CategoryChip(
                        label = cat,
                        emoji = categoryEmojis[cat] ?: "",
                        isSelected = selectedCategory == cat,
                        isDark = isDark,
                        onClick = { selectedCategory = cat }
                    )
                }
            }

            // ── Recipe Grid ──────────────────────────────────────────────
            if (filteredRecipes.isEmpty()) {
                EmptyState(
                    isFiltered = searchQuery.isNotEmpty() || selectedCategory != "All",
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp, top = 4.dp)
                ) {
                    items(filteredRecipes, key = { it.id }) { recipe ->
                        RecipeRowCard(
                            recipe = recipe,
                            currentUid = currentUid,
                            isDark = isDark,
                            onClick = { onSelectRecipe(recipe.id) }
                        )
                    }
                }
            }
        }

        // ── FAB ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(60.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(PrimaryColor)
                .clickable { onAddRecipe() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Recipe",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            settingsStore = settingsStore,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun DashboardTab(
    text: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryColor else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dashTabBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White
        else if (isDark) DarkTextMuted else LightTextMuted,
        label = "dashTabText"
    )
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun BasicSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isDark: Boolean
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = if (isDark) DarkTextMuted else LightTextMuted,
                fontSize = 14.sp
            )
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = if (isDark) DarkTextMain else LightTextMain,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CategoryChip(
    label: String,
    emoji: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryColor
        else if (isDark) DarkCardBg else LightCardBg,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White
        else if (isDark) DarkTextMuted else LightTextMuted,
        label = "chipText"
    )
    val borderColor = if (isSelected) Color.Transparent
    else if (isDark) DarkBorder else LightBorder

    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$emoji $label",
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
fun RecipeRowCard(recipe: Recipe, currentUid: String, isDark: Boolean, onClick: () -> Unit) {
    val cardBg = if (isDark) DarkCardBg else LightCardBg
    val difficultyColor = when (recipe.difficulty) {
        "Easy" -> SuccessColor
        "Hard" -> DangerColor
        else -> PrimaryColor
    }
    val isMyRecipe = recipe.userId == currentUid || recipe.userId.isEmpty()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg)
            .border(1.dp, if (isDark) DarkBorder else LightBorder, RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Food Photo (with gradient and badges)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                NetworkImage(
                    url = recipe.image,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.4f)
                                )
                            )
                        )
                )

                // Origin badge top-left
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background((if (isMyRecipe) SuccessColor else SecondaryColor).copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMyRecipe) Icons.Default.Person else Icons.Default.Public,
                        contentDescription = if (isMyRecipe) "My Recipe" else "Discovered",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Category tag bottom-left on image
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.BottomStart)
                        .clip(CircleShape)
                        .background(PrimaryColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = recipe.category,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Difficulty badge top-right
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(difficultyColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = recipe.difficulty,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Text info & stats panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Title and rating
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = recipe.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) DarkTextMain else LightTextMain,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        lineHeight = 22.sp
                    )
                    
                    if (recipe.numRatings > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "★",
                                color = PrimaryColor,
                                fontSize = 15.sp,
                                modifier = Modifier.offset(y = (-1).dp)
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", recipe.averageRating),
                                fontSize = 13.sp,
                                color = if (isDark) DarkTextMain else LightTextMain,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Brief description
                if (recipe.description.isNotEmpty()) {
                    Text(
                        text = recipe.description,
                        fontSize = 13.sp,
                        color = if (isDark) DarkTextMuted else LightTextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }

                // Row of basic stats: Time & Servings
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏱ ${recipe.prepTime + recipe.cookTime} min",
                        fontSize = 12.sp,
                        color = if (isDark) DarkTextMuted else LightTextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "👥 ${recipe.servings} servings",
                        fontSize = 12.sp,
                        color = if (isDark) DarkTextMuted else LightTextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Additional details at the bottom: Calories, diet details (Vegetarian, Vegan, Gluten-Free)
                val hasNutritionalInfo = recipe.calories != null || recipe.protein != null || recipe.carbs != null || recipe.fat != null
                val isVegetarian = recipe.tags.any { it.contains("vegeter", ignoreCase = true) || it.contains("vegetarian", ignoreCase = true) }
                val isVegan = recipe.tags.any { it.contains("vegan", ignoreCase = true) }
                val isGlutenFree = recipe.tags.any { it.contains("gluten", ignoreCase = true) || it.contains("gf", ignoreCase = true) }

                if (hasNutritionalInfo || isVegetarian || isVegan || isGlutenFree) {
                    HorizontalDivider(
                        color = (if (isDark) DarkBorder else LightBorder).copy(alpha = 0.5f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Nutrition details on the left
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (recipe.calories != null) {
                                Text(
                                    text = "🔥 ${recipe.calories} kcal",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) DarkTextMain else LightTextMain
                                )
                            }
                            if (recipe.protein != null) {
                                Text(
                                    text = "🥩 ${recipe.protein}g protein",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) DarkTextMuted else LightTextMuted
                                )
                            }
                        }

                        // Dietary labels on the right
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isVegan) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F5E9))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "🌱 Vegan",
                                        color = Color(0xFF2E7D32),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (isVegetarian) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F5E9))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "🌱 Veg",
                                        color = Color(0xFF2E7D32),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (isGlutenFree) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFF3E0))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "🌾 GF",
                                        color = Color(0xFFE65100),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(isFiltered: Boolean, isDark: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            if (isFiltered) {
                Text("🔍", fontSize = 64.sp)
            } else {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "App Icon",
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp))
                )
            }
            Text(
                if (isFiltered) "No recipes found" else "Your cookbook is empty",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = if (isDark) DarkTextMain else LightTextMain
            )
            Text(
                if (isFiltered) "Try a different search or category"
                else "Tap + to add your first recipe!",
                fontSize = 14.sp,
                color = if (isDark) DarkTextMuted else LightTextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun NetworkImage(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            val myBitmap = ImageCache.getOrDownload(context, url)
            if (myBitmap != null) {
                bitmap = myBitmap.asImageBitmap()
            }
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(
                if (isDark) Color(0xFF2A1C16) else Color(0xFFFEF4F0)
            ),
            contentAlignment = Alignment.Center
        ) {
            Text("🍲", fontSize = 28.sp)
        }
    }
}

// ── Settings Dialog ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    settingsStore: SettingsStore,
    onDismiss: () -> Unit
) {
    var themeInput by remember { mutableStateOf(settingsStore.getTheme()) }
    val isDark = isAppInDarkTheme()
    val bgColor = if (isDark) DarkCardBg else LightCardBg
    val borderColor = if (isDark) DarkBorder else LightBorder

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚙️", fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            "Settings",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = if (isDark) DarkTextMain else LightTextMain
                        )
                        Text(
                            "Configure your app",
                            fontSize = 12.sp,
                            color = if (isDark) DarkTextMuted else LightTextMuted
                        )
                    }
                }

                HorizontalDivider(color = borderColor)

                // User Profile
                val currentUser = Firebase.auth.currentUser
                if (currentUser != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) Color(0xFF1E1C1B) else Color(0xFFF9F7F4))
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Signed in as",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) DarkTextMuted else LightTextMuted
                            )
                            Text(
                                currentUser.displayName ?: currentUser.email ?: "User",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDark) DarkTextMain else LightTextMain
                            )
                            if (currentUser.email != null && currentUser.displayName != null) {
                                Text(
                                    currentUser.email ?: "",
                                    fontSize = 12.sp,
                                    color = if (isDark) DarkTextMuted else LightTextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val coroutineScope = rememberCoroutineScope()
                            val context = LocalContext.current
                            val authManager = remember { AuthManager(context) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, DangerColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            RecipeStore(context).clearCache()
                                            authManager.signOut()
                                            onDismiss()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Sign Out",
                                    color = DangerColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }


                // Theme Mode section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Theme Mode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDark) DarkTextMain else LightTextMain
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(
                            "system" to "System",
                            "light" to "Light",
                            "dark" to "Dark"
                        )
                        options.forEach { (value, label) ->
                            val isSelected = themeInput == value
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) PrimaryColor else (if (isDark) DarkBg else LightBg)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) PrimaryColor else borderColor,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { themeInput = value },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else (if (isDark) DarkTextMain else LightTextMain),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }


                // Save button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PrimaryColor)
                        .clickable {
                            settingsStore.saveTheme(themeInput)
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Save Settings",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
