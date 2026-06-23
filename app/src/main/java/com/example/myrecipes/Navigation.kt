package com.example.myrecipes

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.myrecipes.ui.dashboard.DashboardScreen
import com.example.myrecipes.ui.detail.RecipeDetailScreen
import com.example.myrecipes.ui.form.RecipeFormScreen
import com.example.myrecipes.ui.cook.CookModeScreen
import com.example.myrecipes.ui.auth.LoginScreen
import com.example.myrecipes.ui.auth.UsernamePromptScreen
import com.example.myrecipes.theme.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment

@Composable
fun MainNavigation(
  sharedText: String? = null,
  onSharedTextConsumed: () -> Unit = {}
) {
  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      currentUser = firebaseAuth.currentUser
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose {
      Firebase.auth.removeAuthStateListener(listener)
    }
  }

  var showUsernamePrompt by remember { mutableStateOf(false) }
  var isCheckingUsername by remember { mutableStateOf(false) }

  LaunchedEffect(currentUser) {
    val uid = currentUser?.uid
    if (uid != null) {
      isCheckingUsername = true
      try {
        val doc = Firebase.firestore.collection("users").document(uid).get().await()
        val existingUsername = doc.getString("username")
        showUsernamePrompt = existingUsername.isNullOrEmpty()
      } catch (e: Exception) {
        e.printStackTrace()
        showUsernamePrompt = true
      } finally {
        isCheckingUsername = false
      }
    } else {
      showUsernamePrompt = false
    }
  }

  if (currentUser == null) {
    LoginScreen(
      onLoginSuccess = {},
      modifier = Modifier.fillMaxSize()
    )
  } else if (isCheckingUsername) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(if (isAppInDarkTheme()) DarkBg else LightBg),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator(color = PrimaryColor)
    }
  } else if (showUsernamePrompt) {
    UsernamePromptScreen(
      uid = currentUser!!.uid,
      onUsernameSaved = { showUsernamePrompt = false },
      modifier = Modifier.fillMaxSize()
    )
  } else {
    val backStack = rememberNavBackStack(Main)

    androidx.compose.runtime.LaunchedEffect(sharedText) {
      if (sharedText != null) {
        backStack.add(RecipeForm(recipeId = null, initialUrl = sharedText))
        onSharedTextConsumed()
      }
    }

    NavDisplay(
      backStack = backStack,
      onBack = { backStack.removeLastOrNull() },
      entryProvider =
        entryProvider {
          entry<Main> {
            DashboardScreen(
              onSelectRecipe = { id -> backStack.add(RecipeDetail(id)) },
              onAddRecipe = { backStack.add(RecipeForm(null)) },
              modifier = Modifier.fillMaxSize()
            )
          }
          entry<RecipeDetail> { key ->
            RecipeDetailScreen(
              recipeId = key.recipeId,
              onBack = { backStack.removeLastOrNull() },
              onEditRecipe = { id -> backStack.add(RecipeForm(id)) },
              onStartCooking = { id -> backStack.add(CookMode(id)) },
              onDeleteRecipe = { backStack.removeLastOrNull() },
              modifier = Modifier.fillMaxSize()
            )
          }
          entry<RecipeForm> { key ->
            RecipeFormScreen(
              recipeId = key.recipeId,
              initialUrl = key.initialUrl,
              onBack = { backStack.removeLastOrNull() },
              onSave = { backStack.removeLastOrNull() },
              modifier = Modifier.fillMaxSize()
            )
          }
          entry<CookMode> { key ->
            CookModeScreen(
              recipeId = key.recipeId,
              onExit = { backStack.removeLastOrNull() },
              modifier = Modifier.fillMaxSize()
            )
          }
        },
    )
  }
}
