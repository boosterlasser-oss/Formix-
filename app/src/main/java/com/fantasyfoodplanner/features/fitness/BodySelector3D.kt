package com.fantasyfoodplanner.features.fitness

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.fantasyfoodplanner.ui.FantasyColors
import com.fantasyfoodplanner.ui.FText

private const val BODY_PARTS_DIR = "animations/Körper"

data class BodyPart(
    val id: String,
    val name: String,
    val animationFile: String,
    val focusGroup: String
)

private val ALL_BODY_PARTS = listOf(
    BodyPart("chest", "Brust", "man-chest.json", "Brust"),
    BodyPart("biceps", "Bizeps", "man-biceps.json", "Bizeps"),
    BodyPart("triceps", "Trizeps", "man-triceps.json", "Trizeps"),
    BodyPart("shoulders", "Schultern", "man-shoulders.json", "Schultern"),
    BodyPart("back", "Rücken", "man-back.json", "Rücken"),
    BodyPart("abs", "Bauch", "man-abs.json", "Bauch / Core"),
    BodyPart("forearms", "Unterarme", "man-forearms.json", "Arme"),
    BodyPart("quads", "Beine", "man-thigs.json", "Beine"),
    BodyPart("hamstrings", "Beinbeuger", "man-hamstrings.json", "Beine"),
    BodyPart("calves", "Waden", "man-calves.json", "Beine"),
    BodyPart("glutes", "Po", "man-glutes.json", "Po / Gesäß"),
    BodyPart("neck", "Nacken", "man-neck.json", "Nacken"),
    BodyPart("chest_w", "Brust (W)", "woman-chest.json", "Brust"),
    BodyPart("biceps_w", "Bizeps (W)", "woman-biceps.json", "Bizeps"),
    BodyPart("triceps_w", "Trizeps (W)", "woman-triceps.json", "Trizeps"),
    BodyPart("shoulders_w", "Schultern (W)", "woman-shoulders.json", "Schultern"),
    BodyPart("back_w", "Rücken (W)", "woman-back.json", "Rücken"),
    BodyPart("abs_w", "Bauch (W)", "woman-abs.json", "Bauch / Core"),
    BodyPart("forearms_w", "Unterarme (W)", "woman-forearms.json", "Arme"),
    BodyPart("quads_w", "Beine (W)", "woman-thigs.json", "Beine"),
    BodyPart("hamstrings_w", "Beinbeuger (W)", "woman-hamstrings.json", "Beine"),
    BodyPart("calves_w", "Waden (W)", "woman-calves.json", "Beine"),
    BodyPart("glutes_w", "Po (W)", "woman-glutes.json", "Po / Gesäß"),
    BodyPart("neck_w", "Nacken (W)", "woman-neck.json", "Nacken"),
    BodyPart("back_calves", "Waden (Hinten)", "man-back-calves.json", "Beine"),
    BodyPart("back_hamstrings", "Beinbeuger (H)", "man-hamstrings.json", "Beine"),
    BodyPart("back_calves_w", "Waden (W H)", "woman-back-calves.json", "Beine"),
)

@Composable
fun BodySelector3D(
    selectedFocusGroups: Set<String>,
    onFocusGroupsChanged: (Set<String>) -> Unit
) {
    var expandedDropdown by remember { mutableStateOf(false) }
    var selectedBodyParts by remember { mutableStateOf(listOf<BodyPart>()) }

    LaunchedEffect(selectedFocusGroups) {
        if (selectedFocusGroups.isNotEmpty() && selectedBodyParts.isEmpty()) {
            selectedBodyParts = ALL_BODY_PARTS.filter { bodyPart ->
                selectedFocusGroups.contains(bodyPart.focusGroup)
            }
        }
    }

    LaunchedEffect(selectedBodyParts) {
        val focusGroups = selectedBodyParts.map { it.focusGroup }.toSet()
        onFocusGroupsChanged(focusGroups)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FText(
            "Was möchtest du trainieren?",
            sizeSp = 14,
            bold = true,
            color = Color.White,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { expandedDropdown = !expandedDropdown },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text(
                if (selectedBodyParts.isEmpty()) "Körperteile auswählen..."
                else "${selectedBodyParts.size} ausgewählt",
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Dropdown",
                tint = FantasyColors.Accent
            )
        }

        if (expandedDropdown) {
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 550.dp)
                    .background(Color(0xFF0A0A0A), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ALL_BODY_PARTS.chunked(2)) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { bodyPart ->
                            BodyPartCard(
                                bodyPart = bodyPart,
                                isSelected = selectedBodyParts.any { it.id == bodyPart.id },
                                onToggle = {
                                    selectedBodyParts = if (selectedBodyParts.any { it.id == bodyPart.id }) {
                                        selectedBodyParts.filter { it.id != bodyPart.id }
                                    } else {
                                        selectedBodyParts + bodyPart
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (selectedBodyParts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FText(
                "✓ ${selectedBodyParts.size} Körperteil${if (selectedBodyParts.size > 1) "e" else ""} ausgewählt",
                sizeSp = 12,
                bold = true,
                color = Color(0xFF2196F3),
                modifier = Modifier.align(Alignment.Start)
            )
        }
    }
}

@Composable
fun BodyPartCard(
    bodyPart: BodyPart,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var loadError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .background(
                if (isSelected) Color(0xFF2196F3).copy(alpha = 0.3f) else Color(0xFF1A1A1A),
                RoundedCornerShape(8.dp)
            )
            .border(
                2.dp,
                if (isSelected) Color(0xFF2196F3) else Color(0xFF444444),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onToggle)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Lottie Animation mit AndroidView (stabiler)
        if (!loadError) {
            AndroidView(
                factory = { ctx ->
                    LottieAnimationView(ctx).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        repeatCount = LottieDrawable.INFINITE
                        try {
                            setAnimation("$BODY_PARTS_DIR/${bodyPart.animationFile}")
                            playAnimation()
                        } catch (e: Exception) {
                            loadError = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )
        } else {
            // Fallback bei Fehler
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                FText("🏋️", sizeSp = 28, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        FText(
            bodyPart.name,
            sizeSp = 9,
            bold = isSelected,
            color = if (isSelected) Color(0xFF2196F3) else Color.White
        )
    }
}

