package com.fantasyfoodplanner.features

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fantasyfoodplanner.data.*
import com.fantasyfoodplanner.ui.*
import com.fantasyfoodplanner.utils.DaySummaryData
import kotlinx.coroutines.flow.first

@Composable
fun RecipesScreen(onBack: () -> Unit, onAddToPlan: (String) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDb.get(ctx) }
    var allRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedRecipeForDetail by remember { mutableStateOf<Recipe?>(null) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(Unit) {
        allRecipes = db.recipeDao().getAll().first()
        userProfile = db.userDao().profile().first()
    }

    val filteredRecipes = allRecipes.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.category.contains(searchQuery, ignoreCase = true) 
    }

     FantasySurface {
        Column {
            MainAppBar("Rezept-Datenbank", onBack = onBack, showAI = true)

            FantasyTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Rezepte durchsuchen...",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredRecipes, key = { it.id }) { recipe ->
                    RecipeCard(recipe) {
                        selectedRecipeForDetail = recipe
                    }
                }
            }
        }

        selectedRecipeForDetail?.let { recipe ->
            RecipeDetailDialog(
                recipe = recipe,
                daySummary = DaySummaryData(0, 0.0, 0.0, 0.0), // Platzhalter für Datenbank-Ansicht
                userProfile = userProfile,
                onDismiss = { selectedRecipeForDetail = null },
                onAction = {
                    onAddToPlan(recipe.id)
                    selectedRecipeForDetail = null
                },
                actionLabel = "Zum Plan hinzufügen"
            )
        }
    }
}

@Composable
private fun RecipeCard(recipe: Recipe, onClick: () -> Unit) {
    val categoryLabel = when (recipe.category) {
        "fit" -> "FITNESS"
        "build" -> "MUSKELAUFBAU"
        "lose" -> "ABNEHMEN"
        else -> recipe.category.uppercase()
    }
    FantasyCard(modifier = Modifier.clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FText(recipe.name, bold = true, sizeSp = 18, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Surface(color = FantasyColors.Accent.copy(0.12f), shape = RoundedCornerShape(4.dp), border = androidx.compose.foundation.BorderStroke(0.5.dp, FantasyColors.Accent.copy(0.3f))) {
                FText(" $categoryLabel ", sizeSp = 9, color = FantasyColors.Accent, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        FText("${recipe.kcal} kcal | E: ${recipe.protein}g K: ${recipe.carbs}g F: ${recipe.fat}g", sizeSp = 13, color = FantasyColors.GrayText)
    }
}
