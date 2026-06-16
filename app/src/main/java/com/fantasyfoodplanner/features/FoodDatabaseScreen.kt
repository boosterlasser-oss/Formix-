package com.fantasyfoodplanner.features

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fantasyfoodplanner.data.*
import com.fantasyfoodplanner.logic.*
import com.fantasyfoodplanner.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.Locale

/**
 * Zentraler Such-Screen für ALLES Essbare.
 * Integriert nun die strikte Trainings-Validierung (Pre/Post/Regen).
 */
@Composable
fun FoodDatabaseScreen(
    dateEpochDay: Long,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { AppDb.get(ctx) }
    val scope = rememberCoroutineScope()

    var allRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var allProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf("Name") }
    var searchQuery by remember { mutableStateOf("") }

    // State für den Trainings-Filter-Bereich
    var activeTrainingTab by remember { mutableStateOf<String?>(null) }
    var nutritionTrainingType by remember { mutableStateOf(SettingsManager.getNutritionTrainingType(ctx)) }

    var selectedFoodItem by remember { mutableStateOf<FoodItem?>(null) }
    var selectedRecipeItem by remember { mutableStateOf<Recipe?>(null) }
    var selectedProductItem by remember { mutableStateOf<Product?>(null) }
    
    var showGramDialogForFood by remember { mutableStateOf<FoodItem?>(null) }
    var showGramDialogForProduct by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(Unit) {
        allRecipes = db.recipeDao().getAll().first()
        allProducts = db.productDao().getAll().first()
    }

    val allTags = remember { 
        listOf("Elektrolyt", "Regeneration", "pre_workout", "post_workout", "regeneration", "Muskelaufbau", "Diätfreundlich", "Performance").sorted()
    }

    // --- LOGIK: FILTERN + VALIDIEREN ---
    val filteredList = remember(searchQuery, selectedCategory, selectedTag, sortBy, allRecipes, allProducts, activeTrainingTab, nutritionTrainingType) {
        val parsed = SearchProcessor.parseQuery(searchQuery)
        val fullList = mutableListOf<Any>()
        
        if (selectedCategory == null || selectedCategory == "Obst" || selectedCategory == "Gemüse") {
            fullList.addAll(foodDatabase.filter { food ->
                val tags = getTagsForItem(food)
                (selectedCategory == null || food.category == selectedCategory) &&
                (selectedTag == null || tags.contains(selectedTag))
            })
        }
        if (selectedCategory == null || selectedCategory == "Rezepte") {
            fullList.addAll(allRecipes.filter { recipe ->
                val tags = getTagsForItem(recipe)
                (selectedTag == null || tags.contains(selectedTag))
            })
        }
        if (selectedCategory == null || selectedCategory == "Produkte") {
            fullList.addAll(allProducts.filter { product ->
                val tags = getTagsForItem(product)
                (selectedTag == null || tags.contains(selectedTag))
            })
        }

        val validatedList = if (activeTrainingTab != null) {
            fullList.filter { item ->
                TrainingValidator.validate(item, activeTrainingTab!!, nutritionTrainingType)
            }
        } else {
            fullList
        }

        val uniqueList = validatedList.distinctBy { getNameKey(it) }

        val scoredList = uniqueList.mapNotNull { item ->
            val score = SearchProcessor.calculateScore(item, parsed)
            if (score > 0) item to score else null
        }

        val finalSortMode = parsed.detectedSort ?: sortBy
        scoredList.sortedWith { a, b ->
            if (a.second != b.second) {
                b.second.compareTo(a.second)
            } else {
                when (finalSortMode) {
                    "Kalorien" -> getKcal(b.first).toDouble().compareTo(getKcal(a.first).toDouble())
                    "KalorienAsc" -> getKcal(a.first).toDouble().compareTo(getKcal(b.first).toDouble())
                    "Protein" -> getProtein(b.first).compareTo(getProtein(a.first))
                    "Kohlenhydrate" -> getCarbs(b.first).compareTo(getCarbs(a.first))
                    "KohlenhydrateAsc" -> getCarbs(a.first).compareTo(getCarbs(b.first))
                    "Vitamin C" -> getVitaminC(b.first).compareTo(getVitaminC(a.first))
                    "Kalium" -> getPotassium(b.first).compareTo(getPotassium(a.first))
                    else -> getName(a.first).compareTo(getName(b.first))
                }
            }
        }.map { it.first }
    }

    FantasySurface {
        Column(Modifier.fillMaxSize()) {
            MainAppBar("Datenbank-Suche", onBack = onBack, showAI = true)

            if (activeTrainingTab != null) {
                Surface(
                    color = FantasyColors.Accent.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FantasyColors.Accent.copy(alpha = 0.3f))
                ) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FText(activeTrainingTab!!.uppercase(), sizeSp = 10, bold = true, color = FantasyColors.Accent)
                        FText(" | ", color = Color.Gray)
                        FText("Modus: ${nutritionTrainingType.name.replace("_", "/")}", sizeSp = 10, color = Color.White)
                    }
                }
            }

            FantasyTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Suchen (z.B. 'eiweiß', 'low carb')...",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                color = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(12.dp)
            ) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val cats = listOf("Alle", "Obst", "Gemüse", "Rezepte", "Produkte")
                    cats.forEach { cat ->
                        item {
                            val isSel = (selectedCategory ?: "Alle") == cat
                            Surface(
                                onClick = { selectedCategory = if (cat == "Alle") null else cat },
                                color = if (isSel) FantasyColors.Accent.copy(alpha = 0.2f) else Color(0xFF252525),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) FantasyColors.Accent.copy(alpha = 0.5f) else Color(0xFF333333))
                            ) {
                                FText(cat, sizeSp = 12, bold = isSel, color = if (isSel) FantasyColors.Accent else FantasyColors.Text, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            Surface(
                                onClick = { selectedTag = null },
                                color = if (selectedTag == null) FantasyColors.Accent.copy(alpha = 0.2f) else Color(0xFF252525),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedTag == null) FantasyColors.Accent.copy(alpha = 0.5f) else Color(0xFF333333))
                            ) {
                                FText("Alle Tags", sizeSp = 10, color = if (selectedTag == null) FantasyColors.Accent else FantasyColors.GrayText, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                            }
                        }
                        allTags.forEach { tag ->
                            item {
                                Surface(
                                    onClick = { selectedTag = tag },
                                    color = if (selectedTag == tag) FantasyColors.Accent.copy(alpha = 0.2f) else Color(0xFF252525),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedTag == tag) FantasyColors.Accent.copy(alpha = 0.5f) else Color(0xFF333333))
                                ) {
                                    FText(tag, sizeSp = 10, color = if (selectedTag == tag) FantasyColors.Accent else FantasyColors.GrayText, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                }
                            }
                        }
                    }
                }

                var showSortMenu by remember { mutableStateOf(false) }
                Box {
                    Surface(
                        onClick = { showSortMenu = true },
                        color = Color(0xFF1A1A1A),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            FText(sortBy, sizeSp = 10, bold = true)
                            Icon(Icons.Default.ArrowDropDown, null, tint = FantasyColors.Accent, modifier = Modifier.size(16.dp))
                        }
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(Color(0xFF1A1A1A))
                    ) {
                        listOf("Name", "Kalorien", "Protein", "Kohlenhydrate", "Vitamin C", "Kalium").forEach { option ->
                            DropdownMenuItem(
                                text = { FText(option) },
                                onClick = { sortBy = option; showSortMenu = false }
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredList.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                            val msg = if (activeTrainingTab != null) "0 Treffer – Tags/Makros prüfen" else "Keine Ergebnisse gefunden."
                            FText(msg, color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    items(filteredList, key = { getItemKey(it) }) { item ->
                        val itemTags = getTagsForItem(item)
                        val isValidated = activeTrainingTab != null
                        when (item) {
                            is FoodItem -> FoodItemRow(item, 100, tags = itemTags, isValidated = isValidated) { 
                                selectedFoodItem = item
                            }
                            is Recipe -> RecipeItemRow(item, tags = itemTags, isValidated = isValidated) { 
                                selectedRecipeItem = item 
                            }
                            is Product -> ProductItemRow(item, 100, tags = itemTags, isValidated = isValidated) { 
                                selectedProductItem = item 
                            }
                        }
                    }
                }
            }

            TrainingAreaBottom(
                activeTab = activeTrainingTab ?: "Keiner",
                onTabSelected = { tab ->
                    activeTrainingTab = if (activeTrainingTab == tab) null else tab
                    nutritionTrainingType = SettingsManager.getNutritionTrainingType(ctx)
                }
            )
        }

        selectedFoodItem?.let { food ->
            FoodItemDetailDialog(food = food, onDismiss = { selectedFoodItem = null }, onAdd = { 
                showGramDialogForFood = food
                selectedFoodItem = null 
            })
        }

        selectedRecipeItem?.let { recipe ->
            RecipeDetailDialog(
                recipe = recipe, 
                daySummary = null, 
                userProfile = null, 
                onDismiss = { selectedRecipeItem = null },
                onAction = {
                    scope.launch(Dispatchers.IO) {
                        db.mealDao().upsert(MealEntry(dateEpochDay = dateEpochDay, recipeId = recipe.id))
                        launch(Dispatchers.Main) { selectedRecipeItem = null; onBack() }
                    }
                },
                actionLabel = "Zum Plan hinzufügen"
            )
        }

        selectedProductItem?.let { product ->
            ProductDetailDialog(
                product = product,
                grams = 100,
                onDismiss = { selectedProductItem = null },
                onAdd = {
                    showGramDialogForProduct = product
                    selectedProductItem = null
                }
            )
        }

        showGramDialogForFood?.let { food ->
            GramSelectionDialog(
                title = "Menge für ${food.name}",
                onDismiss = { showGramDialogForFood = null },
                onConfirm = { grams ->
                    scope.launch(Dispatchers.IO) {
                        val entry = FoodAdapter.mapFoodToMealEntry(food, grams, dateEpochDay)
                        db.mealDao().upsert(entry)
                        launch(Dispatchers.Main) { showGramDialogForFood = null; onBack() }
                    }
                },
                kcalPer100g = food.caloriesPer100g
            )
        }

        showGramDialogForProduct?.let { product ->
            GramSelectionDialog(
                title = "Menge für ${product.name}",
                onDismiss = { showGramDialogForProduct = null },
                onConfirm = { grams ->
                    scope.launch(Dispatchers.IO) {
                        db.mealDao().upsert(MealEntry(dateEpochDay = dateEpochDay, productId = product.id, grams = grams))
                        launch(Dispatchers.Main) { showGramDialogForProduct = null; onBack() }
                    }
                },
                kcalPer100g = product.kcal.toDouble()
            )
        }
    }
}

