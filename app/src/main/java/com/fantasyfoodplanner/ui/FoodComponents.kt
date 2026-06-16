package com.fantasyfoodplanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fantasyfoodplanner.data.*
import com.fantasyfoodplanner.logic.NutritionTrainingType
import com.fantasyfoodplanner.logic.SettingsManager
import com.fantasyfoodplanner.utils.DaySummaryData
import java.util.Locale

@Composable
fun NutrientStat(label: String, value: String) {
    Column {
        FText(label, sizeSp = 10, color = Color.Gray)
        FText(value, sizeSp = 15, bold = true)
    }
}

@Composable
fun NutrientInfo(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FText(label, color = Color.Gray, sizeSp = 10)
        FText(value, bold = true, sizeSp = 15, color = Color.White)
    }
}

/**
 * Zentrale Master-Card für alle Suchergebnisse und Plan-Einträge.
 */
@Composable
fun GoldSearchItemCard(
    title: String,
    subtitle: String,
    categoryLabel: String,
    tags: List<String> = emptyList(),
    isValidated: Boolean = false,
    icon: ImageVector = Icons.Default.Info,
    iconColor: Color = FantasyColors.Accent.copy(alpha = 0.6f),
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A))
            .border(1.dp, FantasyColors.Accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                FText(categoryLabel.uppercase(), sizeSp = 10, color = FantasyColors.Accent, bold = true)
                Spacer(Modifier.height(2.dp))
                FText(title, sizeSp = 17, bold = true, color = FantasyColors.Text)
                FText(subtitle, sizeSp = 13, color = FantasyColors.GrayText)
                if (tags.isNotEmpty() || isValidated) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tags.take(4).forEach { tag ->
                            Surface(
                                color = FantasyColors.Accent.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, FantasyColors.Accent.copy(alpha = 0.25f))
                            ) {
                                FText(tag, sizeSp = 9, color = FantasyColors.Accent, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        if (isValidated) {
                            Surface(
                                color = Color(0xFF1B5E20).copy(alpha = 0.25f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
                            ) {
                                FText("Validiert ✅", sizeSp = 9, color = Color(0xFF81C784), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(FantasyColors.Accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun RecipeItemRow(recipe: Recipe, tags: List<String> = emptyList(), isValidated: Boolean = false, onClick: () -> Unit) {
    GoldSearchItemCard(
        title = recipe.name,
        subtitle = "REZEPT | ${recipe.kcal} kcal | E: ${recipe.protein}g K: ${recipe.carbs}g",
        categoryLabel = "Rezepte",
        tags = tags,
        isValidated = isValidated,
        onClick = onClick
    )
}

@Composable
fun FoodItemRow(food: FoodItem, grams: Int, tags: List<String> = emptyList(), isValidated: Boolean = false, onClick: () -> Unit) {
    val factor = grams / 100.0
    val kcal = (food.caloriesPer100g * factor).toInt()
    val prot = String.format(Locale.GERMANY, "%.1fg", food.proteinPer100g * factor)
    GoldSearchItemCard(
        title = if (grams == 100) food.name else "${food.name} ($grams g)",
        subtitle = "${food.category.uppercase()} | $kcal kcal | E: $prot",
        categoryLabel = food.category,
        tags = tags.ifEmpty { food.performanceTags },
        isValidated = isValidated,
        onClick = onClick
    )
}

@Composable
fun ProductItemRow(product: Product, grams: Int, tags: List<String> = emptyList(), isValidated: Boolean = false, onClick: () -> Unit) {
    val factor = grams / 100.0
    val kcal = (product.kcal * factor).toInt()
    val prot = String.format(Locale.GERMANY, "%.1fg", product.protein * factor)
    GoldSearchItemCard(
        title = if (grams == 100) product.name else "${product.name} ($grams g)",
        subtitle = "PRODUKT | $kcal kcal | E: $prot",
        categoryLabel = "Produkte",
        tags = tags,
        isValidated = isValidated,
        onClick = onClick
    )
}

@Composable
fun ManualMealRow(entry: ManualMealEntry, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF121212))
            .border(1.dp, FantasyColors.Gold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                FText("EIGENE MAHLZEIT", sizeSp = 10, color = FantasyColors.Gold, bold = true)
                FText(entry.name, sizeSp = 18, bold = true, color = FantasyColors.Gold)
                FText("${entry.kcal} kcal | E: ${entry.protein}g K: ${entry.carbs}g F: ${entry.fat}g", color = Color.Gray, sizeSp = 14)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = Color.Red.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun TrainingAreaBottom(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    val ctx = LocalContext.current
    var currentTrainingType by remember { mutableStateOf(SettingsManager.getNutritionTrainingType(ctx)) }
    var showTypePicker by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF0D0B01),
        border = androidx.compose.foundation.BorderStroke(1.dp, FantasyColors.Accent.copy(0.3f))
    ) {
        Column {
            // Trainingsart Auswahl (Scope Punkt 3 & 8)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FText("Trainingsart:", sizeSp = 11, color = Color.Gray)
                TextButton(onClick = { showTypePicker = true }) {
                    FText(currentTrainingType.name.replace("_", "/"), sizeSp = 11, color = FantasyColors.Accent, bold = true)
                }
            }

            // Tabs (Scope Punkt 2)
            Row(Modifier.fillMaxWidth()) {
                val tabs = listOf("Pre-Workout", "Post-Workout", "Regeneration")
                tabs.forEach { tab ->
                    val isActive = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onTabSelected(tab) }
                            .background(if (isActive) FantasyColors.Accent.copy(0.1f) else Color.Transparent)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FText(
                            text = tab,
                            sizeSp = 12,
                            bold = isActive,
                            color = if (isActive) FantasyColors.Accent else Color.Gray
                        )
                        if (isActive) {
                            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.5f).height(2.dp).background(FantasyColors.Accent))
                        }
                    }
                }
            }
        }
    }

    if (showTypePicker) {
        AlertDialog(
            onDismissRequest = { showTypePicker = false },
            title = { FText("Trainingsart wählen") },
            containerColor = Color(0xFF151515),
            text = {
                Column {
                    NutritionTrainingType.values().forEach { type ->
                        TextButton(
                            onClick = {
                                currentTrainingType = type
                                SettingsManager.setNutritionTrainingType(ctx, type)
                                showTypePicker = false
                                onTabSelected(activeTab) // Refresh current list
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FText(type.name.replace("_", "/"), color = if (currentTrainingType == type) FantasyColors.Accent else Color.White)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun GramSelectionDialog(title: String, onDismiss: () -> Unit, onConfirm: (Int) -> Unit, kcalPer100g: Double) {
    var gramText by remember { mutableStateOf("100") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { FText(title, color = FantasyColors.Accent, bold = true) },
        containerColor = Color(0xFF151515),
        text = {
            Column {
                FText("Wie viel Gramm?", sizeSp = 14)
                Spacer(Modifier.height(8.dp))
                FantasyTextField(gramText, { gramText = it }, "Gramm", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(12.dp))
                val grams = gramText.toIntOrNull() ?: 0
                val factor = grams / 100.0
                FText("Hochgerechnet: ${(kcalPer100g * factor).toInt()} kcal", sizeSp = 12, color = Color.Gray)
            }
        },
        confirmButton = {
            FantasyButton("Hinzufügen") {
                val grams = gramText.toIntOrNull() ?: 100
                onConfirm(grams.coerceIn(1, 2000))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { FText("Abbrechen", color = Color.Gray) } }
    )
}

@Composable
fun RecipeDetailDialog(recipe: Recipe, daySummary: DaySummaryData?, userProfile: UserProfile?, onDismiss: () -> Unit, onAction: (() -> Unit)? = null, actionLabel: String = "Entfernen") {
    val tags = remember(recipe) {
        PerformanceTagEngine.computeTags(recipe.kcal.toDouble(), recipe.protein, recipe.carbs, recipe.fat)
    }
    val ingredientList = remember(recipe) {
        recipe.ingredients.split(Regex(",\\s*|\\n")).filter { it.isNotBlank() }
    }
    val instructionSteps = remember(recipe) {
        val steps = recipe.instructions.split(Regex("\\.\\s*|\\n+")).filter { it.isNotBlank() }
        if (steps.size <= 1 && steps.none { it.matches(Regex("^\\d\\..*")) }) {
            steps.map { it.trim() }
        } else {
            steps.mapIndexed { i, s -> s.trim().removePrefix("${i + 1}.").trim() }.filter { it.isNotBlank() }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { FText(recipe.name, color = FantasyColors.Accent, bold = true) },
        containerColor = Color(0xFF121212),
        text = {
            Box(Modifier.fillMaxWidth()) {
                LazyColumn(Modifier.fillMaxWidth()) {
                    item {
                        Row(Modifier.fillMaxWidth().background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            NutrientInfo("Kcal", recipe.kcal.toString())
                            NutrientInfo("E", "${recipe.protein}g")
                            NutrientInfo("K", "${recipe.carbs}g")
                            NutrientInfo("F", "${recipe.fat}g")
                        }
                        Spacer(Modifier.height(16.dp))
                        
                        FlowRow(mainAxisSpacing = 8.dp, crossAxisSpacing = 8.dp) {
                            tags.forEach { tag ->
                                Surface(color = FantasyColors.Accent.copy(0.1f), shape = RoundedCornerShape(4.dp), border = androidx.compose.foundation.BorderStroke(1.dp, FantasyColors.Accent)) {
                                    FText(tag, sizeSp = 11, color = FantasyColors.Accent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        FText("ZUTATEN", bold = true, color = FantasyColors.Accent, sizeSp = 18)
                        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = Color.DarkGray)
                        Column(Modifier.padding(start = 8.dp)) {
                            ingredientList.forEach { ingredient ->
                                Row(Modifier.padding(vertical = 2.dp)) {
                                    FText("• ", color = FantasyColors.Accent, bold = true, sizeSp = 15)
                                    FText(ingredient.trim(), sizeSp = 15, color = Color.White)
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        FText("ZUBEREITUNG", bold = true, color = FantasyColors.Accent, sizeSp = 18)
                        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = Color.DarkGray)
                        Spacer(Modifier.height(8.dp))
                        if (instructionSteps.isEmpty()) {
                            Text(text = recipe.instructions, color = Color.White, fontSize = 16.sp, lineHeight = 24.sp, modifier = Modifier.padding(bottom = 16.dp))
                        } else {
                            instructionSteps.forEachIndexed { i, step ->
                                Row(Modifier.padding(bottom = 12.dp)) {
                                    Box(Modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).background(FantasyColors.Accent).padding(0.dp), contentAlignment = Alignment.Center) {
                                        FText("${i + 1}", bold = true, sizeSp = 13, color = Color.Black)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(text = step, color = Color.White, fontSize = 16.sp, lineHeight = 24.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        if (recipe.category.isNotBlank()) {
                            val categoryLabel = when (recipe.category) {
                                "fit" -> "FITNESS"
                                "build" -> "MUSKELAUFBAU"
                                "lose" -> "ABNEHMEN"
                                else -> recipe.category.uppercase()
                            }
                            Surface(color = FantasyColors.Accent.copy(0.15f), shape = RoundedCornerShape(6.dp)) {
                                FText(" $categoryLabel ", sizeSp = 13, color = FantasyColors.Accent, bold = true, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        if (onAction != null) { Spacer(Modifier.height(24.dp)); FantasyButton(actionLabel, Modifier.fillMaxWidth(), alpha = 0.8f) { onAction() } }
                    }
                }
            }
        },
        confirmButton = { FantasyButton("Schließen") { onDismiss() } }
    )
}

@Composable
fun FoodItemDetailDialog(food: FoodItem, onDismiss: () -> Unit, onAdd: (() -> Unit)? = null, onDelete: (() -> Unit)? = null) {
    val tags = remember(food) {
        PerformanceTagEngine.computeTags(
            food.caloriesPer100g, food.proteinPer100g, food.carbsPer100g, food.fatPer100g,
            food.vitaminC, food.potassium, food.magnesium, food.iron
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { FText(food.name, color = FantasyColors.Accent, bold = true) },
        containerColor = Color(0xFF121212),
        text = {
            LazyColumn(Modifier.fillMaxWidth()) {
                item {
                    FText("Nährwerte pro 100g", bold = true, sizeSp = 14, color = FantasyColors.GrayText)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { NutrientStat("Kalorien", "${food.caloriesPer100g.toInt()} kcal"); NutrientStat("Eiweiß", "${food.proteinPer100g} g") }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) { NutrientStat("Kohlenhydrate", "${food.carbsPer100g} g"); NutrientStat("Fett", "${food.fatPer100g} g") }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) { NutrientStat("Kalium", "${food.potassium.toInt()} mg"); NutrientStat("Vitamin C", "${food.vitaminC.toInt()} mg") }
                    Spacer(Modifier.height(20.dp))
                    FText("Analyse-Tags", bold = true, sizeSp = 14, color = FantasyColors.GrayText)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(mainAxisSpacing = 8.dp, crossAxisSpacing = 8.dp) { 
                        tags.forEach { tag -> 
                            Surface(color = FantasyColors.Accent.copy(0.1f), shape = RoundedCornerShape(4.dp), border = androidx.compose.foundation.BorderStroke(1.dp, FantasyColors.Accent)) { 
                                FText(tag, sizeSp = 11, color = FantasyColors.Accent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) 
                            } 
                        } 
                    }
                    Spacer(Modifier.height(20.dp))
                    FText("Training-Nutzen", bold = true, sizeSp = 14, color = FantasyColors.GrayText)
                    Spacer(Modifier.height(8.dp))
                    val description = buildString { 
                        if (tags.contains("pre_workout")) append("Ideal als Energielieferant vor intensiven Einheiten. ")
                        if (tags.contains("post_workout")) append("Unterstützt die Regeneration und füllt Speicher nach dem Training auf. ")
                        if (tags.contains("regeneration")) append("Unterstützt die Erholung durch Mikronährstoffe. ")
                        if (tags.contains("Elektrolyt")) append("Hilft beim Ausgleich des Mineralstoffhaushalts. ")
                        if (tags.contains("Muskelaufbau")) append("Unterstützt die Muskelbiosynthese. ")
                        if (tags.contains("Diätfreundlich")) append("Perfekt für Phasen der Gewichtsreduktion geeignet. ")
                        if (isEmpty()) append("Eine gesunde Ergänzung für deinen täglichen Nährstoffbedarf.") 
                    }
                    FText(description, sizeSp = 14, color = Color.White)
                    if (onAdd != null) { Spacer(Modifier.height(24.dp)); FantasyButton("Zum Plan hinzufügen", Modifier.fillMaxWidth()) { onAdd() } }
                    if (onDelete != null) { Spacer(Modifier.height(16.dp)); FantasyButton("Aus Plan entfernen", Modifier.fillMaxWidth(), alpha = 0.8f) { onDelete() } }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { FText("Schließen", color = Color.Gray) } }
    )
}

@Composable
fun ProductDetailDialog(
    product: Product,
    grams: Int,
    onDismiss: () -> Unit,
    onAdd: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val tags = remember(product) {
        PerformanceTagEngine.computeTags(product.kcal.toDouble(), product.protein, product.carbs, product.fat)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { FText("${product.name} ($grams g)", color = FantasyColors.Accent, bold = true) },
        containerColor = Color(0xFF121212),
        text = {
            Column {
                val factor = grams / 100.0
                val kcalVal = (product.kcal * factor).toInt().toString()
                val eVal = String.format(Locale.GERMANY, "%.1fg", product.protein * factor)
                val kVal = String.format(Locale.GERMANY, "%.1fg", product.carbs * factor)
                val fVal = String.format(Locale.GERMANY, "%.1fg", product.fat * factor)
                Row(Modifier.fillMaxWidth().background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) { 
                    NutrientInfo("Kcal", kcalVal)
                    NutrientInfo("E", eVal)
                    NutrientInfo("K", kVal)
                    NutrientInfo("F", fVal) 
                }
                Spacer(Modifier.height(16.dp))
                FlowRow(mainAxisSpacing = 8.dp, crossAxisSpacing = 8.dp) {
                    tags.forEach { tag ->
                        Surface(color = FantasyColors.Accent.copy(0.1f), shape = RoundedCornerShape(4.dp), border = androidx.compose.foundation.BorderStroke(1.dp, FantasyColors.Accent)) {
                            FText(tag, sizeSp = 11, color = FantasyColors.Accent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                if (onAdd != null) {
                    FantasyButton("Zum Plan hinzufügen", Modifier.fillMaxWidth()) { onAdd() }
                }
                if (onDelete != null) {
                    Spacer(Modifier.height(16.dp))
                    FantasyButton("Aus Plan entfernen", Modifier.fillMaxWidth(), alpha = 0.8f) { onDelete() }
                }
            }
        },
        confirmButton = { FantasyButton("Schließen") { onDismiss() } }
    )
}

@Composable
fun TrainingInsightCard(insights: List<String>) {
    Surface(color = Color(0xFF151515), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, FantasyColors.Accent.copy(0.6f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            FText("Training-Insight", sizeSp = 14, bold = true, color = FantasyColors.Accent)
            Spacer(Modifier.height(8.dp))
            insights.forEach { text -> Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) { FText("• ", color = FantasyColors.Accent, bold = true); FText(text, sizeSp = 13, color = Color.White) } }
        }
    }
}

@Composable
fun FlowRow(modifier: Modifier = Modifier, mainAxisSpacing: Dp = 0.dp, crossAxisSpacing: Dp = 0.dp, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val sequences = mutableListOf<List<Placeable>>()
        val crossAxisSizes = mutableListOf<Int>()
        var currentSequence = mutableListOf<Placeable>()
        var currentMainAxisSize = 0
        var currentCrossAxisSize = 0
        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)
            if (currentMainAxisSize + placeable.width > constraints.maxWidth && currentSequence.isNotEmpty()) {
                sequences.add(currentSequence)
                crossAxisSizes.add(currentCrossAxisSize)
                currentSequence = mutableListOf()
                currentMainAxisSize = 0
                currentCrossAxisSize = 0
            }
            currentSequence.add(placeable)
            currentMainAxisSize += placeable.width + mainAxisSpacing.roundToPx()
            currentCrossAxisSize = maxOf(currentCrossAxisSize, placeable.height)
        }
        sequences.add(currentSequence)
        crossAxisSizes.add(currentCrossAxisSize)
        val height = crossAxisSizes.sum() + (crossAxisSizes.size - 1) * crossAxisSpacing.roundToPx()
        layout(constraints.maxWidth, height) {
            var y = 0
            sequences.forEachIndexed { i, sequence ->
                var x = 0
                sequence.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + mainAxisSpacing.roundToPx()
                }
                y += crossAxisSizes[i] + crossAxisSpacing.roundToPx()
            }
        }
    }
}
