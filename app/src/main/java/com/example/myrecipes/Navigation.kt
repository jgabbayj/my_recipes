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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

@Composable
fun MainNavigation() {
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

  if (currentUser == null) {
    LoginScreen(
      onLoginSuccess = {},
      modifier = Modifier.fillMaxSize()
    )
  } else {
    val backStack = rememberNavBackStack(Main)

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
