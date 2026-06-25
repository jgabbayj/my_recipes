package com.morsel.recipes.ui.cook

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morsel.recipes.data.Recipe
import com.morsel.recipes.data.RecipeStore
import com.morsel.recipes.data.IngredientConverter
import com.morsel.recipes.theme.*
import java.util.Locale

@Composable
fun CookModeScreen(
    recipeId: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recipeStore = remember { RecipeStore(context) }
    val isDark = isAppInDarkTheme()
    var recipe by remember { mutableStateOf<Recipe?>(null) }

    var currentScreen by remember { mutableStateOf("checklist") }
    val checkedIngredients = remember { mutableStateMapOf<Int, Boolean>() }
    var currentStepIdx by remember { mutableStateOf(0) }
    var goingForward by remember { mutableStateOf(true) }

    // Timer states
    var timerDuration by remember { mutableStateOf(0) }
    var timerSecondsLeft by remember { mutableStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(recipeId) {
        recipe = recipeStore.getById(recipeId)
    }

    val currentRecipe = recipe ?: return

    val activeStepText = currentRecipe.steps.getOrNull(currentStepIdx) ?: ""

    fun detectTimerSeconds(text: String): Int? {
        val hrRegex = Regex("""\b(\d+)\s*(?:-|to)?\s*(\d+)?\s*(?:hours|hour|hrs|hr)\b""", RegexOption.IGNORE_CASE)
        val minRegex = Regex("""\b(\d+)\s*(?:-|to)?\s*(\d+)?\s*(?:minutes|minute|mins|min)\b""", RegexOption.IGNORE_CASE)
        val secRegex = Regex("""\b(\d+)\s*(?:-|to)?\s*(\d+)?\s*(?:seconds|second|secs|sec)\b""", RegexOption.IGNORE_CASE)
        var total = 0
        hrRegex.find(text)?.let { m -> total += (m.groupValues[2].ifEmpty { m.groupValues[1] }).toIntOrNull()?.times(3600) ?: 0 }
        minRegex.find(text)?.let { m -> total += (m.groupValues[2].ifEmpty { m.groupValues[1] }).toIntOrNull()?.times(60) ?: 0 }
        secRegex.find(text)?.let { m -> total += (m.groupValues[2].ifEmpty { m.groupValues[1] }).toIntOrNull() ?: 0 }
        return if (total > 0) total else null
    }

    LaunchedEffect(currentStepIdx, currentScreen) {
        if (currentScreen == "steps") {
            val seconds = detectTimerSeconds(activeStepText)
            timerDuration = seconds ?: 0
            timerSecondsLeft = seconds ?: 0
            isTimerRunning = false
        }
    }

    LaunchedEffect(isTimerRunning, timerSecondsLeft) {
        if (isTimerRunning && timerSecondsLeft > 0) {
            kotlinx.coroutines.delay(1000)
            timerSecondsLeft -= 1
            if (timerSecondsLeft == 0) {
                isTimerRunning = false
                try {
                    ToneGenerator(AudioManager.STREAM_ALARM, 100).startTone(ToneGenerator.TONE_CDMA_PIP, 800)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    fun formatTime(secs: Int): String {
        val h = secs / 3600; val m = (secs % 3600) / 60; val s = secs % 60
        return if (h > 0) String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
        else String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    val progress = if (currentScreen == "checklist") 0f
    else (currentStepIdx + 1).toFloat() / currentRecipe.steps.size
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "progress"
    )

    val bgColor = if (isDark) DarkBg else LightBg
    val cardBg = if (isDark) DarkCardBg else LightCardBg
    val borderColor = if (isDark) DarkBorder else LightBorder

    Box(modifier = modifier.fillMaxSize().background(bgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Custom TopBar ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = if (isDark)
                                listOf(Color(0xFF1E1410), bgColor)
                            else
                                listOf(Color(0xFFFFF4EE), bgColor)
                        )
                    )
                    .padding(top = 48.dp, start = 16.dp, end = 20.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDark) DarkCardBg else LightCardBg)
                                .border(1.dp, borderColor, CircleShape)
                                .clickable { onExit() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit",
                                tint = if (isDark) DarkTextMain else LightTextMain,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                "COOK MODE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = PrimaryColor,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                currentRecipe.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) DarkTextMain else LightTextMain,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 200.dp)
                            )
                        }
                    }

                    // Step counter pill
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(PrimaryColor.copy(alpha = 0.12f))
                            .border(1.dp, PrimaryColor.copy(alpha = 0.3f), CircleShape)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (currentScreen == "checklist") "Prep" else "${currentStepIdx + 1} / ${currentRecipe.steps.size}",
                            color = PrimaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // ── Progress Bar ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(if (isDark) DarkCardBg else LightBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(PrimaryColor, PrimaryHoverColor))
                        )
                )
            }

            // ── Content ──────────────────────────────────────────────────
            if (currentScreen == "checklist") {
                PrepChecklistContent(
                    recipe = currentRecipe,
                    checkedIngredients = checkedIngredients,
                    isDark = isDark,
                    cardBg = cardBg,
                    borderColor = borderColor,
                    bgColor = bgColor,
                    onStartCooking = { currentScreen = "steps" }
                )
            } else {
                StepsContent(
                    recipe = currentRecipe,
                    currentStepIdx = currentStepIdx,
                    goingForward = goingForward,
                    activeStepText = activeStepText,
                    timerDuration = timerDuration,
                    timerSecondsLeft = timerSecondsLeft,
                    isTimerRunning = isTimerRunning,
                    isDark = isDark,
                    cardBg = cardBg,
                    borderColor = borderColor,
                    formatTime = ::formatTime,
                    onTimerToggle = { isTimerRunning = !isTimerRunning },
                    onTimerReset = { timerSecondsLeft = timerDuration; isTimerRunning = false },
                    onPrev = {
                        if (currentStepIdx > 0) { goingForward = false; currentStepIdx-- }
                    },
                    onNext = {
                        if (currentStepIdx < currentRecipe.steps.size - 1) {
                            goingForward = true
                            currentStepIdx++
                        } else {
                            onExit()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PrepChecklistContent(
    recipe: Recipe,
    checkedIngredients: MutableMap<Int, Boolean>,
    isDark: Boolean,
    cardBg: Color,
    borderColor: Color,
    bgColor: Color,
    onStartCooking: () -> Unit
) {
    var isImperial by remember { mutableStateOf(false) }
    val isRecipeEnglish = !recipe.title.any { it in '\u0590'..'\u05FF' }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 4.dp)) {
            Text(
                "Gather Ingredients",
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = if (isDark) DarkTextMain else LightTextMain
            )
            Text(
                "Check off each ingredient as you prepare.",
                fontSize = 13.sp,
                color = if (isDark) DarkTextMuted else LightTextMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (isRecipeEnglish) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Row 1: Unit System (Metric / Imperial)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Unit System",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkTextMuted else LightTextMuted
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Metric",
                            fontSize = 11.sp,
                            fontWeight = if (!isImperial) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isImperial) PrimaryColor else if (isDark) DarkTextMuted else LightTextMuted,
                            modifier = Modifier.clickable { isImperial = false }
                        )
                        Switch(
                            checked = isImperial,
                            onCheckedChange = { isImperial = it },
                            modifier = Modifier.scale(0.8f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryColor,
                                uncheckedThumbColor = if (isDark) DarkTextMuted else LightTextMuted,
                                uncheckedTrackColor = borderColor.copy(alpha = 0.5f)
                            )
                        )
                        Text(
                            text = "Imperial",
                            fontSize = 11.sp,
                            fontWeight = if (isImperial) FontWeight.Bold else FontWeight.Normal,
                            color = if (isImperial) PrimaryColor else if (isDark) DarkTextMuted else LightTextMuted,
                            modifier = Modifier.clickable { isImperial = true }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            recipe.ingredients.forEachIndexed { index, ing ->
                val isChecked = checkedIngredients[index] ?: false
                val rowBg = if (isChecked) {
                    if (isDark) Color(0xFF1E2A1A) else Color(0xFFF0FFF4)
                } else cardBg
                val rowBorder = if (isChecked) SuccessColor.copy(alpha = 0.5f) else borderColor

                val convertedText = if (isRecipeEnglish) {
                    val system = if (isImperial) IngredientConverter.UnitSystem.IMPERIAL else IngredientConverter.UnitSystem.METRIC
                    IngredientConverter.convertIngredient(ing, system)
                } else {
                    ing
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(rowBg)
                        .border(1.dp, rowBorder, RoundedCornerShape(14.dp))
                        .clickable { checkedIngredients[index] = !isChecked }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Custom checkbox
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isChecked) SuccessColor else Color.Transparent)
                            .border(
                                2.dp,
                                if (isChecked) Color.Transparent else if (isDark) DarkTextMuted else LightTextLight,
                                RoundedCornerShape(7.dp)
                            )
                    ) {
                        if (isChecked) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(
                        text = convertedText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isChecked) {
                            if (isDark) DarkTextMuted else LightTextMuted
                        } else {
                            if (isDark) DarkTextMain else LightTextMain
                        },
                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(PrimaryColor)
                .clickable { onStartCooking() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Text(
                    "Start Cooking Steps",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
            }
        }
    }
}

@Composable
private fun StepsContent(
    recipe: Recipe,
    currentStepIdx: Int,
    goingForward: Boolean,
    activeStepText: String,
    timerDuration: Int,
    timerSecondsLeft: Int,
    isTimerRunning: Boolean,
    isDark: Boolean,
    cardBg: Color,
    borderColor: Color,
    formatTime: (Int) -> String,
    onTimerToggle: () -> Unit,
    onTimerReset: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val isLastStep = currentStepIdx == recipe.steps.size - 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ── Step progress dots ───────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val visibleDots = minOf(recipe.steps.size, 8)
            val offset = if (recipe.steps.size > 8 && currentStepIdx >= 4)
                minOf(currentStepIdx - 3, recipe.steps.size - 8) else 0
            (0 until visibleDots).forEach { i ->
                val idx = i + offset
                val isActive = idx == currentStepIdx
                val isPast = idx < currentStepIdx
                Box(
                    modifier = Modifier
                        .size(if (isActive) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isActive -> PrimaryColor
                                isPast -> PrimaryColor.copy(alpha = 0.4f)
                                else -> if (isDark) DarkBorder else LightBorder
                            }
                        )
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${currentStepIdx + 1} of ${recipe.steps.size}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) DarkTextMuted else LightTextMuted
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Step Card ────────────────────────────────────────────────────
        AnimatedContent(
            targetState = currentStepIdx,
            transitionSpec = {
                val enter = if (goingForward)
                    slideInHorizontally { it / 3 } + fadeIn()
                else
                    slideInHorizontally { -it / 3 } + fadeIn()
                val exit = if (goingForward)
                    slideOutHorizontally { -it / 3 } + fadeOut()
                else
                    slideOutHorizontally { it / 3 } + fadeOut()
                enter togetherWith exit
            },
            modifier = Modifier.weight(1f),
            label = "stepAnim"
        ) { stepIdx ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Step badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryColor.copy(alpha = 0.12f))
                            .border(1.dp, PrimaryColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "STEP ${stepIdx + 1}",
                            color = PrimaryColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = recipe.steps.getOrNull(stepIdx) ?: "",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) DarkTextMain else LightTextMain,
                        lineHeight = 32.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Timer ────────────────────────────────────────────────────────
        if (timerDuration > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isTimerRunning) PrimaryColor.copy(alpha = 0.08f)
                        else if (isDark) DarkCardBg else LightCardBg
                    )
                    .border(
                        1.dp,
                        if (isTimerRunning) PrimaryColor.copy(alpha = 0.4f) else borderColor,
                        RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("⏱️", fontSize = 24.sp)
                        Column {
                            Text(
                                "STEP TIMER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) DarkTextLight else LightTextLight,
                                letterSpacing = 1.sp
                            )
                            Text(
                                formatTime(timerSecondsLeft),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isTimerRunning) PrimaryColor
                                else if (isDark) DarkTextMain else LightTextMain
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) DarkBg else Color(0xFFF0EDE8))
                                .clickable { onTimerReset() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Refresh, null,
                                tint = if (isDark) DarkTextMuted else LightTextMuted,
                                modifier = Modifier.size(18.dp))
                        }
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isTimerRunning) Color(0xFF1E1E1E) else PrimaryColor)
                                .clickable { onTimerToggle() }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isTimerRunning) "⏸ Pause" else "▶ Start",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // ── Navigation ───────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Back
            Box(
                modifier = Modifier
                    .height(58.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isDark) DarkCardBg else LightCardBg)
                    .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                    .then(if (currentStepIdx > 0) Modifier.clickable { onPrev() } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        null,
                        tint = if (currentStepIdx > 0) {
                            if (isDark) DarkTextMain else LightTextMain
                        } else {
                            if (isDark) DarkTextLight else LightTextLight
                        },
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        "Prev",
                        color = if (currentStepIdx > 0) {
                            if (isDark) DarkTextMain else LightTextMain
                        } else {
                            if (isDark) DarkTextLight else LightTextLight
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            // Next / Finish
            Box(
                modifier = Modifier
                    .height(58.dp)
                    .weight(2f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isLastStep)
                            Brush.linearGradient(listOf(SuccessColor, Color(0xFF38A169)))
                        else
                            Brush.linearGradient(listOf(PrimaryColor, PrimaryHoverColor))
                    )
                    .clickable { onNext() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isLastStep) "🎉 Finish!" else "Next Step",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                    if (!isLastStep) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
