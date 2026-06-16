package com.fantasyfoodplanner.logic

import com.fantasyfoodplanner.data.ExerciseLog
import com.fantasyfoodplanner.data.ExerciseWithSets
import com.fantasyfoodplanner.data.SetLog
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests für ExerciseLogic – Progressionsberechnung und Limit-Erkennung.
 */
class ExerciseLogicTest {

    // --- calculateScore ---

    @Test
    fun `calculateScore WEIGHTED gibt Gewicht zurueck`() {
        val log = createLog("Bankdrücken", weightKg = 80.0, difficultyLevel = 0)
        val sets = listOf(createSetLog(log.id, timeDone = 0))
        val score = ExerciseLogic.calculateScore(log, sets)
        assertEquals(80.0, score, 0.01)
    }

    @Test
    fun `calculateScore BODYWEIGHT gibt DifficultyLevel zurueck`() {
        val log = createLog("Liegestütze", weightKg = 0.0, difficultyLevel = 3)
        val sets = listOf(createSetLog(log.id, timeDone = 0))
        val score = ExerciseLogic.calculateScore(log, sets)
        assertEquals(3.0, score, 0.01)
    }

    @Test
    fun `calculateScore TIME gibt Gesamtzeit zurueck`() {
        val log = createLog("Plank", weightKg = 0.0, difficultyLevel = 0)
        val sets = listOf(
            createSetLog(log.id, timeDone = 30),
            createSetLog(log.id, timeDone = 45)
        )
        val score = ExerciseLogic.calculateScore(log, sets)
        assertEquals(75.0, score, 0.01)
    }

    // --- calculateNextProgression ---

    @Test
    fun `leere History gibt Defaults fuer WEIGHTED`() {
        val result = ExerciseLogic.calculateNextProgression(emptyList(), "Bankdrücken")
        assertEquals(20.0, result.weightKg, 0.01)
        assertEquals(3, result.plannedSets)
        assertEquals(10, result.repTarget)
        assertFalse(result.isLimitDetected)
    }

    @Test
    fun `leere History gibt Defaults fuer BODYWEIGHT`() {
        val result = ExerciseLogic.calculateNextProgression(emptyList(), "Liegestütze")
        assertEquals(0.0, result.weightKg, 0.01)
        assertEquals(3, result.plannedSets)
        assertEquals(12, result.repTarget)
        assertEquals(1, result.difficultyLevel)
    }

    @Test
    fun `leere History gibt Defaults fuer TIME`() {
        val result = ExerciseLogic.calculateNextProgression(emptyList(), "Plank")
        assertEquals(0.0, result.weightKg, 0.01)
        assertEquals(3, result.plannedSets)
        assertEquals(30, result.durationTargetSeconds)
    }

    @Test
    fun `erfolgreiche Session WEIGHTED steigert Gewicht um 2_5kg`() {
        val history = listOf(
            createExerciseWithSets("Bankdrücken", weightKg = 60.0, successful = true, allSetsSuccess = true)
        )
        val result = ExerciseLogic.calculateNextProgression(history, "Bankdrücken")
        assertEquals(62.5, result.weightKg, 0.01)
    }

    @Test
    fun `fehlgeschlagene Session WEIGHTED haelt Gewicht`() {
        val history = listOf(
            createExerciseWithSets("Bankdrücken", weightKg = 60.0, successful = false, allSetsSuccess = false)
        )
        val result = ExerciseLogic.calculateNextProgression(history, "Bankdrücken")
        assertEquals(60.0, result.weightKg, 0.01)
    }

    @Test
    fun `erfolgreiche Session BODYWEIGHT mit weniger als 5 Sets steigert Sets`() {
        val history = listOf(
            createExerciseWithSets("Liegestütze", sets = 3, diffLevel = 2, successful = true, allSetsSuccess = true)
        )
        val result = ExerciseLogic.calculateNextProgression(history, "Liegestütze")
        assertEquals(4, result.plannedSets)
        assertEquals(2, result.difficultyLevel)
    }

    @Test
    fun `erfolgreiche Session BODYWEIGHT bei 5 Sets steigert Difficulty`() {
        val history = listOf(
            createExerciseWithSets("Liegestütze", sets = 5, diffLevel = 2, successful = true, allSetsSuccess = true)
        )
        val result = ExerciseLogic.calculateNextProgression(history, "Liegestütze")
        assertEquals(5, result.plannedSets)
        assertEquals(3, result.difficultyLevel)
    }

    @Test
    fun `erfolgreiche Session TIME steigert Dauer um 30s`() {
        val history = listOf(
            createExerciseWithSets("Plank", timeTarget = 60, successful = true, allSetsSuccess = true)
        )
        val result = ExerciseLogic.calculateNextProgression(history, "Plank")
        assertEquals(90, result.durationTargetSeconds)
    }

