package com.morsel.recipes.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morsel.recipes.theme.*
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsernamePromptScreen(
    uid: String,
    onUsernameSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = isAppInDarkTheme()
    
    var inputUsername by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    // Generate a persistent 3-digit identifier when screen is loaded
    val identifier = remember { String.format("%03d", Random.nextInt(1000)) }
    val maxCharLimit = 16

    val finalUsername = if (inputUsername.trim().isNotEmpty()) {
        "${inputUsername.trim()}#$identifier"
    } else {
        ""
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDark)
                        listOf(Color(0xFF1E1108), Color(0xFF121110), Color(0xFF0E0D0C))
                    else
                        listOf(Color(0xFFFFF8F4), Color(0xFFFAF6F2), Color(0xFFFAF8F5))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background blobs
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(x = 100.dp, y = (-150).dp)
                .clip(CircleShape)
                .background(PrimaryColor.copy(alpha = if (isDark) 0.08f else 0.06f))
                .align(Alignment.TopEnd)
        )
        Box(
            modifier = Modifier
                .size(180.dp)
                .offset(x = (-60).dp, y = 100.dp)
                .clip(CircleShape)
                .background(SecondaryColor.copy(alpha = if (isDark) 0.06f else 0.04f))
                .align(Alignment.BottomStart)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Welcome to Morsel!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = PrimaryColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Choose a cooking alias for your profile.\nA 3-digit identifier will be appended to it.",
                    fontSize = 14.sp,
                    color = if (isDark) DarkTextMuted else LightTextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            // Glassmorphic Input Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isDark) DarkCardBg else LightCardBg)
                    .border(
                        1.5.dp,
                        if (isDark) DarkBorder else LightBorder,
                        RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "CHOOSE USERNAME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) DarkTextLight else LightTextLight,
                        letterSpacing = 1.5.sp
                    )

                    OutlinedTextField(
                        value = inputUsername,
                        onValueChange = {
                            if (it.length <= maxCharLimit) {
                                inputUsername = it.replace(Regex("[#\\s]"), "") // Disallow hashes/spaces
                            }
                        },
                        placeholder = { Text("e.g. Jonathan", color = if (isDark) DarkTextLight else LightTextLight) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryColor,
                            unfocusedBorderColor = if (isDark) DarkBorder else LightBorder,
                            focusedTextColor = if (isDark) DarkTextMain else LightTextMain,
                            unfocusedTextColor = if (isDark) DarkTextMain else LightTextMain
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Only letters & numbers (max 16)",
                                    fontSize = 11.sp,
                                    color = if (isDark) DarkTextLight else LightTextLight
                                )
                                Text(
                                    text = "${inputUsername.length} / $maxCharLimit",
                                    fontSize = 11.sp,
                                    color = if (inputUsername.length == maxCharLimit) DangerColor else (if (isDark) DarkTextLight else LightTextLight)
                                )
                            }
                        }
                    )

                    if (finalUsername.isNotEmpty()) {
                        // Interactive Live Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryColor.copy(alpha = 0.08f))
                                .border(1.dp, PrimaryColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Preview: $finalUsername",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = PrimaryColor
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(color = PrimaryColor, modifier = Modifier.size(36.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (inputUsername.trim().isEmpty()) Color.Gray.copy(alpha = 0.3f) else PrimaryColor)
                        .then(
                            if (inputUsername.trim().isNotEmpty()) {
                                Modifier.clickable {
                                    isLoading = true
                                    coroutineScope.launch {
                                        try {
                                            Firebase.firestore.collection("users")
                                                .document(uid)
                                                .set(
                                                    mapOf("username" to finalUsername),
                                                    com.google.firebase.firestore.SetOptions.merge()
                                                )
                                                .await()
                                            
                                            Toast.makeText(context, "Username saved!", Toast.LENGTH_SHORT).show()
                                            onUsernameSaved()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Continue",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
