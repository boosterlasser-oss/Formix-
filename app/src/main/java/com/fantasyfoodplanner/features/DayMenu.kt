package com.fantasyfoodplanner.features

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fantasyfoodplanner.data.*
import com.fantasyfoodplanner.logic.*
import com.fantasyfoodplanner.ui.*
import com.fantasyfoodplanner.utils.NutrientCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs

@Composable
fun DayMenuScreen(
    date: LocalDate, 
    onBack: () -> Unit,
    onGoSearch: (Long) -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { AppDb.get(ctx) }
    val scope = rememberCoroutineScope()

    val allRecipes by db.recipeDao().getAll().collectAsState(initial = emptyList(), context = Dispatchers.Main.immediate)
    val allProducts by db.productDao().getAll().collectAsState(initial = emptyList(), context = Dispatchers.Main.immediate)
    val dayMeals by db.mealDao().mealsOn(date.toEpochDay()).collectAsState(initial = emptyList(), context = Dispatchers.Main.immediate)
    val manualMeals by db.manualMealDao().getForDay(date.toEpochDay()).collectAsState(initial = emptyList(), context = Dispatchers.Main.immediate)
    val userProfile by db.userDao().profile().collectAsState(initial = null, context = Dispatchers.Main.immediate)

    var selectedRecipeWithEntry by remember { mutableStateOf<Pair<Recipe, MealEntry>?>(null) }
    var selectedFoodWithEntry by remember { mutableStateOf<Pair<FoodItem, MealEntry>?>(null) }
    var selectedProductWithEntry by remember { mutableStateOf<Pair<Product, MealEntry>?>(null) }
    
    var showManualDialog by remember { mutableStateOf(false) }
    var showAiPlanDialog by remember { mutableStateOf(false) }
    var manualMealToDelete by remember { mutableStateOf<ManualMealEntry?>(null) }
    var showConfirmDeleteRecipe by remember { mutableStateOf(false) }
    var showConfirmDeleteFood by remember { mutableStateOf(false) }
    var showConfirmDeleteProduct by remember { mutableStateOf(false) }
    var showCoachSheet by remember { mutableStateOf(false) }
    val app = ctx.applicationContext as Application

    val recipesForDayWithEntries = remember(dayMeals, allRecipes) {
        dayMeals.mapNotNull { entry ->
            if (entry.recipeId != null) {
                allRecipes.find { it.id == entry.recipeId }?.to(entry)
            } else null
        }.sortedBy { it.first.name }
    }

    val foodsForDayWithEntries = remember(dayMeals) {
        dayMeals.mapNotNull { entry ->
            if (entry.foodName != null) {
                foodDatabase.find { it.name == entry.foodName }?.to(entry)
            } else null
        }.sortedBy { it.first.name }
    }

    val productsForDayWithEntries = remember(dayMeals, allProducts) {
        dayMeals.mapNotNull { entry ->
            if (entry.productId != null) {
                allProducts.find { it.id == entry.productId }?.to(entry)
            } else null
        }.sortedBy { it.first.name }
    }

    val totals = remember(dayMeals, manualMeals, allRecipes, allProducts) {
        NutrientCalculator.calculateTotals(dayMeals, manualMeals, allRecipes, allProducts)
    }

    val glowGreen = Color(0xFF00FF7F)

    FantasySurface {
        Box(
            Modifier
                .fillMaxSize()
        ) {
            Column(Modifier.fillMaxSize()) {
                MainAppBar("Plan: ${date.dayOfMonth}.${date.monthValue}.", onBack = onBack, showAI = true)

                userProfile?.let { profile ->
                    Surface(
                        color = Color.Black.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, FantasyColors.Gold.copy(0.4f))
                    ) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            NutrientProgress("Kcal", totals.kcal, profile.dailyKcalTarget)
                            NutrientProgress("Prot", totals.p.toInt(), profile.dailyProteinTarget)
                        }
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (recipesForDayWithEntries.isEmpty() && foodsForDayWithEntries.isEmpty() && productsForDayWithEntries.isEmpty() && manualMeals.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                FText("Keine Einträge.\nKI Plan erstellen lassen!", textAlign = TextAlign.Center, color = FantasyColors.GrayText)
                            }
                        }
                    } else {
                        if (recipesForDayWithEntries.isNotEmpty()) {
                            item {
                                FText("REZEPTE", sizeSp = 11, bold = true, color = FantasyColors.Accent, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                            }
                            items(recipesForDayWithEntries) { (recipe, entry) ->
                                val tags = PerformanceTagEngine.computeTags(recipe.kcal.toDouble(), recipe.protein, recipe.carbs, recipe.fat)
                                RecipeItemRow(recipe, tags = tags) { selectedRecipeWithEntry = recipe to entry }
                            }
                        }
                        if (foodsForDayWithEntries.isNotEmpty()) {
                            item {
                                FText("LEBENSMITTEL", sizeSp = 11, bold = true, color = FantasyColors.Accent, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                            }
                            items(foodsForDayWithEntries) { (food, entry) ->
                                val tags = PerformanceTagEngine.computeTags(food.caloriesPer100g, food.proteinPer100g, food.carbsPer100g, food.fatPer100g, food.vitaminC, food.potassium, food.magnesium, food.iron)
                                FoodItemRow(food, entry.grams, tags = tags) { selectedFoodWithEntry = food to entry }
                            }
                        }
                        if (productsForDayWithEntries.isNotEmpty()) {
                            item {
                                FText("PRODUKTE", sizeSp = 11, bold = true, color = FantasyColors.Accent, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                            }
                            items(productsForDayWithEntries) { (product, entry) ->
                                val tags = PerformanceTagEngine.computeTags(product.kcal.toDouble(), product.protein, product.carbs, product.fat)
                                ProductItemRow(product, entry.grams, tags = tags) { selectedProductWithEntry = product to entry }
                            }
                        }
                        if (manualMeals.isNotEmpty()) {
                            item {
                                FText("MANUELLE EINTRÄGE", sizeSp = 11, bold = true, color = FantasyColors.Accent, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                            }
                            items(manualMeals) { entry ->
                                ManualMealRow(entry, onDelete = { manualMealToDelete = entry })
                            }
                        }
                    }
                }

                Column(Modifier.padding(16.dp)) {
                    FantasyButton("KI: Tagesplan erstellen", Modifier.fillMaxWidth()) { showAiPlanDialog = true }
                    Spacer(Modifier.height(8.dp))
                    FantasyButton("Eigene Mahlzeit", Modifier.fillMaxWidth()) { showManualDialog = true }
                    Spacer(Modifier.height(8.dp))
                    FantasyButton("Datenbank-Suche", Modifier.fillMaxWidth(), alpha = 0.7f) { onGoSearch(date.toEpochDay()) }
                }
            }

            if (showAiPlanDialog) {
                AiPlanSelectionDialog(
                    profile = userProfile,
                    allRecipes = allRecipes,
                    consumedKcal = totals.kcal,
                    consumedP = totals.p,
                    consumedC = totals.c,
                    consumedF = totals.f,
                    onDismiss = { showAiPlanDialog = false },
                    onAddRecipe = { recipe ->
                        scope.launch(Dispatchers.IO) {
                            db.mealDao().upsert(MealEntry(dateEpochDay = date.toEpochDay(), recipeId = recipe.id))
                        }
                    }
                )
            }

            if (showManualDialog) {
                ManualMealDialog(
                    onDismiss = { showManualDialog = false },
                    onSave = { name, kcal, p, k, f ->
                        scope.launch(Dispatchers.IO) {
                            db.manualMealDao().insert(ManualMealEntry(
                                dateEpochDay = date.toEpochDay(),
                                name = name,
                                kcal = kcal,
                                protein = p,
                                carbs = k,
                                fat = f
                            ))
                            showManualDialog = false
                        }
                    }
                )
            }

            selectedRecipeWithEntry?.let { (recipe, entry) ->
                RecipeDetailDialog(
                    recipe = recipe,
                    daySummary = totals,
                    userProfile = userProfile,
                    onDismiss = { selectedRecipeWithEntry = null },
                    onAction = { showConfirmDeleteRecipe = true },
                    actionLabel = "Aus Plan entfernen"
                )
                if (showConfirmDeleteRecipe) {
                    AlertDialog(
                        onDismissRequest = { showConfirmDeleteRecipe = false },
                        title = { Text("Eintrag entfernen") },
                        text = { Text("Möchtest du \"${recipe.name}\" wirklich aus dem Plan entfernen?") },
                        confirmButton = {
                            FantasyButton("Entfernen") {
                                scope.launch(Dispatchers.IO) { db.mealDao().delete(entry) }
                                showConfirmDeleteRecipe = false
                                selectedRecipeWithEntry = null
                            }
                        },
                        dismissButton = {
                            FantasyButton("Abbrechen", alpha = 0.6f) { showConfirmDeleteRecipe = false }
                        }
                    )
                }
            }

            selectedFoodWithEntry?.let { (food, entry) ->
                FoodItemDetailDialog(
                    food = food,
                    onDismiss = { selectedFoodWithEntry = null },
                    onDelete = { showConfirmDeleteFood = true }
                )
                if (showConfirmDeleteFood) {
                    AlertDialog(
                        onDismissRequest = { showConfirmDeleteFood = false },
                        title = { Text("Eintrag entfernen") },
                        text = { Text("Möchtest du \"${food.name}\" wirklich entfernen?") },
                        confirmButton = {
                            FantasyButton("Entfernen") {
                                scope.launch(Dispatchers.IO) { db.mealDao().delete(entry) }
                                showConfirmDeleteFood = false
                                selectedFoodWithEntry = null
                            }
                        },
                        dismissButton = {
                            FantasyButton("Abbrechen", alpha = 0.6f) { showConfirmDeleteFood = false }
                        }
                    )
                }
            }

            selectedProductWithEntry?.let { (product, entry) ->
                ProductDetailDialog(
                    product = product,
                    grams = entry.grams,
                    onDismiss = { selectedProductWithEntry = null },
                    onDelete = { showConfirmDeleteProduct = true }
                )
                if (showConfirmDeleteProduct) {
                    AlertDialog(
                        onDismissRequest = { showConfirmDeleteProduct = false },
                        title = { Text("Eintrag entfernen") },
                        text = { Text("Möchtest du \"${product.name}\" wirklich entfernen?") },
                        confirmButton = {
                            FantasyButton("Entfernen") {
                                scope.launch(Dispatchers.IO) { db.mealDao().delete(entry) }
                                showConfirmDeleteProduct = false
                                selectedProductWithEntry = null
                            }
                        },
                        dismissButton = {
                            FantasyButton("Abbrechen", alpha = 0.6f) { showConfirmDeleteProduct = false }
                        }
                    )
                }
            }

            // Bestätigungsdialog für manuelle Mahlzeiten
            manualMealToDelete?.let { entry ->
                AlertDialog(
                    onDismissRequest = { manualMealToDelete = null },
                    title = { Text("Eintrag entfernen") },
                    text = { Text("Möchtest du \"${entry.name}\" wirklich entfernen?") },
                    confirmButton = {
                        FantasyButton("Entfernen") {
                            scope.launch(Dispatchers.IO) { db.manualMealDao().delete(entry) }
                            manualMealToDelete = null
                        }
                    },
                    dismissButton = {
                        FantasyButton("Abbrechen", alpha = 0.6f) { manualMealToDelete = null }
                    }
                )
            }
        }
    }
}

