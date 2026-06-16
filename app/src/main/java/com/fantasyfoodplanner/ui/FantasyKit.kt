package com.fantasyfoodplanner.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.fantasyfoodplanner.logic.SubscriptionTier
import com.fantasyfoodplanner.logic.SubscriptionManager
import kotlinx.coroutines.delay
import kotlin.random.Random

object FantasyColors {
    // Heller, sportlicher Verlauf als Hintergrund
    val Background = Color(0xFF0D0D1A) // Dunkler Hintergrund, passend zum Fantasy-Gradient
    val CardBg = Color(0xFF1A1A2E) // Dunkler Kartenhintergrund, passend zum Fantasy-Gradient
    val Accent = Color(0xFF1A5CFF) // Sportliches Blau
    val Gold = Color(0xFFFFD700)
    val Secondary = Color(0xFFB8860B)
    val Text = Color(0xFFE8E8E8) // Helles Grau/Weiß für Lesbarkeit auf dunklem Grund
    val GrayText = Color(0xFFB0B0B0)
    val ButtonBg = Accent // Buttons sportlich blau
    val ButtonText = Color.White // Buttons immer weißer Text
}

@Composable
fun AIHeadIcon(modifier: Modifier = Modifier) {
    // Lottie Trainer Animation statt Canvas-Kopf
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.Asset("animations/trainer.json")
    )

    LottieAnimation(
        composition = composition,
        modifier = modifier.size(32.dp),
        isPlaying = true,
        iterations = LottieConstants.IterateForever,
        speed = 1f
    )
}

private val TRAINING_TIPS = listOf(
    "Technik vor Gewicht.",
    "Rumpf fest – Rücken neutral.",
    "Langsam ablassen, kontrolliert hoch.",
    "Atme beim Hochdrücken aus.",
    "Trink regelmäßig Wasser.",
    "Kurzes Warmup reduziert Verletzungsrisiko.",
    "Heute müde? Reduziere Gewicht um 5–10%.",
    "Saubere Wiederholungen zählen mehr als viele.",
    "Pause 60–90s für Kraft.",
    "Bewegungsradius voll nutzen – ohne Schmerz.",
    "Fokus auf den Muskel, den du trainierst.",
    "Beständigkeit ist der Schlüssel zum Erfolg.",
    "Schlaf ist genauso wichtig wie das Training.",
    "Höre auf deinen Körper – Pausen sind okay.",
    "Protein unterstützt deinen Muskelaufbau.",
    "Vermeide Schwung bei Kraftübungen.",
    "Setze dir kleine, erreichbare Ziele.",
    "Dokumentiere dein Training für Fortschritt.",
    "Dehnen nach dem Training fördert Beweglichkeit.",
    "Wärme deine Gelenke spezifisch auf.",
    "Die letzte Wiederholung sollte schwer sein.",
    "Atme tief in den Bauch ein.",
    "Halte die Spannung in der gesamten Bewegung.",
    "Frühstücke ausgewogen für Energie.",
    "Kein Schmerz, aber intensives Brennen ist gut."
)

private var lastTipIndex = -1

@Composable
fun TipBubble(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    val tip = remember { 
        var idx = Random.nextInt(TRAINING_TIPS.size)
        while (idx == lastTipIndex) idx = Random.nextInt(TRAINING_TIPS.size)
        lastTipIndex = idx
        TRAINING_TIPS[idx]
    }

    LaunchedEffect(Unit) {
        visible = true
        delay(4000)
        visible = false
    }

    if (visible) {
        Box(
            modifier = modifier
                .widthIn(max = 220.dp)
                .background(Color(0xFF151515), RoundedCornerShape(12.dp))
                .border(1.dp, FantasyColors.Accent, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            FText(text = tip, sizeSp = 13, color = Color.White)
        }
    }
}

@Composable
fun FantasySurface(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Fitness-Gradient-Hintergrund (Sporthintergrund überall)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1A5CFF), // sportliches Blau
                            Color(0xFF00FF7F), // sportliches Grün
                            Color(0xFFF5F6FA)  // helles Grau/Weiß
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
        )
        // Overlay für Lesbarkeit (60% Schwarz)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
        )
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {
            content()
        }
    }
}

// Anpassung: Lesbarkeit auf weißen Karten sicherstellen
@Composable
fun FText(
    text: String,
    sizeSp: Int = 16,
    bold: Boolean = false,
    color: Color = FantasyColors.Text,
    textAlign: TextAlign? = null,
    italic: Boolean = false,
    highlight: Boolean = false, // NEU: Hervorhebung
    modifier: Modifier = Modifier
) {
    val effectiveColor = when {
        highlight -> Color.Yellow // Hervorgehobener Text
        else -> color
    }
    Text(
        text = text,
        fontSize = sizeSp.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        color = effectiveColor,
        textAlign = textAlign,
        modifier = modifier
    )
}

