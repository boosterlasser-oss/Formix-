package com.fantasyfoodplanner.features

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.fantasyfoodplanner.audio.SynthSound
import com.fantasyfoodplanner.ui.FantasyColors
import com.fantasyfoodplanner.ui.FText
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Coach Fly-In Animation – Premium Cinematic Version
 *
 * KOMPAKT (~10 Sekunden) mit 3D-Hintergrund-Szene, Neon-Farben + Sound-Effekten.
 *
 * Erststart (isFirstTime=true):
 *   3D Tokyo-Szene, Coach fliegt ein, stellt sich mit Sprechblasen vor.
 *
 * Wiederkehrend (isFirstTime=false):
 *   3D Collision-World-Szene, Coach fliegt ein, motiviert mit zufälligem Spruch.
 *
 * Phasen (10s total):
 *   Phase 1: 0.00–0.15 → Einfliegen von links (~1.5s) + Whoosh-Sound
 *   Phase 2: 0.15–0.85 → Schweben + 3 Sprechblasen (~7s) + Pop-Sounds
 *   Phase 3: 0.85–1.00 → Schrumpfen + Abflug (~1.5s) + Arpeggio-Sound
 */

// ══════════════════════════════════════════════
// Neon-Farbpalette aus AppBaukasten
// ══════════════════════════════════════════════
private val NeonCyan = Color(0xFF00F5FF)
private val NeonMagenta = Color(0xFFFF00E5)
private val NeonGreen = Color(0xFF39FF14)
private val NeonPurple = Color(0xFF7C4DFF)
private val NeonYellow = Color(0xFFFFEA00)
private val DarkBg = Color(0xFF050510)
private val CardBg = Color(0xFF0D0D1A)