@Composable
fun NutrientProgress(label: String, value: Int, target: Int) {
    val progress = if (target > 0) value.toFloat() / target else 0f
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
        FText(label, sizeSp = 12, color = Color.White.copy(0.7f))
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .height(18.dp)
                .width(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(0.15f))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(FantasyColors.Accent, RoundedCornerShape(8.dp))
            )
            FText("$value / $target", sizeSp = 12, color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun ManualMealDialog(onDismiss: () -> Unit, onSave: (String, Int, Double, Double, Double) -> Unit) {
    val ctx = LocalContext.current

    // --- STATE: BLOCK A (Pro 100g) ---
    var kcal100 by remember { mutableStateOf("") }
    var p100 by remember { mutableStateOf("") }
    var c100 by remember { mutableStateOf("") }
    var f100 by remember { mutableStateOf("") }

    // --- STATE: BLOCK B (Portion) ---
    var amountG by remember { mutableStateOf("100") }
    var name by remember { mutableStateOf("") }

    // --- SCAN STATE ---
    var showLiveScanner by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var showSettingsHint by remember { mutableStateOf(false) }

    // Helper: Berechnete Portionswerte
    val factor = (amountG.toDoubleOrNull() ?: 100.0) / 100.0
    val kcalPortion = (kcal100.toDoubleOrNull() ?: 0.0) * factor
    val pPortion = (p100.replace(",",".").toDoubleOrNull() ?: 0.0) * factor
    val cPortion = (c100.replace(",",".").toDoubleOrNull() ?: 0.0) * factor
    val fPortion = (f100.replace(",",".").toDoubleOrNull() ?: 0.0) * factor

    // Plausibilitätscheck auf 100g Basis
    val kcalMacros100 = (p100.replace(",",".").toDoubleOrNull() ?: 0.0) * 4 + 
                        (c100.replace(",",".").toDoubleOrNull() ?: 0.0) * 4 + 
                        (f100.replace(",",".").toDoubleOrNull() ?: 0.0) * 9
    val kcalVal100 = kcal100.toDoubleOrNull() ?: 0.0
    val isPlausible = kcalVal100 > 0 && abs(kcalVal100 - kcalMacros100) <= maxOf(40.0, kcalVal100 * 0.25)
                     && (p100.replace(",",".").toDoubleOrNull() ?: 0.0) <= 120.0

    fun applyCandidate(res: OcrEngine.ScanResult) {
        // Selektives Merge: Nur tatsächlich erkannte Werte überschreiben
        // Bereits eingetragene Werte werden NICHT mit 0 überschrieben
        if (res.hasKcal && res.kcal100 > 0) kcal100 = res.kcal100.toInt().toString()
        if (res.hasProtein && res.protein100 > 0) p100 = String.format(Locale.US, "%.1f", res.protein100)
        if (res.hasCarbs && res.carbs100 > 0) c100 = String.format(Locale.US, "%.1f", res.carbs100)
        if (res.hasFat && res.fat100 > 0) f100 = String.format(Locale.US, "%.1f", res.fat100)
        if (res.portionSize != null) amountG = res.portionSize.toInt().toString()
        // Produktname aus Barcode-API übernehmen (wenn vorhanden und Name-Feld noch leer)
        if (!res.name.isNullOrBlank() && name.isBlank()) name = res.name
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms[Manifest.permission.CAMERA] == true) showLiveScanner = true else showSettingsHint = true
    }

    // ═══════ LIVE SCANNER FULLSCREEN ═══════
    // MUSS vor dem AlertDialog stehen, damit der Dialog nicht gerendert wird
    if (showLiveScanner) {
        Box(Modifier.fillMaxSize()) {
            LiveScannerScreen(
                onResult = { result ->
                    applyCandidate(result)
                    showLiveScanner = false
                },
                onDismiss = { showLiveScanner = false }
            )
        }
        return // Fullscreen: Dialog nicht anzeigen
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Row(verticalAlignment = Alignment.CenterVertically) {
            FText("Eigene Mahlzeit", color = FantasyColors.Accent, bold = true)
            if (isPlausible && kcal100.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Check, null, tint = Color.Green, modifier = Modifier.size(16.dp))
                FText("Validiert", sizeSp = 10, color = Color.Green)
            }
        }},
        containerColor = Color(0xFF121212),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FantasyButton("📷 Etikett scannen", Modifier.fillMaxWidth()) {
                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        showLiveScanner = true
                    } else {
                        showPermissionRationale = true
                    }
                }

                FantasyTextField(name, { name = it }, "Produktbezeichnung")

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // --- BLOCK A: PRO 100G ---
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FText("PRO 100 g", sizeSp = 12, bold = true, color = FantasyColors.Accent)
                        FantasyTextField(kcal100, { kcal100 = it }, "kcal", keyboardType = KeyboardType.Number)
                        FantasyTextField(p100, { p100 = it }, "Prot (g)", keyboardType = KeyboardType.Number)
                        FantasyTextField(c100, { c100 = it }, "KH (g)", keyboardType = KeyboardType.Number)
                        FantasyTextField(f100, { f100 = it }, "Fett (g)", keyboardType = KeyboardType.Number)
                    }

                    // --- BLOCK B: PORTION ---
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FText("PORTION", sizeSp = 12, bold = true, color = FantasyColors.Gold)
                        FantasyTextField(amountG, { amountG = it }, "Deine Menge (g)", keyboardType = KeyboardType.Number)
                        
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(0.05f)).padding(8.dp)) {
                            Column {
                                FText("${kcalPortion.toInt()} kcal", sizeSp = 14, bold = true)
                                FText("P: ${String.format(Locale.US, "%.1f", pPortion)}g", sizeSp = 12)
                                FText("K: ${String.format(Locale.US, "%.1f", cPortion)}g", sizeSp = 12)
                                FText("F: ${String.format(Locale.US, "%.1f", fPortion)}g", sizeSp = 12)
                            }
                        }
                    }
                }

                if (!isPlausible && kcal100.isNotEmpty()) {
                    Surface(color = Color.Red.copy(0.1f), shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            FText("Werte unplausibel! Bitte 100g-Block prüfen.", sizeSp = 10, color = Color.Red)
                        }
                    }
                }
            }
        },
        confirmButton = {
            FantasyButton("Eintragen") {
                if (name.isNotEmpty()) {
                    onSave(name, kcalPortion.toInt(), pPortion, cPortion, fPortion)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { FText("Abbrechen", color = Color.Gray) } }
    )


    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { FText("Berechtigung benötigt", color = FantasyColors.Accent) },
            containerColor = Color(0xFF1A1A1A),
            text = { FText("Die Kamera wird benötigt, um Nährwert-Etiketten zu scannen.", color = Color.White) },
            confirmButton = {
                FantasyButton("Verstanden") {
                    showPermissionRationale = false
                    permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                }
            },
            dismissButton = { TextButton(onClick = { showPermissionRationale = false }) { FText("Abbrechen") } }
        )
    }

    if (showSettingsHint) {
        AlertDialog(
            onDismissRequest = { showSettingsHint = false },
            title = { FText("Berechtigung verweigert", color = Color.Red) },
            containerColor = Color(0xFF1A1A1A),
            text = { FText("Bitte aktiviere den Kamerazugriff in den Einstellungen.", color = Color.White) },
            confirmButton = {
                FantasyButton("Einstellungen öffnen") {
                    showSettingsHint = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", ctx.packageName, null)
                    }
                    ctx.startActivity(intent)
                }
            },
            dismissButton = { TextButton(onClick = { showSettingsHint = false }) { FText("Schließen") } }
        )
    }
}

