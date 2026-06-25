package com.morsel.recipes.ui.auth

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morsel.recipes.data.AuthManager
import com.morsel.recipes.theme.*
import com.morsel.recipes.R
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val isDark = isAppInDarkTheme()

    // Subtle pulsing animation on the logo
    val infiniteTransition = rememberInfiniteTransition(label = "logoAnim")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )

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
        // Decorative blobs (top-right and bottom-left)
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = 100.dp, y = (-160).dp)
                .clip(CircleShape)
                .background(PrimaryColor.copy(alpha = if (isDark) 0.09f else 0.07f))
                .align(Alignment.TopEnd)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = (-80).dp, y = 120.dp)
                .clip(CircleShape)
                .background(SecondaryColor.copy(alpha = if (isDark) 0.07f else 0.05f))
                .align(Alignment.BottomStart)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Logo ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .size(110.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                PrimaryColor.copy(alpha = 0.18f),
                                PrimaryColor.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(1.5.dp, PrimaryColor.copy(alpha = 0.25f), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp))
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── App Name ─────────────────────────────────────────────────
            Text(
                text = "Morsel",
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = PrimaryColor,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your personal cookbook,\nsynced everywhere.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) DarkTextMuted else LightTextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ── Feature pills ────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeaturePill("🤖 AI Parsing", isDark, modifier = Modifier.weight(1f))
                FeaturePill("☁️ Cloud Sync", isDark, modifier = Modifier.weight(1f))
                FeaturePill("🍳 Cook Mode", isDark, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── Sign-In Button ────────────────────────────────────────────
            if (isLoading) {
                CircularProgressIndicator(
                    color = PrimaryColor,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isDark) DarkCardBg else LightCardBg)
                        .border(
                            1.5.dp,
                            if (isDark) DarkBorder else LightBorder,
                            RoundedCornerShape(18.dp)
                        )
                        .clickable {
                            isLoading = true
                            coroutineScope.launch {
                                val result = authManager.signInWithGoogle(context)
                                isLoading = false
                                result.fold(
                                    onSuccess = {
                                        Toast.makeText(context, "Welcome, ${it.displayName ?: "User"}!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(context, "Sign in failed: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Google G icon (coloured)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF4285F4),
                                            Color(0xFF34A853)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "G",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Continue with Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) DarkTextMain else LightTextMain
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "By signing in you agree to our Terms & Privacy Policy.",
                fontSize = 11.sp,
                color = if (isDark) DarkTextLight else LightTextLight,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeaturePill(text: String, isDark: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) DarkCardBg else LightCardBg)
            .border(1.dp, if (isDark) DarkBorder else LightBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) DarkTextMuted else LightTextMuted,
            textAlign = TextAlign.Center
        )
    }
}