@Composable
fun CoachFlyInAnimation(
    isFirstTime: Boolean = false,
    onFinished: () -> Unit
) {
    val config = LocalConfiguration.current
    val screenWidthDp = config.screenWidthDp.dp
    val screenHeightDp = config.screenHeightDp.dp

    // ═══ Sprechblasen-Texte ═══
    val firstTimeBubbles = listOf(
        "Hey! Ich bin dein KI-Coach! 💪",
        "Zusammen erreichen wir dein Ziel!",
        "Lass uns loslegen!"
    )
    val returningBubbles = remember {
        listOf(
            listOf("Willkommen zurück, Champ! 🔥", "Heute wird ein guter Tag!", "Los geht's!"),
            listOf("Na, bereit für Action? 💪", "Dein Körper wartet auf dich!", "Let's go!"),
            listOf("Schön, dass du da bist! ⚡", "Jeder Tag zählt!", "Auf geht's!"),
            listOf("Der Coach ist ready! 🏋️", "Du bist stärker als gestern!", "Zeig's allen!"),
            listOf("Yo! Keine Ausreden heute! 🔥", "Push dich ans Limit!", "Du schaffst das!"),
            listOf("Hey Champion! 💥", "Disziplin schlägt Motivation!", "Rein da!")
        ).random()
    }
    val bubbles = if (isFirstTime) firstTimeBubbles else returningBubbles

    // ═══ Animations-Steuerung: 10 Sekunden total ═══
    // Phase 1: 0.00–0.15 → Einfliegen (~1.5s)
    // Phase 2: 0.15–0.85 → Schweben + Sprechblasen (~7.0s)
    //   Bubble 1: 0.15–0.38
    //   Bubble 2: 0.38–0.62
    //   Bubble 3: 0.62–0.85
    // Phase 3: 0.85–1.00 → Schrumpfen + Abflug (~1.5s)

    var targetProgress by remember { mutableFloatStateOf(0f) }
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 10000, easing = LinearEasing),
        label = "flyProgress"
    )

    // Glow-Puls (Neon-Cyan/Magenta abwechselnd)
    val glowTransition = rememberInfiniteTransition(label = "glow")
    val glowPulse by glowTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "glowPulse"
    )

    // Neon-Farbwechsel (Cyan → Magenta → Cyan)
    val neonColorShift by glowTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "neonShift"
    )

    // Lottie Coach Head
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.Asset("animations/trainer.json")
    )

    // Welche Bubble gerade aktiv ist
    val activeBubble = when {
        progress < 0.17f -> -1   // Einfliegen
        progress < 0.40f -> 0    // Bubble 1
        progress < 0.64f -> 1    // Bubble 2
        progress < 0.87f -> 2    // Bubble 3
        else -> -1               // Abflug
    }

    // Bubble-Alpha (sanftes Ein-/Ausblenden)
    val bubbleAlpha = when {
        progress < 0.17f -> 0f
        progress < 0.19f -> (progress - 0.17f) / 0.02f      // Fade in Bubble 1
        progress < 0.37f -> 1f
        progress < 0.40f -> 1f - (progress - 0.37f) / 0.03f  // Fade out
        progress < 0.42f -> (progress - 0.40f) / 0.02f       // Fade in Bubble 2
        progress < 0.60f -> 1f
        progress < 0.64f -> 1f - (progress - 0.60f) / 0.04f  // Fade out
        progress < 0.66f -> (progress - 0.64f) / 0.02f       // Fade in Bubble 3
        progress < 0.83f -> 1f
        progress < 0.87f -> 1f - (progress - 0.83f) / 0.04f  // Fade out
        else -> 0f
    }.coerceIn(0f, 1f)

    // ═══ Sound-Trigger ═══
    var whooshPlayed by remember { mutableStateOf(false) }
    var pop1Played by remember { mutableStateOf(false) }
    var pop2Played by remember { mutableStateOf(false) }
    var pop3Played by remember { mutableStateOf(false) }
    var fanfarePlayed by remember { mutableStateOf(false) }

    LaunchedEffect(progress) {
        // Whoosh beim Einfliegen
        if (progress > 0.02f && !whooshPlayed) {
            whooshPlayed = true
            SynthSound.play(SynthSound.cinematicWhoosh)
        }
        // Pop bei jeder Bubble
        if (progress > 0.17f && !pop1Played) {
            pop1Played = true
            SynthSound.play(SynthSound.pop)
        }
        if (progress > 0.40f && !pop2Played) {
            pop2Played = true
            SynthSound.play(SynthSound.pop)
        }
        if (progress > 0.64f && !pop3Played) {
            pop3Played = true
            SynthSound.play(SynthSound.pop)
        }
        // Aufsteigende Fanfare beim Abflug
        if (progress > 0.85f && !fanfarePlayed) {
            fanfarePlayed = true
            SynthSound.play(SynthSound.correct)
        }
    }

    // Start
    LaunchedEffect(Unit) {
        delay(200)
        targetProgress = 1f
    }

    // Wenn fertig
    LaunchedEffect(progress) {
        if (progress >= 0.99f) {
            delay(400)
            onFinished()
        }
    }

    // ═══ Coach Position + Größe berechnen ═══
    val p = progress
    val posX: Float
    val posY: Float
    val coachScale: Float
    val bodyAlpha: Float
    val rotation: Float

    when {
        p <= 0.15f -> {
            // Phase 1: Einfliegen von links unten (~1.5s)
            val t = p / 0.15f
            val ease = FastOutSlowInEasing.transform(t)
            posX = lerp(-0.3f, 0.45f, ease)
            posY = lerp(1.1f, 0.42f, ease)
            coachScale = lerp(0.4f, 1.5f, ease)
            bodyAlpha = 1f
            rotation = lerp(-30f, -3f, ease)
        }
        p <= 0.85f -> {
            // Phase 2: Schweben in der Mitte + Sprechblasen (~7s)
            val t = (p - 0.15f) / 0.70f
            posX = 0.48f + kotlin.math.sin(t * Math.PI.toFloat() * 3) * 0.04f
            posY = 0.40f + kotlin.math.sin(t * Math.PI.toFloat() * 2) * 0.03f
            coachScale = 1.5f + kotlin.math.sin(t * Math.PI.toFloat() * 4) * 0.08f
            bodyAlpha = 1f
            rotation = kotlin.math.sin(t * Math.PI.toFloat() * 3) * 4f
        }
        else -> {
            // Phase 3: Schrumpfen + in obere rechte Ecke (~1.5s)
            val t = (p - 0.85f) / 0.15f
            val ease = FastOutSlowInEasing.transform(t)
            posX = lerp(0.48f, 0.92f, ease)
            posY = lerp(0.40f, 0.06f, ease)
            coachScale = lerp(1.5f, 0.25f, ease)
            bodyAlpha = lerp(1f, 0f, ease)
            rotation = lerp(0f, 20f, ease)
        }
    }

    // Neon-Glow-Farbe (pulsiert zwischen Cyan und Magenta)
    val neonGlowColor = lerpColor(NeonCyan, NeonMagenta, neonColorShift)

    // ═══════════════ RENDERING ═══════════════
    Box(Modifier.fillMaxSize().background(DarkBg)) {

        // ═══ 3D-HINTERGRUND (SceneView mit GLB-Modell) ═══
        val sceneNodes = remember { mutableStateListOf<Node>() }
        val glbPath = if (isFirstTime) "3d/littlest_tokyo.glb" else "3d/collision_world.glb"

        Scene(
            modifier = Modifier.fillMaxSize(),
            nodes = sceneNodes,
            onCreate = { sceneView ->
                // Dunkler Skybox-Hintergrund passend zum Neon-Theme
                try {
                    val skybox = com.google.android.filament.Skybox.Builder()
                        .color(0.02f, 0.02f, 0.06f, 1.0f)
                        .build(sceneView.engine)
                    sceneView.skybox = skybox
                } catch (e: Exception) {
                    Log.w("CoachFlyIn", "Skybox-Fehler: ${e.message}")
                }

                // Kamera-Position (etwas zurück + leicht von oben)
                sceneView.cameraNode.apply {
                    position = Position(0.0f, 1.5f, 4.0f)
                    verticalFovDegrees = 55.0f
                    lookAt(targetPosition = Position(0.0f, 0.0f, 0.0f))
                }

                // 3D-Modell laden
                try {
                    val modelNode = ModelNode(
                        engine = sceneView.engine,
                        modelGlbFileLocation = glbPath,
                        autoAnimate = true,
                        scaleUnits = 2.5f,
                        centerOrigin = Position(0.0f, -1.0f, 0.0f),
                        onError = { e -> Log.w("CoachFlyIn", "3D-Modell Fehler: $e") },
                        onLoaded = { Log.d("CoachFlyIn", "3D-Modell geladen: $glbPath") }
                    )
                    sceneNodes += modelNode
                } catch (e: Exception) {
                    Log.w("CoachFlyIn", "ModelNode-Fehler: ${e.message}")
                }
            },
            onFrame = { _ ->
                // Langsame automatische Kamera-Rotation für cinematic Effekt
                // (Die Szene dreht sich leicht von selbst)
            }
        )

        // ═══ NEON OVERLAY (dunkler Gradient über der 3D-Szene) ═══
        Canvas(Modifier.fillMaxSize()) {
            // Dunkler Overlay damit Coach-Figur gut sichtbar ist
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.4f),
                        Color.Black.copy(alpha = 0.15f),
                        Color.Black.copy(alpha = 0.5f)
                    )
                )
            )

            // Neon-Lichteffekte am oberen und unteren Rand
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(NeonCyan.copy(alpha = 0.08f * glowPulse), Color.Transparent)
                ),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.3f, size.height * 0.05f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(NeonMagenta.copy(alpha = 0.06f * glowPulse), Color.Transparent)
                ),
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.7f, size.height * 0.95f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(NeonPurple.copy(alpha = 0.05f * glowPulse), Color.Transparent)
                ),
                radius = size.width * 0.4f,
                center = Offset(size.width * 0.8f, size.height * 0.3f)
            )

            // ═══ NEON-FITNESSGERÄTE (schwebende Silhouetten) ═══
            drawNeonFitnessEquipment(glowPulse, neonColorShift)
        }

        // ═══ SPEED TRAILS (Neon) ═══
        if (p < 0.30f && p > 0.02f) {
            Canvas(Modifier.fillMaxSize()) {
                val trailAlpha = if (p < 0.15f) p / 0.15f * 0.6f else (0.30f - p) / 0.15f * 0.3f
                drawNeonSpeedTrail(posX, posY, trailAlpha, neonGlowColor)
            }
        }

        // ═══ COACH FIGUR ═══
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .offset(
                        x = screenWidthDp * (posX - 0.5f),
                        y = screenHeightDp * (posY - 0.5f)
                    )
            ) {
                // Neon-Glow hinter dem Coach
                Box(
                    Modifier
                        .size((110 * coachScale).dp)
                        .alpha(glowPulse * (1f - (p - 0.85f).coerceAtLeast(0f) * 6.7f).coerceAtLeast(0f))
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    neonGlowColor.copy(alpha = 0.35f),
                                    NeonPurple.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                        .align(Alignment.Center)
                )

                // Muskulöser Körper (Canvas)
                if (bodyAlpha > 0.05f) {
                    Canvas(
                        Modifier
                            .size((80 * coachScale).dp)
                            .alpha(bodyAlpha)
                            .rotate(rotation)
                            .align(Alignment.Center)
                    ) {
                        drawMuscularBody(p, neonGlowColor)
                    }
                }

                // Lottie Coach Head
                Box(
                    Modifier
                        .size((40 * coachScale).dp)
                        .offset(y = (-25 * coachScale).dp)
                        .rotate(rotation * 0.3f)
                        .align(Alignment.Center)
                ) {
                    LottieAnimation(
                        composition = composition,
                        modifier = Modifier.fillMaxSize(),
                        isPlaying = true,
                        iterations = LottieConstants.IterateForever,
                        speed = 1.2f
                    )
                }
            }
        }

        // ═══ SPRECHBLASE (Neon-Style) ═══
        if (activeBubble >= 0 && activeBubble < bubbles.size && bubbleAlpha > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    Modifier
                        .padding(bottom = 100.dp)
                        .alpha(bubbleAlpha)
                ) {
                    // Dreieck (Zeiger nach oben zum Coach)
                    Canvas(
                        Modifier
                            .size(20.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = (-8).dp)
                    ) {
                        val path = Path().apply {
                            moveTo(size.width / 2, 0f)
                            lineTo(0f, size.height)
                            lineTo(size.width, size.height)
                            close()
                        }
                        drawPath(path, color = CardBg)
                    }

                    // Sprechblasen-Box mit Neon-Gradient-Rand
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(CardBg, DarkBg)
                                )
                            )
                            .padding(2.dp) // Neon-Rand
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(CardBg, Color(0xFF0A0A18))
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FText(
                                bubbles[activeBubble],
                                sizeSp = 20, bold = true,
                                color = NeonCyan,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // ═══ NEON-RAND-GLOW oben und unten ═══
        Canvas(Modifier.fillMaxSize()) {
            // Oberer Neon-Streifen (Cyan)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(NeonCyan.copy(alpha = 0.15f * glowPulse), Color.Transparent),
                    startY = 0f, endY = size.height * 0.03f
                ),
                size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.03f)
            )
            // Unterer Neon-Streifen (Magenta)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, NeonMagenta.copy(alpha = 0.12f * glowPulse)),
                    startY = size.height * 0.97f, endY = size.height
                ),
                topLeft = Offset(0f, size.height * 0.97f),
                size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.03f)
            )
        }
    }
}

