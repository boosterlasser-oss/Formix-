package com.fantasyfoodplanner.features

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.ProductDetails
import com.fantasyfoodplanner.logic.BillingManager
import com.fantasyfoodplanner.logic.SubscriptionTier
import com.fantasyfoodplanner.ui.*

/**
 * Vollständiger Upgrade/Paywall-Screen mit:
 * - Preisvergleich aller Tiers
 * - Feature-Liste pro Plan
 * - "Beliebteste Wahl"-Badge (Jährlich)
 * - Echte Billing-Integration via BillingManager
 */
@Composable
fun UpgradeScreen(
    onBack: () -> Unit,
    highlightTier: SubscriptionTier = SubscriptionTier.PREMIUM,
    billingManager: BillingManager? = null
) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    var selectedPlan by remember { mutableStateOf(PlanType.PREMIUM_YEARLY) }

    // Produkte aus dem BillingManager beobachten
    val products by (billingManager?.products ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList<ProductDetails>()))
        .collectAsState()
    val isConnected by (billingManager?.isConnected ?: kotlinx.coroutines.flow.MutableStateFlow(false))
        .collectAsState()

    // Fehlermeldung wenn Kauf nicht möglich
    var errorMessage by remember { mutableStateOf<String?>(null) }

    FantasySurface {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header ──────────────────────────────────────────
            MainAppBar(title = "FORMIX Upgrade", onBack = onBack)

            Column(Modifier.padding(horizontal = 16.dp)) {

                // Slogan
                FText(
                    "Hol das Maximum aus deinem Training heraus.",
                    sizeSp = 16,
                    color = FantasyColors.GrayText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))

                // ── Plan-Auswahl-Tabs ────────────────────────────
                FText("PLAN WÄHLEN", sizeSp = 11, bold = true, color = FantasyColors.GrayText)
                Spacer(Modifier.height(8.dp))

                // Premium-Sektion
                PlanSectionHeader(
                    tier = SubscriptionTier.PREMIUM,
                    tagline = "Alle Features freischalten",
                    highlight = highlightTier == SubscriptionTier.PREMIUM
                )
                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlanCard(
                        type = PlanType.PREMIUM_MONTHLY,
                        selected = selectedPlan == PlanType.PREMIUM_MONTHLY,
                        modifier = Modifier.weight(1f)
                    ) { selectedPlan = PlanType.PREMIUM_MONTHLY }
                    PlanCard(
                        type = PlanType.PREMIUM_YEARLY,
                        selected = selectedPlan == PlanType.PREMIUM_YEARLY,
                        isBestValue = true,
                        modifier = Modifier.weight(1f)
                    ) { selectedPlan = PlanType.PREMIUM_YEARLY }
                }

                Spacer(Modifier.height(24.dp))

                            // ── Feature-Vergleich ────────────────────────────
                FText("WAS IST ENTHALTEN", sizeSp = 11, bold = true, color = FantasyColors.GrayText)
                Spacer(Modifier.height(8.dp))
                FeatureComparisonTable(selectedTier = selectedPlan.tier)

                Spacer(Modifier.height(24.dp))

                // ── Kaufen-Button ────────────────────────────────
                val buttonLabel = when {
                    !isConnected -> "Verbinde mit Play Store..."
                    products.none { it.productId == selectedPlan.productId } -> "Lade Preise..."
                    else -> "Jetzt ${selectedPlan.label} kaufen"
                }
                val buttonEnabled = isConnected &&
                    products.any { it.productId == selectedPlan.productId } &&
                    activity != null &&
                    billingManager != null

                FantasyButton(
                    label = buttonLabel,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = buttonEnabled
                ) {
                    val productDetails = products.find { it.productId == selectedPlan.productId }
                    if (productDetails != null && activity != null && billingManager != null) {
                        billingManager.launchPurchase(activity, productDetails)
                    } else {
                        errorMessage = "Verbindung zum Play Store nicht möglich. Bitte Internet prüfen und erneut versuchen."
                    }
                }

                // Fehleranzeige
                errorMessage?.let { msg ->
                    Spacer(Modifier.height(8.dp))
                    ErrorCard(message = msg)
                    LaunchedEffect(msg) {
                        kotlinx.coroutines.delay(4000)
                        errorMessage = null
                    }
                }

                Spacer(Modifier.height(8.dp))

                FText(
                    "Jederzeit kündbar. Keine versteckten Kosten.",
                    sizeSp = 11,
                    color = FantasyColors.GrayText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────
// Plan-Typen
// ────────────────────────────────────────────────────────────────

enum class PlanType(
    val label: String,
    val price: String,
    val period: String,
    val productId: String,
    val tier: SubscriptionTier,
    val savingsBadge: String? = null
) {
    PREMIUM_MONTHLY("Premium Monatlich", "2,99 EUR", "/Monat", BillingManager.PREMIUM_MONTHLY, SubscriptionTier.PREMIUM),
    PREMIUM_YEARLY( "Premium Jährlich",  "19,99 EUR", "/Jahr",  BillingManager.PREMIUM_YEARLY,  SubscriptionTier.PREMIUM, "44% Ersparnis")
}

// ────────────────────────────────────────────────────────────────
// Plan-Sektion Header
// ────────────────────────────────────────────────────────────────

@Composable
private fun PlanSectionHeader(
    tier: SubscriptionTier,
    tagline: String,
    highlight: Boolean
) {
    val tierColor = FantasyColors.Accent
    Row(
        Modifier
            .fillMaxWidth()
            .background(tierColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FText(
            tier.name,
            sizeSp = 15,
            bold = true,
            color = tierColor,
            modifier = Modifier.weight(1f)
        )
        FText(tagline, sizeSp = 12, color = FantasyColors.GrayText)
    }
}

// ────────────────────────────────────────────────────────────────
// Einzelne Plan-Card
// ────────────────────────────────────────────────────────────────

@Composable
private fun PlanCard(
    type: PlanType,
    selected: Boolean,
    modifier: Modifier = Modifier,
    isBestValue: Boolean = false,
    onClick: () -> Unit
) {
    val accentColor = FantasyColors.Accent
    val borderColor = if (selected) accentColor else FantasyColors.GrayText.copy(alpha = 0.3f)
    val bgColor = if (selected) accentColor.copy(alpha = 0.12f) else FantasyColors.CardBg

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
            onClick = onClick,
            colors = CardDefaults.cardColors(containerColor = bgColor),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FText(type.period, sizeSp = 10, color = FantasyColors.GrayText)
                Spacer(Modifier.height(4.dp))
                FText(type.price, sizeSp = 14, bold = true, color = if (selected) accentColor else FantasyColors.Text)
                if (type.savingsBadge != null) {
                    Spacer(Modifier.height(4.dp))
                    FText(type.savingsBadge, sizeSp = 9, color = Color(0xFF00FF7F), bold = true)
                }
            }
        }
        // "Beliebteste Wahl"-Badge
        if (isBestValue) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-10).dp)
                    .background(accentColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                FText("Beliebt", sizeSp = 9, bold = true, color = Color.White)
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────
// Feature-Vergleich Tabelle
// ────────────────────────────────────────────────────────────────

private data class FeatureRow(val name: String, val free: Boolean, val premium: Boolean)

private val FEATURES = listOf(
    FeatureRow("Manuelles Kalorien-Tracking",     true,  true),
    FeatureRow("3 Trainingstypen",                true,  true),
    FeatureRow("50 Basis-Rezepte",                true,  true),
    FeatureRow("Ernaehrungsmodul (Tagesplaner)",  false, true),
    FeatureRow("500+ Rezepte & Datenbank",        false, true),
    FeatureRow("Barcode-Scanner",                 false, true),
    FeatureRow("Alle 5 Trainingstypen",           false, true),
    FeatureRow("Unbegrenzte Workouts",            false, true),
    FeatureRow("Vollstaendige Statistiken",       false, true),
    FeatureRow("Datenexport (CSV/JSON)",          false, true),
)

@Composable
private fun FeatureComparisonTable(selectedTier: SubscriptionTier) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FantasyColors.CardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Spalten-Header
            Row(Modifier.fillMaxWidth()) {
                FText("Feature", sizeSp = 11, bold = true, color = FantasyColors.GrayText, modifier = Modifier.weight(3f))
                FText("Free",    sizeSp = 11, bold = true, color = FantasyColors.GrayText, modifier = Modifier.weight(2f), textAlign = TextAlign.Center)
                FText("Premium", sizeSp = 11, bold = true, color = if (selectedTier == SubscriptionTier.PREMIUM) FantasyColors.Accent else FantasyColors.GrayText, modifier = Modifier.weight(2f), textAlign = TextAlign.Center)
            }

            Divider(color = FantasyColors.GrayText.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

            FEATURES.forEach { feat ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FText(feat.name, sizeSp = 12, color = FantasyColors.Text, modifier = Modifier.weight(3f))
                    FeatureCheck(feat.free,    SubscriptionTier.FREE,    selectedTier, Modifier.weight(2f))
                    FeatureCheck(feat.premium, SubscriptionTier.PREMIUM, selectedTier, Modifier.weight(2f))
                }
            }
        }
    }
}

@Composable
private fun FeatureCheck(
    available: Boolean,
    columnTier: SubscriptionTier,
    selectedTier: SubscriptionTier,
    modifier: Modifier = Modifier
) {
    val isHighlighted = columnTier == selectedTier
    val iconColor = if (available) Color(0xFF00FF7F) else FantasyColors.GrayText.copy(alpha = 0.3f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (available) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Enthalten",
                tint = iconColor,
                modifier = Modifier.size(if (isHighlighted) 20.dp else 16.dp)
            )
        } else {
            FText("–", sizeSp = 14, color = FantasyColors.GrayText.copy(alpha = 0.3f))
        }
    }
}
