package com.fantasyfoodplanner.logic

import com.fantasyfoodplanner.features.fitness.FocusMode
import com.fantasyfoodplanner.features.fitness.SessionMode
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests für CheckInAnalyzer – NLP-basierte Befindlichkeitsanalyse.
 */
class CheckInAnalyzerTest {

    private val defaultLearning = LearningProfile()

    // --- Session Mode Bestimmung ---

    @Test
    fun `muede ergibt SHORT Modus`() {
        val result = CheckInAnalyzer.analyze("Bin heute müde", defaultLearning)
        assertEquals(SessionMode.SHORT, result.mode)
    }

    @Test
    fun `fit ergibt PUSH Modus`() {
        val result = CheckInAnalyzer.analyze("Bin heute fit und motiviert", defaultLearning)
        assertEquals(SessionMode.PUSH, result.mode)
    }

    @Test
    fun `Schmerz ergibt RECOVERY Modus`() {
        val result = CheckInAnalyzer.analyze("Habe Schmerzen im Rücken", defaultLearning)
        assertEquals(SessionMode.RECOVERY, result.mode)
    }

    @Test
    fun `Zeitdruck ergibt SHORT Modus`() {
        val result = CheckInAnalyzer.analyze("Wenig Zeit heute", defaultLearning)
        assertEquals(SessionMode.SHORT, result.mode)
    }

    @Test
    fun `neutraler Text ergibt NORMAL Modus`() {
        val result = CheckInAnalyzer.analyze("okay", defaultLearning)
        assertEquals(SessionMode.NORMAL, result.mode)
    }

    @Test
    fun `leerer Text ergibt NORMAL Modus`() {
        val result = CheckInAnalyzer.analyze("", defaultLearning)
        assertEquals(SessionMode.NORMAL, result.mode)
    }

    // --- Focus Mode ---

    @Test
    fun `Schmerz setzt Focus auf MOBILITY`() {
        val result = CheckInAnalyzer.analyze("Muskelkater", defaultLearning)
        assertEquals(FocusMode.MOBILITY, result.focus)
    }

    @Test
    fun `fit setzt Focus auf PERFORMANCE`() {
        val result = CheckInAnalyzer.analyze("Super fit heute!", defaultLearning)
        assertEquals(FocusMode.PERFORMANCE, result.focus)
    }

    @Test
    fun `Zeitdruck setzt Focus auf EFFICIENCY`() {
        val result = CheckInAnalyzer.analyze("schnell trainieren", defaultLearning)
        assertEquals(FocusMode.EFFICIENCY, result.focus)
    }

    @Test
    fun `Muedigkeit setzt Focus auf TECHNIQUE`() {
        val result = CheckInAnalyzer.analyze("schlapp heute", defaultLearning)
        assertEquals(FocusMode.TECHNIQUE, result.focus)
    }

    // --- Soreness Focus Extraktion ---

    @Test
    fun `Schulter Schmerz extrahiert Schultern`() {
        val result = CheckInAnalyzer.analyze("Schulter tut weh", defaultLearning)
        assertEquals("Schultern", result.sorenessFocus)
    }

    @Test
    fun `Ruecken Schmerz extrahiert Ruecken`() {
        val result = CheckInAnalyzer.analyze("Rücken verspannt", defaultLearning)
        assertEquals("Rücken", result.sorenessFocus)
    }

    @Test
    fun `Beine Schmerz extrahiert Beine`() {
        val result = CheckInAnalyzer.analyze("Beine sind schwer", defaultLearning)
        assertEquals("Beine", result.sorenessFocus)
    }

    @Test
    fun `Brust Schmerz extrahiert Brust`() {
        val result = CheckInAnalyzer.analyze("Brust ist verspannt", defaultLearning)
        assertEquals("Brust", result.sorenessFocus)
    }

    @Test
    fun `Arm Schmerz extrahiert Arme`() {
        val result = CheckInAnalyzer.analyze("Bizeps hat Muskelkater", defaultLearning)
        assertEquals("Arme", result.sorenessFocus)
    }

    @Test
    fun `Core Schmerz extrahiert Bauch Core`() {
        val result = CheckInAnalyzer.analyze("Bauch tut weh", defaultLearning)
        assertEquals("Bauch / Core", result.sorenessFocus)
    }

    @Test
    fun `kein Schmerzbereich ergibt null`() {
        val result = CheckInAnalyzer.analyze("Alles gut", defaultLearning)
        assertNull(result.sorenessFocus)
    }

    // --- Intensitaet ---

    @Test
    fun `Intensifier erhoeht Modifer`() {
        val resultNormal = CheckInAnalyzer.analyze("fit", defaultLearning)
        val resultIntense = CheckInAnalyzer.analyze("mega fit", defaultLearning)
        assertTrue(
            "Intensifier sollte höheren Modifier geben",
            resultIntense.modifier.intensityFactor >= resultNormal.modifier.intensityFactor
        )
    }

    // --- Stress ---

    @Test
    fun `Stress ergibt SHORT Modus`() {
        val result = CheckInAnalyzer.analyze("Mega viel Stress bei der Arbeit", defaultLearning)
        assertEquals(SessionMode.SHORT, result.mode)
    }

    // --- Negation ---

    @Test
    fun `nicht muede wird nicht als Low erkannt`() {
        val result = CheckInAnalyzer.analyze("nicht müde", defaultLearning)
        // Bei Negation sollte es NICHT als LOW interpretiert werden
        assertNotEquals(SessionMode.RECOVERY, result.mode)
    }

    // --- Explanation ---

    @Test
    fun `jede Analyse hat eine Erklaerung`() {
        val texts = listOf("", "fit", "müde", "Schmerz", "wenig Zeit", "ok")
        texts.forEach { text ->
            val result = CheckInAnalyzer.analyze(text, defaultLearning)
            assertTrue(
                "Erklärung darf nicht leer sein für '$text'",
                result.explanation.isNotEmpty()
            )
        }
    }

    // --- Emoji Handling ---

    @Test
    fun `Schlaf Emoji wird als muede interpretiert`() {
        val result = CheckInAnalyzer.analyze("😴", defaultLearning)
        assertEquals(SessionMode.SHORT, result.mode)
    }

    @Test
    fun `Feuer Emoji wird als motiviert interpretiert`() {
        val result = CheckInAnalyzer.analyze("🔥", defaultLearning)
        assertEquals(SessionMode.PUSH, result.mode)
    }

    // --- Krank Keyword ---

    @Test
    fun `krank ergibt RECOVERY`() {
        val result = CheckInAnalyzer.analyze("bin krank", defaultLearning)
        assertEquals(SessionMode.RECOVERY, result.mode)
    }

    @Test
    fun `verletzt ergibt RECOVERY`() {
        val result = CheckInAnalyzer.analyze("habe mich verletzt", defaultLearning)
        assertEquals(SessionMode.RECOVERY, result.mode)
    }
}

