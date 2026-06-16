package com.fantasyfoodplanner.features

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fantasyfoodplanner.R
import com.fantasyfoodplanner.ui.FantasyColors
import com.fantasyfoodplanner.ui.FText
import kotlinx.coroutines.delay
import kotlin.random.Random

private object SplashSessionState {
    var progress: Float = 0f
    var minTimeReached: Boolean = false
    var splashStartTime: Long = 0L
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun SplashScreenContent(
    initializationFinished: Boolean = true,
    onFinished: () -> Unit = {}
) {
    // FIX: Aktuelle Werte tracken, damit LaunchedEffect sie "frisch" sieht
    val currentInitFinished by rememberUpdatedState(initializationFinished)
    val currentOnFinished by rememberUpdatedState(onFinished)

    var progress by rememberSaveable { mutableStateOf(SplashSessionState.progress) }
    val minSplashTime = 3000L  // SplashScreen jetzt 3 Sekunden Mindestzeit
    var minTimeReached by rememberSaveable { mutableStateOf(SplashSessionState.minTimeReached) }
    var splashStartTime by rememberSaveable { mutableStateOf(SplashSessionState.splashStartTime) }

    SideEffect {
        SplashSessionState.progress = progress
        SplashSessionState.minTimeReached = minTimeReached
        SplashSessionState.splashStartTime = splashStartTime
    }

    LaunchedEffect(Unit) {
        if (splashStartTime == 0L) {
            splashStartTime = System.currentTimeMillis()
        }
        val startTime = splashStartTime
        val tickInterval = 30L // Alle 30ms updaten = flüssige Animation

        // Gleichmäßig über die gesamte Mindestzeit verteilen
        while (progress < 1.0f) {
            delay(tickInterval)
            val elapsed = System.currentTimeMillis() - startTime
            val timeProgress = (elapsed.toFloat() / minSplashTime).coerceIn(0f, 1f)

            if (elapsed >= minSplashTime) {
                minTimeReached = true
            }

            // Leichter natürlicher Varianz-Effekt: Fortschritt folgt der Zeit mit Mini-Schwankung
            val targetProgress = timeProgress * 0.95f // Max 95% über Zeitfortschritt
            val jitter = Random.nextFloat() * 0.005f // Winzige Schwankung für natürlichen Look
            progress = (targetProgress + jitter).coerceIn(progress, 0.95f) // Nie rückwärts

            // Ab 95%: Nur weiter wenn Initialisierung fertig
            if (timeProgress >= 1.0f && currentInitFinished) {
                // Letzte 5% flüssig auffüllen
                while (progress < 1.0f) {
                    delay(tickInterval)
                    progress = (progress + 0.02f).coerceAtMost(1.0f)
                }
                break
            }

            // Falls Zeit abgelaufen aber Init noch nicht fertig: bei 95% warten
            if (timeProgress >= 1.0f && !currentInitFinished) {
                progress = 0.95f
                // Sanftes Pulsieren bei 95% statt stillstehen
                while (!currentInitFinished) {
                    delay(300)
                }
                // Init fertig → schnell auf 100%
                while (progress < 1.0f) {
                    delay(tickInterval)
                    progress = (progress + 0.02f).coerceAtMost(1.0f)
                }
                break
            }
        }

        // 100% kurz anzeigen, dann weiter zum Dashboard
        if (progress >= 1.0f && minTimeReached && currentInitFinished) {
            delay(400) // 100% für 0,4s sichtbar
            currentOnFinished()
        }
    }

    val currentProgressPercentage = (progress * 100).toInt()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // HINTERGRUND = REINES VOLLbild (60% OPACITY)
        Image(
            painter = painterResource(id = R.drawable.mein_koerperbild),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.60f,
            modifier = Modifier.fillMaxSize()
        )

        // Content Layer
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            FText(
                text = "Sport ist kein Mord.",
                sizeSp = 32,
                bold = true,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )
            FText(
                text = "... sondern deine stärkste Entscheidung.",
                sizeSp = 18,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )
        }

        // LADEBALKEN
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.8f)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF7C4DFF),       // Neon Purple
                                        Color(0xFF00F5FF),       // Neon Cyan
                                        Color(0xFF39FF14)        // Neon Green
                                    )
                                ),
                                shape = RoundedCornerShape(50)
                            )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                FText(
                    text = "$currentProgressPercentage%",
                    sizeSp = 16,
                    bold = true,
                    color = Color.White,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(45.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            FText(
                text = "FORMIX LÄDT...",
                sizeSp = 12,
                color = Color(0xFF00F5FF).copy(alpha = 0.8f),
                bold = true,
                textAlign = TextAlign.Center
            )
        }
    }
}