@Composable
fun AiPlanSelectionDialog(
    profile: UserProfile?,
    allRecipes: List<Recipe>,
    consumedKcal: Int,
    consumedP: Double,
    consumedC: Double,
    consumedF: Double,
    onDismiss: () -> Unit,
    onAddRecipe: (Recipe) -> Unit
) {
    if (profile == null) return
    
    var timingMode by remember { mutableStateOf(TimingMode.NEUTRAL) }
    var trainingIntensity by remember { mutableStateOf("MED") }
    var timeOffset by remember { mutableStateOf("60") }

    val result = remember(allRecipes, consumedKcal, consumedP, consumedC, consumedF, timingMode, trainingIntensity, timeOffset) {
        AiLogic.generateDayPlanWithContext(
            profile, allRecipes, consumedKcal, consumedP, consumedC, consumedF,
            timingMode = timingMode,
            trainingIntensity = trainingIntensity,
            timeOffsetMinutes = timeOffset.toIntOrNull() ?: 60
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121212),
        title = { 
            Column {
                FText("KI EMPFEHLUNGEN", color = FantasyColors.Accent, bold = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FText("Ziel: ", sizeSp = 12, color = FantasyColors.GrayText)
                    FText(result.goal.name, sizeSp = 12, color = FantasyColors.Accent, bold = true)
                }
            }
        },
        text = {
            Column {
                LazyRow(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = timingMode == TimingMode.PRE,
                            onClick = { timingMode = TimingMode.PRE },
                            label = { FText("Vor Training (PRE)", sizeSp = 10) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = timingMode == TimingMode.POST,
                            onClick = { timingMode = TimingMode.POST },
                            label = { FText("Nach Training (POST)", sizeSp = 10) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = timingMode == TimingMode.NEUTRAL,
                            onClick = { timingMode = TimingMode.NEUTRAL },
                            label = { FText("Neutral", sizeSp = 10) }
                        )
                    }
                }

                if (timingMode != TimingMode.NEUTRAL) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        FText("Minuten: ", sizeSp = 10, color = FantasyColors.GrayText)
                        FantasyTextField(timeOffset, { timeOffset = it }, "", Modifier.width(60.dp), keyboardType = KeyboardType.Number)
                        Spacer(Modifier.width(16.dp))
                        FText("Intensität: ", sizeSp = 10, color = FantasyColors.GrayText)
                        var expandedInt by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { expandedInt = true }) { FText(trainingIntensity, color = FantasyColors.Accent) }
                            DropdownMenu(expanded = expandedInt, onDismissRequest = { expandedInt = false }, modifier = Modifier.background(Color(0xFF1A1A1A))) {
                                listOf("LOW", "MED", "HIGH").forEach { i ->
                                    DropdownMenuItem(text = { FText(i) }, onClick = { trainingIntensity = i; expandedInt = false })
                                }
                            }
                        }
                    }
                }

                Divider(Modifier.padding(vertical = 8.dp), color = Color.DarkGray)

                LazyColumn(modifier = Modifier.fillMaxHeight(0.7f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val optimal = result.suggestions.filter { it.score >= 80 }
                    val good = result.suggestions.filter { it.score in 50..79 }
                    val suboptimal = result.suggestions.filter { it.score < 50 }

                    if (optimal.isNotEmpty()) {
                        item { FText("TOP EMPFEHLUNGEN", sizeSp = 11, bold = true, color = FantasyColors.Accent) }
                        items(optimal) { scored ->
                            AiSuggestionRow(scored) { onAddRecipe(scored.recipe) }
                        }
                    }
                    if (good.isNotEmpty()) {
                        item { Spacer(Modifier.height(8.dp)); FText("WEITERE PASSENDE OPTIONEN", sizeSp = 11, bold = true, color = FantasyColors.GrayText) }
                        items(good) { scored ->
                            AiSuggestionRow(scored) { onAddRecipe(scored.recipe) }
                        }
                    }
                    if (suboptimal.isNotEmpty()) {
                        item { Spacer(Modifier.height(8.dp)); FText("ALTERNATIVE (WENIGER IDEAL)", sizeSp = 11, bold = true, color = Color.DarkGray) }
                        items(suboptimal) { scored ->
                            AiSuggestionRow(scored) { onAddRecipe(scored.recipe) }
                        }
                    }
                }
            }
        },
        confirmButton = { FantasyButton("Fertig") { onDismiss() } }
    )
}