@Composable
fun FantasyButton(label: String, modifier: Modifier = Modifier, alpha: Float = 1f, enabled: Boolean = true, onClick: () -> Unit) {
    val effectiveAlpha = if (enabled) alpha else 0.4f
    Box(
        modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .alpha(effectiveAlpha)
            .clip(RoundedCornerShape(16.dp))
            .background(FantasyColors.ButtonBg)
            .border(1.dp, FantasyColors.Accent, RoundedCornerShape(16.dp))
            .then(if (enabled) Modifier.clickable(onClickLabel = label) { onClick() } else Modifier)
            .semantics { role = Role.Button; contentDescription = label }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        FText(label, sizeSp = 16, bold = true, color = FantasyColors.ButtonText)
    }
}

@Composable
fun FantasyTextField(
    value: String, 
    onValueChange: (String) -> Unit, 
    label: String, 
    modifier: Modifier = Modifier, 
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { FText(label, color = FantasyColors.GrayText) },
        modifier = modifier,
        textStyle = androidx.compose.ui.text.TextStyle(color = FantasyColors.Text),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FantasyColors.Accent,
            unfocusedBorderColor = FantasyColors.Secondary,
            cursorColor = FantasyColors.Accent,
            focusedLabelColor = FantasyColors.Accent,
            unfocusedLabelColor = FantasyColors.GrayText,
            focusedTextColor = FantasyColors.Text,
            unfocusedTextColor = FantasyColors.Text
        ),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun MainAppBar(title: String, onBack: () -> Unit, showAI: Boolean = false, actions: @Composable (RowScope.() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text("←", Modifier.clickable { onBack() }.padding(8.dp), color = FantasyColors.Accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        FText(title, sizeSp = 20, bold = true, color = FantasyColors.Accent, modifier = Modifier.weight(1f))
        if (actions != null) {
            actions()
        }
        if (showAI) {
            AIHeadIcon()
        }
    }
}

@Composable
fun FantasyCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = FantasyColors.CardBg,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    alpha: Float = 1f,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Dashboard Action Card – Material Design 3 Card für die Hauptseite.
 * Professionelles Layout: Icon in rundem Container mit grünem Glow,
 * Titel + Untertitel rechts daneben, Chevron am Ende.
 */
@Composable
fun DashboardActionCard(
    title: String,
    iconResId: Int,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isPrimary: Boolean = false,
    iconSize: androidx.compose.ui.unit.Dp = 56.dp,
    onClick: () -> Unit
) {
    val cardBg = if (isPrimary) {
        Brush.linearGradient(
            colors = listOf(
                FantasyColors.Accent,
                Color(0xFF3D7AFF)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                FantasyColors.CardBg,
                Color(0xFF1F1F3A)
            )
        )
    }
    val glowGreen = Color(0xFF00FF7F)
    val borderColor = if (isPrimary) glowGreen.copy(alpha = 0.35f) else glowGreen.copy(alpha = 0.18f)

    Box(modifier = modifier.fillMaxWidth()) {
        // ── Grüner Glow hinter der Card (3 Schichten, abnehmende Deckkraft) ──
        Box(
            Modifier
                .matchParentSize()
                .padding(horizontal = 2.dp, vertical = 6.dp)
                .background(glowGreen.copy(alpha = if (isPrimary) 0.10f else 0.05f), RoundedCornerShape(26.dp))
        )
        Box(
            Modifier
                .matchParentSize()
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .background(glowGreen.copy(alpha = if (isPrimary) 0.07f else 0.03f), RoundedCornerShape(24.dp))
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable { onClick() },
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = iconResId),
                        contentDescription = title,
                        modifier = Modifier.size(iconSize),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(Modifier.width(14.dp))

                    // ── Text ──
                    Column(modifier = Modifier.weight(1f)) {
                        FText(
                            text = title,
                            sizeSp = 16,
                            bold = true,
                            color = if (isPrimary) Color.White else FantasyColors.Text
                        )
                        if (subtitle != null) {
                            Spacer(Modifier.height(3.dp))
                            FText(
                                text = subtitle,
                                sizeSp = 12,
                                color = if (isPrimary) Color.White.copy(alpha = 0.75f) else FantasyColors.GrayText
                            )
                        }
                    }

                    // ── Chevron ──
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                glowGreen.copy(alpha = if (isPrimary) 0.15f else 0.08f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        FText(
                            text = "›",
                            sizeSp = 18,
                            bold = true,
                            color = if (isPrimary) Color.White.copy(alpha = 0.85f) else glowGreen.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Feature-Gate Komponenten (Subscription)
// ─────────────────────────────────────────────

@Composable
fun FeatureGate(
    requiredTier: SubscriptionTier,
    featureName: String,
    content: @Composable () -> Unit
) {
    val ctx = LocalContext.current
    val currentTier = SubscriptionManager.getCurrentTier(ctx)
    val hasAccess = when (requiredTier) {
        SubscriptionTier.FREE -> true
        SubscriptionTier.PREMIUM -> currentTier != SubscriptionTier.FREE
    }
    if (hasAccess) {
        content()
    } else {
        LockedFeatureCard(featureName, requiredTier)
    }
}

@Composable
fun LockedFeatureCard(featureName: String, requiredTier: SubscriptionTier) {
    var showUpgrade by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(0.7f)
            .clickable(onClickLabel = "$featureName freischalten") { showUpgrade = true }
            .semantics { role = Role.Button; contentDescription = "$featureName gesperrt - ${requiredTier.name} erforderlich" },
        colors = CardDefaults.cardColors(containerColor = FantasyColors.CardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, FantasyColors.Accent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, contentDescription = "Gesperrt", tint = FantasyColors.GrayText)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                FText(featureName, bold = true)
                FText(
                    "${requiredTier.name} erforderlich",
                    sizeSp = 12,
                    color = FantasyColors.GrayText
                )
            }
            Icon(Icons.Default.ArrowForward, contentDescription = "Upgraden", tint = FantasyColors.Accent)
        }
    }
    if (showUpgrade) {
        UpgradeDialog(requiredTier) { showUpgrade = false }
    }
}

@Composable
fun UpgradeDialog(requiredTier: SubscriptionTier, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FantasyColors.CardBg,
        title = {
            FText(
                "Upgrade auf ${requiredTier.name}",
                bold = true,
                sizeSp = 18,
                color = FantasyColors.Accent
            )
        },
        text = {
            Column {
                FText(
                    when (requiredTier) {
                        SubscriptionTier.PREMIUM -> "Alle Trainingstypen, Barcode-Scanner, 500+ Rezepte, Cloud-Backup und unbegrenzte Workouts!"
                        else -> ""
                    }
                )
                Spacer(Modifier.height(12.dp))
                FText(
                    when (requiredTier) {
                        SubscriptionTier.PREMIUM -> "Ab 2,99 EUR/Monat oder 19,99 EUR/Jahr"
                        else -> ""
                    },
                    color = FantasyColors.Accent,
                    bold = true
                )
            }
        },
        confirmButton = {
            FantasyButton("Jetzt upgraden") {
                // BillingManager wird hier aufgerufen (nach Integration)
                onDismiss()
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                FText("Spaeter", color = FantasyColors.GrayText)
            }
        }
    )
}


/**
 * Kalorien-Status-Card für das Dashboard.
 * Zeigt heutige Kalorien mit Fortschrittsbalken.
 */
@Composable
fun CalorieStatusCard(
    currentKcal: Int,
    targetKcal: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (targetKcal > 0) (currentKcal.toFloat() / targetKcal.toFloat()).coerceIn(0f, 1f) else 0f
    val progressColor = when {
        progress > 0.9f -> Color(0xFFFF6B6B) // rot wenn fast am Limit
        progress > 0.7f -> Color(0xFFFFD93D) // gelb
        else -> Color(0xFF00FF7F) // grün
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = FantasyColors.CardBg),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, FantasyColors.Accent.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            FText("Kalorien heute", sizeSp = 14, color = FantasyColors.GrayText)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                FText("$currentKcal", sizeSp = 32, bold = true, color = FantasyColors.Accent)
                Spacer(Modifier.width(4.dp))
                FText("/ $targetKcal kcal", sizeSp = 16, color = FantasyColors.GrayText, modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = progressColor,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
}

// ─────────────────────────────────────────────
// Typography-System
// ─────────────────────────────────────────────

object FantasyTypography {
    val displayLarge  = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold,   lineHeight = 40.sp)
    val headlineLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold,   lineHeight = 32.sp)
    val headlineMedium= TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold,   lineHeight = 28.sp)
    val bodyLarge     = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp)
    val bodyMedium    = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp)
    val bodySmall     = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp)
    val labelLarge    = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
    val labelSmall    = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp)
}

// ─────────────────────────────────────────────
// Spacing-System
// ─────────────────────────────────────────────

object FantasySpacing {
    val xs  =  4.dp
    val s   =  8.dp
    val m   = 16.dp
    val l   = 24.dp
    val xl  = 32.dp
    val xxl = 48.dp
}

// ─────────────────────────────────────────────
// ErrorCard – Fehleranzeige mit optionalem Retry
// ─────────────────────────────────────────────

@Composable
fun ErrorCard(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    val errorColor = Color(0xFFFF6B6B)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = errorColor.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, errorColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Fehler",
                tint = errorColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                FText("Fehler", sizeSp = 14, bold = true, color = errorColor)
                Spacer(Modifier.height(2.dp))
                FText(message, sizeSp = 12, color = FantasyColors.Text)
            }
            if (onRetry != null) {
                IconButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = "Erneut versuchen", tint = errorColor)
                }
            }
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Schliessen", tint = FantasyColors.GrayText)
                }
            }
        }
    }
}