    // --- isStructuralLimitReached ---

    @Test
    fun `weniger als 3 History Eintraege ergibt kein Limit`() {
        val history = listOf(
            createExerciseWithSets("Bankdrücken", successful = false, allSetsSuccess = false),
            createExerciseWithSets("Bankdrücken", successful = false, allSetsSuccess = false)
        )
        assertFalse(ExerciseLogic.isStructuralLimitReached(history))
    }

    @Test
    fun `3 Fails hintereinander ergibt Limit`() {
        val history = listOf(
            createExerciseWithSets("Bankdrücken", successful = false, allSetsSuccess = false),
            createExerciseWithSets("Bankdrücken", successful = false, allSetsSuccess = false),
            createExerciseWithSets("Bankdrücken", successful = false, allSetsSuccess = false)
        )
        assertTrue(ExerciseLogic.isStructuralLimitReached(history))
    }

    @Test
    fun `niedrige Erfolgsquote ueber 5 Sessions ergibt Limit`() {
        // 1 Erfolg, 4 Misserfolge = 20% < 65%
        val history = listOf(
            createExerciseWithSets("Bankdrücken", successful = true, allSetsSuccess = true),
            createExerciseWithSets("Bankdrücken", successful = false, allSetsSuccess = false),
            createExerciseWithSets("Bankdrücken", successful = false, allSetsSuccess = false),
            createExerciseWithSets("Bankdrücken", successful = false, allSetsSuccess = false),
            createExerciseWithSets("Bankdrücken", successful = false, allSetsSuccess = false)
        )
        assertTrue(ExerciseLogic.isStructuralLimitReached(history))
    }

    @Test
    fun `Plateau ueber 5 Sessions ergibt Limit`() {
        // Alle 5 Sessions mit dem gleichen Gewicht
        val history = (1..5).map {
            createExerciseWithSets("Bankdrücken", weightKg = 60.0, successful = true, allSetsSuccess = true)
        }
        assertTrue(ExerciseLogic.isStructuralLimitReached(history))
    }

    @Test
    fun `Limit wird bei Progression erkannt und blockiert Steigerung`() {
        // 3 Fails → Limit aktiv → keine Steigerung
        val history = listOf(
            createExerciseWithSets("Bankdrücken", weightKg = 60.0, successful = false, allSetsSuccess = false),
            createExerciseWithSets("Bankdrücken", weightKg = 60.0, successful = false, allSetsSuccess = false),
            createExerciseWithSets("Bankdrücken", weightKg = 60.0, successful = false, allSetsSuccess = false)
        )
        val result = ExerciseLogic.calculateNextProgression(history, "Bankdrücken")
        assertTrue(result.isLimitDetected)
        assertEquals(60.0, result.weightKg, 0.01) // Kein Anstieg trotz letztem log
    }

    // --- Hilfsfunktionen ---

    private fun createLog(
        name: String,
        weightKg: Double = 0.0,
        difficultyLevel: Int = 0,
        timeTarget: Int = 0,
        sets: Int = 3,
        successful: Boolean = true
    ): ExerciseLog = ExerciseLog(
        dateEpochDay = 19800,
        workoutType = "STRENGTH",
        exerciseName = name,
        exerciseType = ExerciseLogic.getExerciseType(name).name,
        plannedSets = sets,
        actualSetsDone = sets,
        totalRepsDone = sets * 10,
        weightKg = weightKg,
        difficultyLevel = difficultyLevel,
        timeTargetSeconds = timeTarget,
        wasSuccessful = successful
    )

    private fun createSetLog(
        logId: String,
        timeDone: Int = 0,
        success: Boolean = true
    ): SetLog = SetLog(
        exerciseLogId = logId,
        setIndex = 1,
        repsDone = 10,
        timeDoneSeconds = timeDone,
        setSuccess = success
    )

    private fun createExerciseWithSets(
        name: String,
        weightKg: Double = 60.0,
        sets: Int = 3,
        diffLevel: Int = 0,
        timeTarget: Int = 0,
        successful: Boolean = true,
        allSetsSuccess: Boolean = true
    ): ExerciseWithSets {
        val log = createLog(name, weightKg, diffLevel, timeTarget, sets, successful)
        val setLogs = (1..sets).map { idx ->
            SetLog(
                exerciseLogId = log.id,
                setIndex = idx,
                repsDone = 10,
                timeDoneSeconds = if (timeTarget > 0) timeTarget else null,
                setSuccess = allSetsSuccess
            )
        }
        return ExerciseWithSets(log, setLogs)
    }
}

