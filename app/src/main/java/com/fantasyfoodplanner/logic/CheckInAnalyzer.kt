package com.fantasyfoodplanner.logic

import com.fantasyfoodplanner.features.fitness.DailyModifier
import com.fantasyfoodplanner.features.fitness.FocusMode
import com.fantasyfoodplanner.features.fitness.SessionMode
import java.util.Locale

/**
 * Analysiert den Check-In Text und bestimmt den optimalen Trainingsmodus.
 *
 * Eingabe: Freitext + LearningProfile
 * Ausgabe: SessionMode, FocusMode, DailyModifier, SorenessFocus
 */
object CheckInAnalyzer {

    data class AnalysisResult(
        val mode: SessionMode,
        val focus: FocusMode,
        val modifier: DailyModifier,
        val sorenessFocus: String? = null,
        val explanation: String = "" // NEU: Erklärung für den User
    )

    // Keyword-Listen (erweitert für bessere Erkennung)
    private val LOW_KEYWORDS = listOf(
        // Müdigkeit
        "schlecht", "muede", "kaputt", "platt", "ausgelaugt", "leer",
        "schlapp", "erschoepft", "k.o.", "groggy", "kaum geschlafen",
        "wenig schlaf", "nicht geschlafen", "hundemuede", "fertig",
        "keine kraft", "antriebslos", "traege", "matt", "abgeschlagen",
        // Englisch
        "tired", "exhausted", "no energy"
    )

    private val HIGH_KEYWORDS = listOf(
        // Motivation
        "fit", "motiviert", "energie", "bereit", "vollgas", "bock",
        "stark", "gut", "topfit", "ready", "baller", "power",
        "ausgeruht", "erholt", "frisch", "wach", "top", "super",
        "mega gut", "richtig gut", "bestens", "optimal", "perfekt",
        "hyped", "pumped", "lets go", "los gehts", "am start",
        // Englisch
        "great", "awesome", "strong", "energized"
    )

    private val TIME_PRESSURE_KEYWORDS = listOf(
        "wenig zeit", "schnell", "kurz", "zeitdruck", "keine zeit",
        "muss los", "termin", "fix", "knapp", "eilig", "gleich weg",
        "nur kurz", "30 min", "halbe stunde", "20 min", "15 min",
        "bisschen zeit", "nicht viel zeit"
    )

    private val PAIN_KEYWORDS = listOf(
        "schmerz", "krank", "muskelkater", "zwickt", "verspannt", "weh", "steif",
        "verletzt", "zerrung", "gezerrt", "blockiert", "eingeklemmt",
        "tut weh", "schmerzen", "gestochen", "sticht", "zieht"
    )

    private val STRESS_KEYWORDS = listOf(
        "stress", "kopf", "arbeit", "druck", "genervt", "ueberfordert",
        "gestresst", "anstrengend", "viel zu tun", "hektisch", "chaotisch",
        "nervig", "angespannt", "unruhig", "sorgen", "probleme"
    )

    private val INTENSIFIERS = listOf("sehr", "mega", "richtig", "extrem", "total", "komplett", "brutal")
    private val DAMPENERS = listOf("bisschen", "leicht", "etwas")

    /**
     * Analysiert den Check-In Text und gibt ein AnalysisResult zurück.
     */
    fun analyze(text: String, learning: LearningProfile): AnalysisResult {
        return analyze(text, learning, recentActivityFocusKey = null, activityCategory = null)
    }

    /**
     * Erweiterte Analyse mit Regenerations-Bewusstsein (rückwärtskompatibel).
     */
    fun analyze(text: String, learning: LearningProfile, recentActivityFocusKey: String?): AnalysisResult {
        return analyze(text, learning, recentActivityFocusKey, activityCategory = null)
    }