// ════════════════════════════════════════════
// MUSKULÖSER KÖRPER (Neon-Version)
// ════════════════════════════════════════════

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMuscularBody(
    progress: Float,
    neonAccent: Color
) {
    val w = size.width
    val h = size.height
    val cx = w / 2
    val cy = h / 2

    val accentBlue = Color(0xFF1A5CFF)
    val darkBlue = Color(0xFF0D2D6B)
    val skinTone = Color(0xFFE8B88A)

    // Torso (V-Form)
    val torsoPath = Path().apply {
        moveTo(cx - w * 0.35f, cy - h * 0.15f)
        lineTo(cx - w * 0.12f, cy + h * 0.35f)
        lineTo(cx + w * 0.12f, cy + h * 0.35f)
        lineTo(cx + w * 0.35f, cy - h * 0.15f)
        close()
    }
    drawPath(torsoPath, Brush.verticalGradient(listOf(darkBlue, accentBlue)))

    // Brust
    drawOval(accentBlue.copy(alpha = 0.6f), Offset(cx - w * 0.2f, cy - h * 0.1f), androidx.compose.ui.geometry.Size(w * 0.17f, h * 0.12f))
    drawOval(accentBlue.copy(alpha = 0.6f), Offset(cx + w * 0.03f, cy - h * 0.1f), androidx.compose.ui.geometry.Size(w * 0.17f, h * 0.12f))

    // Arme
    val leftArm = Path().apply {
        moveTo(cx - w * 0.35f, cy - h * 0.15f)
        lineTo(cx - w * 0.48f, cy + h * 0.05f)
        lineTo(cx - w * 0.45f, cy + h * 0.25f)
        lineTo(cx - w * 0.35f, cy + h * 0.2f)
        lineTo(cx - w * 0.3f, cy); close()
    }
    drawPath(leftArm, skinTone)
    drawCircle(skinTone.copy(alpha = 0.8f), w * 0.06f, Offset(cx - w * 0.4f, cy + h * 0.02f))

    val rightArm = Path().apply {
        moveTo(cx + w * 0.35f, cy - h * 0.15f)
        lineTo(cx + w * 0.48f, cy + h * 0.05f)
        lineTo(cx + w * 0.45f, cy + h * 0.25f)
        lineTo(cx + w * 0.35f, cy + h * 0.2f)
        lineTo(cx + w * 0.3f, cy); close()
    }
    drawPath(rightArm, skinTone)
    drawCircle(skinTone.copy(alpha = 0.8f), w * 0.06f, Offset(cx + w * 0.4f, cy + h * 0.02f))

    // Sixpack
    for (row in 0..2) {
        val y = cy + h * 0.05f + row * h * 0.08f
        drawLine(darkBlue.copy(alpha = 0.4f), Offset(cx - w * 0.06f, y), Offset(cx + w * 0.06f, y), 2f)
    }
    drawLine(darkBlue.copy(alpha = 0.3f), Offset(cx, cy - h * 0.05f), Offset(cx, cy + h * 0.3f), 2f)

    // Beine
    drawPath(Path().apply {
        moveTo(cx - w * 0.12f, cy + h * 0.35f); lineTo(cx - w * 0.18f, cy + h * 0.5f)
        lineTo(cx - w * 0.05f, cy + h * 0.5f); lineTo(cx - w * 0.02f, cy + h * 0.35f); close()
    }, darkBlue)
    drawPath(Path().apply {
        moveTo(cx + w * 0.12f, cy + h * 0.35f); lineTo(cx + w * 0.18f, cy + h * 0.5f)
        lineTo(cx + w * 0.05f, cy + h * 0.5f); lineTo(cx + w * 0.02f, cy + h * 0.35f); close()
    }, darkBlue)

    // Neon-Cape (pulsierend in Neon-Farbe statt nur Grün)
    if (progress < 0.70f) {
        val capeAlpha = if (progress < 0.55f) 0.35f else (0.70f - progress) / 0.15f * 0.35f
        val capePath = Path().apply {
            moveTo(cx - w * 0.3f, cy - h * 0.12f)
            cubicTo(cx - w * 0.5f, cy + h * 0.1f, cx - w * 0.3f, cy + h * 0.4f, cx, cy + h * 0.5f)
            cubicTo(cx + w * 0.3f, cy + h * 0.4f, cx + w * 0.3f, cy + h * 0.1f, cx + w * 0.3f, cy - h * 0.12f)
            close()
        }
        drawPath(
            capePath,
            Brush.verticalGradient(
                listOf(
                    neonAccent.copy(alpha = capeAlpha),
                    NeonPurple.copy(alpha = capeAlpha * 0.5f),
                    neonAccent.copy(alpha = capeAlpha * 0.1f)
                )
            )
        )
    }
}