private fun getTagsForItem(item: Any): List<String> {
    return when (item) {
        is FoodItem -> PerformanceTagEngine.computeTags(item.caloriesPer100g, item.proteinPer100g, item.carbsPer100g, item.fatPer100g, item.vitaminC, item.potassium, item.magnesium, item.iron)
        is Recipe -> PerformanceTagEngine.computeTags(item.kcal.toDouble(), item.protein, item.carbs, item.fat)
        is Product -> PerformanceTagEngine.computeTags(item.kcal.toDouble(), item.protein, item.carbs, item.fat)
        else -> emptyList()
    }
}

private fun getName(item: Any): String {
    return when (item) {
        is FoodItem -> item.name
        is Recipe -> item.name
        is Product -> item.name
        else -> ""
    }
}

private fun getKcal(item: Any): Int {
    return when (item) {
        is FoodItem -> item.caloriesPer100g.toInt()
        is Recipe -> item.kcal
        is Product -> item.kcal
        else -> 0
    }
}

private fun getProtein(item: Any): Double {
    return when (item) {
        is FoodItem -> item.proteinPer100g
        is Recipe -> item.protein
        is Product -> item.protein
        else -> 0.0
    }
}

private fun getCarbs(item: Any): Double {
    return when (item) {
        is FoodItem -> item.carbsPer100g
        is Recipe -> item.carbs
        is Product -> item.carbs
        else -> 0.0
    }
}

private fun getVitaminC(item: Any): Double {
    return if (item is FoodItem) item.vitaminC else 0.0
}

private fun getPotassium(item: Any): Double {
    return if (item is FoodItem) item.potassium else 0.0
}

private fun getItemKey(item: Any): String {
    return when (item) {
        is FoodItem -> "food_${item.name}"
        is Recipe -> "recipe_${item.id}"
        is Product -> "product_${item.id}"
        else -> UUID.randomUUID().toString()
    }
}

private fun getNameKey(item: Any): String {
    return getName(item).trim().lowercase(Locale.ROOT)
}

@Composable
fun TrainingAreaBottom(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(color = Color(0xFF101010), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
            TabButton("PRE", activeTab == "pre") { onTabSelected("pre") }
            TabButton("POST", activeTab == "post") { onTabSelected("post") }
            TabButton("REGEN", activeTab == "regen") { onTabSelected("regen") }
        }
    }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (isSelected) FantasyColors.Accent else Color.Gray
    TextButton(onClick = onClick) {
        FText(text, color = color, bold = isSelected)
    }
}