    /**
     * Erweiterte Analyse mit Regenerations-Bewusstsein und Kategorie-Abstufung.
     * @param recentActivityFocusKey Der ActivityFocus.dbKey der gestrigen Aktivität (z.B. "LEGS", "ENDURANCE").
     * @param activityCategory Die weightKg-Kodierung der Aktivitäts-Kategorie:
     *   1.0 = FULL_WORKOUT (volle Belastung → Regeneration aktiv),
     *   2.0 = SUPPLEMENTARY (mittlere Belastung → leichte Regeneration),
     *   3.0 = LIGHT_MOVEMENT (leichte Bewegung → keine Regeneration).
     *   Wenn null → Verhalten wie FULL_WORKOUT (rückwärtskompatibel).
     */
    fun analyze(text: String, learning: LearningProfile, recentActivityFocusKey: String?, activityCategory: Double?): AnalysisResult {
        val normalized = normalizeText(text)

        // Negationsprüfung
        val hasNegation = normalized.contains("nicht") ||
                         normalized.contains("kein") ||
                         normalized.contains("null") ||
                         normalized.contains("ohne")

        // Intensität
        val intensityMulti = calculateIntensityMultiplier(normalized)

        // Keyword-Matching
        val isLow = LOW_KEYWORDS.any { normalized.contains(it) } && !hasNegation
        val isHigh = HIGH_KEYWORDS.any { normalized.contains(it) } && !hasNegation
        val isTimePressed = TIME_PRESSURE_KEYWORDS.any { normalized.contains(it) }
        val isIllOrPain = PAIN_KEYWORDS.any { normalized.contains(it) } && !hasNegation
        val isStressed = STRESS_KEYWORDS.any { normalized.contains(it) }

        // Soreness Focus: zuerst aus Text, dann aus gestriger Aktivität ableiten
        // Aber nur wenn die Aktivität intensiv genug war (nicht LIGHT_MOVEMENT)
        val textSoreness = extractSorenessFocus(normalized)
        val cat = activityCategory ?: 1.0 // Rückwärtskompatibel: kein Wert = FULL_WORKOUT
        val activitySoreness = if (cat <= 2.0) {
            // FULL_WORKOUT (1.0) oder SUPPLEMENTARY (2.0) → Regeneration ableiten
            mapActivityFocusToSoreness(recentActivityFocusKey)
        } else {
            // LIGHT_MOVEMENT (3.0) → zu leicht für Regeneration
            null
        }
        val sorenessFocus = textSoreness ?: activitySoreness

        // Erklärung ergänzen wenn Regeneration aus Aktivität abgeleitet
        val activityHint = if (textSoreness == null && sorenessFocus != null && recentActivityFocusKey != null) {
            val intensityLabel = when {
                cat <= 1.0 -> "intensiv"
                else -> "leicht"
            }
            " Regeneration: Gestrige Aktivität ($intensityLabel) belastete $sorenessFocus — weniger Fokus auf diesen Bereich."
        } else ""

        // Modus-Bestimmung
        val result = when {
            isIllOrPain -> createRecoveryResult(intensityMulti, sorenessFocus)
            isTimePressed -> createShortResult(sorenessFocus)
            isLow || isStressed -> createLowEnergyResult(learning, intensityMulti, sorenessFocus)
            isHigh -> createPushResult(intensityMulti, sorenessFocus)
            else -> createNormalResult(sorenessFocus)
        }

        return if (activityHint.isNotEmpty()) {
            result.copy(explanation = result.explanation + activityHint)
        } else result
    }

    private fun normalizeText(text: String): String {
        return text.lowercase(Locale.ROOT)
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("😴", "muede")
            .replace("🔥", "motiviert")
            .replace("⚡", "energie")
    }

    private fun calculateIntensityMultiplier(text: String): Float {
        return when {
            INTENSIFIERS.any { text.contains(it) } -> 1.2f
            DAMPENERS.any { text.contains(it) } -> 0.8f
            else -> 1.0f
        }
    }

    private fun extractSorenessFocus(text: String): String? {
        return when {
            text.contains("schulter") || text.contains("shoulder") -> "Schultern"
            text.contains("ruecken") || text.contains("back") || text.contains("lat") -> "Rücken"
            text.contains("bein") || text.contains("leg") || text.contains("wade") || text.contains("oberschenkel") -> "Beine"
            text.contains("brust") || text.contains("chest") -> "Brust"
            text.contains("arm") || text.contains("bizeps") || text.contains("trizeps") -> "Arme"
            text.contains("bauch") || text.contains("core") || text.contains("abs") -> "Bauch / Core"
            text.contains("nacken") || text.contains("neck") || text.contains("hals") -> "Nacken"
            text.contains("po") || text.contains("gesaess") || text.contains("glute") || text.contains("hintern") -> "Po / Gesäß"
            else -> null
        }
    }