// ════════════════════════════════════════════
// NEON SPEED TRAIL
// ════════════════════════════════════════════

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNeonSpeedTrail(
    posX: Float, posY: Float, alpha: Float, neonColor: Color
) {
    val cx = posX * size.width
    val cy = posY * size.height

    for (i in 0..6) {
        val offsetY = (i - 3f) * size.height * 0.03f
        val startX = cx - size.width * 0.08f
        val endX = cx - size.width * 0.35f - i * size.width * 0.025f
        val trailColor = if (i % 2 == 0) neonColor else NeonPurple
        drawLine(
            trailColor.copy(alpha = alpha * (0.12f + i * 0.05f)),
            Offset(startX, cy + offsetY),
            Offset(endX, cy + offsetY + size.height * 0.02f),
            strokeWidth = 3f - i * 0.3f
        )
    }
}

// ════════════════════════════════════════════
// HILFSFUNKTIONEN
// ════════════════════════════════════════════

private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}

private fun lerpColor(c1: Color, c2: Color, fraction: Float): Color {
    return Color(
        red = c1.red + (c2.red - c1.red) * fraction,
        green = c1.green + (c2.green - c1.green) * fraction,
        blue = c1.blue + (c2.blue - c1.blue) * fraction,
        alpha = c1.alpha + (c2.alpha - c1.alpha) * fraction
    )
}