@Composable
fun AiSuggestionRow(scored: ScoredRecipe, onAdd: () -> Unit) {
    var added by remember { mutableStateOf(false) }
    val recipe = scored.recipe
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .border(1.dp, if (scored.score >= 80) FantasyColors.Accent.copy(0.4f) else Color.DarkGray, RoundedCornerShape(12.dp))
            .clickable { if (!added) { onAdd(); added = true } }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val categoryLabel = when (recipe.category) {
                        "fit" -> "FITNESS"
                        "build" -> "MUSKELAUFBAU"
                        "lose" -> "ABNEHMEN"
                        else -> recipe.category.uppercase()
                    }
                    Surface(color = FantasyColors.Accent.copy(0.12f), shape = RoundedCornerShape(4.dp), border = androidx.compose.foundation.BorderStroke(0.5.dp, FantasyColors.Accent.copy(0.3f))) {
                        FText(" $categoryLabel ", sizeSp = 9, color = FantasyColors.Accent, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    FText(scored.label, sizeSp = 12, bold = true)
                    Spacer(Modifier.width(8.dp))
                    FText("Score: ${scored.score}", sizeSp = 10, color = FantasyColors.GrayText)
                }
                FText(recipe.name, sizeSp = 16, bold = true, color = FantasyColors.Text)
                FText("${recipe.kcal} kcal | E: ${recipe.protein}g K: ${recipe.carbs}g", sizeSp = 12, color = FantasyColors.GrayText)
                FText(scored.reason, sizeSp = 11, color = FantasyColors.Accent.copy(0.9f), modifier = Modifier.padding(top = 4.dp))
                scored.betterAlternative?.let { alt ->
                    FText("Tipp: $alt", sizeSp = 10, color = FantasyColors.GrayText, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Icon(
                imageVector = if (added) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                tint = if (added) Color.Green else FantasyColors.Accent
            )
        }
    }
}