    /**
     * Mappt einen ActivityFocus.dbKey auf einen Soreness-Bereich.
     * Wird genutzt wenn der User gestern eine Aktivität gemacht hat,
     * damit der PlanGenerator die betroffene Muskelgruppe weniger belastet.
     *
     * @param focusKey Der ActivityFocus.dbKey (z.B. "LEGS", "ENDURANCE", "UPPER_BODY")
     * @return Körperbereich als String oder null wenn kein Mapping möglich
     */
    private fun mapActivityFocusToSoreness(focusKey: String?): String? {
        if (focusKey == null) return null
        return when (focusKey.uppercase()) {
            "LEGS" -> "Beine"
            "UPPER_BODY" -> "Oberkörper"
            "ARMS_SHOULDERS" -> "Arme"
            "STRENGTH" -> "Rücken"          // Kraft-Aktivitäten belasten oft den Rücken
            "ENDURANCE" -> "Beine"           // Ausdauer belastet primär die Beine
            "COORDINATION" -> null           // Koordination = keine spezifische Belastung
            "FLEXIBILITY" -> null            // Beweglichkeit = keine spezifische Belastung
            "FULL_BODY" -> "Ganzkörper"
            "RECREATIONAL" -> null           // Freizeitaktivität = zu unspezifisch
            else -> null
        }
    }

    private fun createRecoveryResult(intensityMulti: Float, sorenessFocus: String?): AnalysisResult {
        val factor = (0.7f * intensityMulti).coerceAtMost(0.85f)
        return AnalysisResult(
            mode = SessionMode.RECOVERY,
            focus = FocusMode.MOBILITY,
            modifier = DailyModifier(factor, factor, factor, 30),
            sorenessFocus = sorenessFocus,
            explanation = "🛡️ Recovery-Modus aktiv wegen Schmerzen/Krankheit. Weniger Belastung, mehr Mobilität."
        )
    }

    private fun createShortResult(sorenessFocus: String?): AnalysisResult {
        return AnalysisResult(
            mode = SessionMode.SHORT,
            focus = FocusMode.EFFICIENCY,
            modifier = DailyModifier(0.9f, 0.9f, 0.9f, 60),
            sorenessFocus = sorenessFocus,
            explanation = "⏱️ Short-Modus aktiv wegen Zeitdruck. Kompaktes, effizientes Training."
        )
    }

    private fun createLowEnergyResult(learning: LearningProfile, intensityMulti: Float, sorenessFocus: String?): AnalysisResult {
        var factor = (1.0f - (0.15f * learning.fatigueSensitivity)) * intensityMulti
        factor = factor.coerceIn(0.75f, 1.1f)
        return AnalysisResult(
            mode = SessionMode.SHORT,
            focus = FocusMode.TECHNIQUE,
            modifier = DailyModifier(factor, factor, factor, 40),
            sorenessFocus = sorenessFocus,
            explanation = "💤 Short-Modus aktiv wegen niedriger Energie/Stress. Fokus auf Technik."
        )
    }

    private fun createPushResult(intensityMulti: Float, sorenessFocus: String?): AnalysisResult {
        val factor = (1.15f * intensityMulti).coerceIn(1.05f, 1.3f)
        return AnalysisResult(
            mode = SessionMode.PUSH,
            focus = FocusMode.PERFORMANCE,
            modifier = DailyModifier(factor, factor, factor, 90),
            sorenessFocus = sorenessFocus,
            explanation = "⚡ Push-Modus aktiv! Du bist fit und motiviert. Volle Power!"
        )
    }

    private fun createNormalResult(sorenessFocus: String?): AnalysisResult {
        return AnalysisResult(
            mode = SessionMode.NORMAL,
            focus = FocusMode.NORMAL,
            modifier = DailyModifier(1.0f, 1.0f, 1.0f, 70),
            sorenessFocus = sorenessFocus,
            explanation = "✅ Normal-Modus. Ausgewogenes Training."
        )
    }
}