// ════════════════════════════════════════════
// NEON-FITNESSGERÄTE (Canvas-Zeichnungen)
// Schwebende Silhouetten von Hanteln, Kettlebell,
// Langhantel, Laufband etc. im Neon-Stil
// ════════════════════════════════════════════

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNeonFitnessEquipment(
    glowPulse: Float,
    colorShift: Float
) {
    val w = size.width
    val h = size.height
    val baseAlpha = 0.18f + glowPulse * 0.08f

    // Farbe wechselt sanft: Cyan → Purple → Cyan
    val equipColor1 = lerpColor(NeonCyan, NeonPurple, colorShift)
    val equipColor2 = lerpColor(NeonMagenta, NeonCyan, colorShift)
    val stroke = w * 0.006f

    // ── 1. Hantel oben links ──
    drawDumbbell(
        cx = w * 0.12f, cy = h * 0.10f,
        equipW = w * 0.12f,
        color = equipColor1.copy(alpha = baseAlpha),
        glowColor = equipColor1.copy(alpha = baseAlpha * 0.3f),
        stroke = stroke
    )

    // ── 2. Kettlebell oben rechts ──
    drawKettlebell(
        cx = w * 0.88f, cy = h * 0.13f,
        equipH = w * 0.09f,
        color = equipColor2.copy(alpha = baseAlpha),
        glowColor = equipColor2.copy(alpha = baseAlpha * 0.3f),
        stroke = stroke
    )

    // ── 3. Langhantel (Barbell) links mittig ──
    drawBarbell(
        cx = w * 0.08f, cy = h * 0.45f,
        barW = w * 0.18f,
        color = NeonGreen.copy(alpha = baseAlpha * 0.8f),
        glowColor = NeonGreen.copy(alpha = baseAlpha * 0.25f),
        stroke = stroke
    )

    // ── 4. Hantel rechts unten ──
    drawDumbbell(
        cx = w * 0.90f, cy = h * 0.75f,
        equipW = w * 0.10f,
        color = equipColor2.copy(alpha = baseAlpha * 0.9f),
        glowColor = equipColor2.copy(alpha = baseAlpha * 0.3f),
        stroke = stroke
    )

    // ── 5. Kettlebell unten links ──
    drawKettlebell(
        cx = w * 0.15f, cy = h * 0.82f,
        equipH = w * 0.08f,
        color = NeonYellow.copy(alpha = baseAlpha * 0.7f),
        glowColor = NeonYellow.copy(alpha = baseAlpha * 0.2f),
        stroke = stroke
    )

    // ── 6. Laufband-Silhouette rechts mitte ──
    drawTreadmill(
        cx = w * 0.87f, cy = h * 0.42f,
        equipW = w * 0.14f,
        color = equipColor1.copy(alpha = baseAlpha * 0.7f),
        glowColor = equipColor1.copy(alpha = baseAlpha * 0.2f),
        stroke = stroke
    )

    // ── 7. Kleine Hantel oben mitte (dezent) ──
    drawDumbbell(
        cx = w * 0.50f, cy = h * 0.05f,
        equipW = w * 0.08f,
        color = NeonPurple.copy(alpha = baseAlpha * 0.6f),
        glowColor = NeonPurple.copy(alpha = baseAlpha * 0.15f),
        stroke = stroke * 0.8f
    )

    // ── 8. Kettlebell unten rechts (dezent) ──
    drawKettlebell(
        cx = w * 0.72f, cy = h * 0.92f,
        equipH = w * 0.06f,
        color = NeonCyan.copy(alpha = baseAlpha * 0.5f),
        glowColor = NeonCyan.copy(alpha = baseAlpha * 0.15f),
        stroke = stroke * 0.7f
    )
}

