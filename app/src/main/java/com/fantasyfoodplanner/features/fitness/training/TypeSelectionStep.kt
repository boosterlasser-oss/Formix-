package com.fantasyfoodplanner.features.fitness.training

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fantasyfoodplanner.logic.SubscriptionManager
import com.fantasyfoodplanner.logic.SubscriptionTier
import com.fantasyfoodplanner.logic.TrainingType
import com.fantasyfoodplanner.ui.*

/**
 * Schritt 1: Trainingstyp-Auswahl (CrossFit, Kraft, Basics, Home)
 * Free-Nutzer haben nur Zugang zu BASICS, HOME, OTHER_ACTIVITY.
 */
@Composable
fun TypeSelectionStep(
    selectedType: TrainingType,
    onTypeSelected: (TrainingType) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val allowedTypes = remember { SubscriptionManager.getAllowedTrainingTypes(ctx) }
    var showUpgrade by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        MainAppBar("Trainings-Modus", onBack = onBack)
        FText("Waehle deinen Fokus fuer heute:", sizeSp = 18, bold = true, highlight = true)
        Spacer(Modifier.height(20.dp))
        TrainingType.entries.forEach { type ->
            val label = when (type) {
                TrainingType.CROSSFIT -> "CrossFit Performance"
                TrainingType.STRENGTH -> "Krafttraining (Gym)"
                TrainingType.BASICS -> "Fitness Basics"
                TrainingType.HOME -> "Zuhause Workout"
                TrainingType.OTHER_ACTIVITY -> "Andere Sportart / Aktivitaet"
            }
            val isLocked = type !in allowedTypes
            if (isLocked) {
                LockedFeatureCard("$label", SubscriptionTier.PREMIUM)
            } else {
                FantasyButton(
                    label = label,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    alpha = if (selectedType == type) 1f else 0.6f
                ) {
                    onTypeSelected(type)
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.weight(1f))
        FantasyButton("Bestaetigen & Weiter", Modifier.fillMaxWidth()) {
            onConfirm()
        }
    }

    if (showUpgrade) {
        UpgradeDialog(SubscriptionTier.PREMIUM) { showUpgrade = false }
    }
}