// ── Hantel (Dumbbell) ──
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDumbbell(
    cx: Float, cy: Float, equipW: Float,
    color: Color, glowColor: Color, stroke: Float
) {
    val halfW = equipW / 2
    val plateH = equipW * 0.45f
    val plateW = equipW * 0.18f

    // Glow
    drawCircle(
        brush = Brush.radialGradient(listOf(glowColor, Color.Transparent)),
        radius = equipW * 0.9f, center = Offset(cx, cy)
    )
    // Stange
    drawLine(color, Offset(cx - halfW, cy), Offset(cx + halfW, cy), strokeWidth = stroke)
    // Linke Gewichte
    drawRoundRect(
        color, Offset(cx - halfW - plateW / 2, cy - plateH / 2),
        androidx.compose.ui.geometry.Size(plateW, plateH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke, stroke)
    )
    // Rechte Gewichte
    drawRoundRect(
        color, Offset(cx + halfW - plateW / 2, cy - plateH / 2),
        androidx.compose.ui.geometry.Size(plateW, plateH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke, stroke)
    )
}

// ── Kettlebell ──
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawKettlebell(
    cx: Float, cy: Float, equipH: Float,
    color: Color, glowColor: Color, stroke: Float
) {
    val bodyR = equipH * 0.4f
    val handleR = equipH * 0.3f

    // Glow
    drawCircle(
        brush = Brush.radialGradient(listOf(glowColor, Color.Transparent)),
        radius = equipH * 0.9f, center = Offset(cx, cy)
    )
    // Kugelkörper
    drawCircle(color, bodyR, Offset(cx, cy + equipH * 0.15f), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
    // Griff (Arc oben)
    drawArc(
        color, startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(cx - handleR, cy - equipH * 0.35f),
        size = androidx.compose.ui.geometry.Size(handleR * 2, handleR * 1.4f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
    )
}

// ── Langhantel (Barbell) ──
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBarbell(
    cx: Float, cy: Float, barW: Float,
    color: Color, glowColor: Color, stroke: Float
) {
    val halfW = barW / 2
    val plateH = barW * 0.28f
    val plateW = barW * 0.12f
    val outerPlateW = barW * 0.08f

    // Glow
    drawCircle(
        brush = Brush.radialGradient(listOf(glowColor, Color.Transparent)),
        radius = barW * 0.7f, center = Offset(cx, cy)
    )
    // Hauptstange
    drawLine(color, Offset(cx - halfW, cy), Offset(cx + halfW, cy), strokeWidth = stroke)
    // Innere Gewichte links
    drawRoundRect(
        color, Offset(cx - halfW - plateW / 2, cy - plateH / 2),
        androidx.compose.ui.geometry.Size(plateW, plateH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke, stroke)
    )
    // Äußere Gewichte links
    drawRoundRect(
        color, Offset(cx - halfW - plateW - outerPlateW, cy - plateH * 0.65f / 2),
        androidx.compose.ui.geometry.Size(outerPlateW, plateH * 0.65f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke, stroke)
    )
    // Innere Gewichte rechts
    drawRoundRect(
        color, Offset(cx + halfW - plateW / 2, cy - plateH / 2),
        androidx.compose.ui.geometry.Size(plateW, plateH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke, stroke)
    )
    // Äußere Gewichte rechts
    drawRoundRect(
        color, Offset(cx + halfW + plateW, cy - plateH * 0.65f / 2),
        androidx.compose.ui.geometry.Size(outerPlateW, plateH * 0.65f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke, stroke)
    )
}

// ── Laufband-Silhouette ──
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTreadmill(
    cx: Float, cy: Float, equipW: Float,
    color: Color, glowColor: Color, stroke: Float
) {
    val halfW = equipW / 2
    val halfH = equipW * 0.35f

    // Glow
    drawCircle(
        brush = Brush.radialGradient(listOf(glowColor, Color.Transparent)),
        radius = equipW * 0.8f, center = Offset(cx, cy)
    )
    // Lauffläche (Rechteck)
    drawRoundRect(
        color,
        Offset(cx - halfW, cy - halfH * 0.2f),
        androidx.compose.ui.geometry.Size(equipW, halfH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke * 2, stroke * 2),
        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
    )
    // Griffstange links
    drawLine(color, Offset(cx - halfW * 0.7f, cy - halfH * 0.2f), Offset(cx - halfW * 0.5f, cy - halfH * 1.4f), strokeWidth = stroke)
    // Griffstange rechts
    drawLine(color, Offset(cx + halfW * 0.7f, cy - halfH * 0.2f), Offset(cx + halfW * 0.5f, cy - halfH * 1.4f), strokeWidth = stroke)
    // Quergriff oben
    drawLine(color, Offset(cx - halfW * 0.5f, cy - halfH * 1.4f), Offset(cx + halfW * 0.5f, cy - halfH * 1.4f), strokeWidth = stroke)
    // Display-Box
    drawRoundRect(
        color,
        Offset(cx - halfW * 0.25f, cy - halfH * 1.7f),
        androidx.compose.ui.geometry.Size(halfW * 0.5f, halfH * 0.35f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke, stroke),
        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke * 0.7f)
    )
}
