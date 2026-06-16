package com.fantasyfoodplanner.logic

import com.fantasyfoodplanner.data.Recipe
import com.fantasyfoodplanner.data.UserProfile
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests für AiLogic – Ernährungs-Scoring und Tagesplan-Logik.
 */
class AiLogicTest {

    private val defaultProfile = UserProfile(
        name = "Test",
        age = 25,
        weightKg = 75.0,
        heightCm = 180,
        sex = "male",
        activityLevel = "moderate",
        goal = "build",
        dailyKcalTarget = 2500,
        dailyProteinTarget = 150
    )

    // --- calculateRemaining ---

    @Test
    fun `calculateRemaining gibt korrektes Restbudget zurueck`() {
        val result = AiLogic.calculateRemaining(defaultProfile, 1000, 50.0, 100.0, 30.0)
        assertEquals(1500, result.remainingKcal)
        assertEquals(100.0, result.remainingProtein, 0.01)
    }

    @Test
    fun `calculateRemaining gibt 0 bei Ueberkonsum zurueck`() {
        val result = AiLogic.calculateRemaining(defaultProfile, 3000, 200.0, 500.0, 200.0)
        assertEquals(0, result.remainingKcal)
        assertEquals(0.0, result.remainingProtein, 0.01)
    }

    @Test
    fun `calculateRemaining bei keinem Konsum gibt volles Budget`() {
        val result = AiLogic.calculateRemaining(defaultProfile, 0, 0.0, 0.0, 0.0)
        assertEquals(2500, result.remainingKcal)
        assertEquals(150.0, result.remainingProtein, 0.01)
    }

    // --- generateDayPlanWithContext ---

    @Test
    fun `leere Rezeptliste gibt leere Vorschlaege`() {
        val result = AiLogic.generateDayPlanWithContext(
            defaultProfile, emptyList(), 0, 0.0, 0.0, 0.0
        )
        assertTrue(result.suggestions.isEmpty())
    }

    @Test
    fun `Rezepte werden nach Score sortiert`() {
        val recipes = listOf(
            Recipe(name = "Low Score", ingredients = "", kcal = 5000, protein = 0.0, carbs = 0.0, fat = 100.0),
            Recipe(name = "High Score", ingredients = "", kcal = 500, protein = 40.0, carbs = 50.0, fat = 10.0)
        )
        val result = AiLogic.generateDayPlanWithContext(
            defaultProfile, recipes, 0, 0.0, 0.0, 0.0
        )
        assertTrue(result.suggestions.isNotEmpty())
        // Höchster Score soll zuerst kommen
        assertTrue(result.suggestions.first().score >= result.suggestions.last().score)
    }

    @Test
    fun `PRE Timing mit schnellen Carbs ergibt hohen Score`() {
        val recipe = Recipe(
            name = "Bananen-Reis", ingredients = "", kcal = 350,
            protein = 5.0, carbs = 60.0, fat = 3.0
        )
        val result = AiLogic.generateDayPlanWithContext(
            defaultProfile, listOf(recipe), 0, 0.0, 0.0, 0.0,
            timingMode = TimingMode.PRE, timeOffsetMinutes = 30
        )
        val scored = result.suggestions.first()
        assertTrue("PRE: schnelle Carbs sollten >80 scoren, war ${scored.score}", scored.score >= 80)
    }

    @Test
    fun `PRE Timing mit fettem Essen ergibt niedrigen Score`() {
        val recipe = Recipe(
            name = "Fettbombe", ingredients = "", kcal = 800,
            protein = 10.0, carbs = 20.0, fat = 60.0
        )
        val result = AiLogic.generateDayPlanWithContext(
            defaultProfile, listOf(recipe), 0, 0.0, 0.0, 0.0,
            timingMode = TimingMode.PRE, timeOffsetMinutes = 30
        )
        val scored = result.suggestions.first()
        assertTrue("PRE: fettes Essen sollte <30 scoren, war ${scored.score}", scored.score < 30)
    }

    @Test
    fun `POST Timing mit viel Protein fuer Aufbau ergibt hohen Score`() {
        val recipe = Recipe(
            name = "Protein Bowl", ingredients = "", kcal = 600,
            protein = 45.0, carbs = 60.0, fat = 10.0
        )
        val result = AiLogic.generateDayPlanWithContext(
            defaultProfile, listOf(recipe), 0, 0.0, 0.0, 0.0,
            timingMode = TimingMode.POST
        )
        val scored = result.suggestions.first()
        assertTrue("POST + Aufbau mit Protein sollte >85 scoren, war ${scored.score}", scored.score >= 85)
    }

    @Test
    fun `Burger Spezialregel POST + HIGH + AUFBAU ergibt guten Score`() {
        val burger = Recipe(
            name = "Classic Burger", ingredients = "", kcal = 700,
            protein = 35.0, carbs = 50.0, fat = 30.0
        )
        val result = AiLogic.generateDayPlanWithContext(
            defaultProfile, listOf(burger), 0, 0.0, 0.0, 0.0,
            timingMode = TimingMode.POST, trainingIntensity = "HIGH"
        )
        val scored = result.suggestions.first()
        assertTrue("Burger POST+HIGH+AUFBAU sollte >= 85 scoren, war ${scored.score}", scored.score >= 85)
        assertEquals("✅ Optimal", scored.label)
    }

    @Test
    fun `Burger bei Fettabbau bekommt schlechteren Score`() {
        val fatLossProfile = defaultProfile.copy(goal = "lose")
        val burger = Recipe(
            name = "Classic Burger", ingredients = "", kcal = 700,
            protein = 35.0, carbs = 50.0, fat = 30.0
        )
        val result = AiLogic.generateDayPlanWithContext(
            fatLossProfile, listOf(burger), 0, 0.0, 0.0, 0.0,
            timingMode = TimingMode.NEUTRAL
        )
        val scored = result.suggestions.first()
        assertTrue("Burger bei Fettabbau sollte <= 50 scoren, war ${scored.score}", scored.score <= 50)
    }

    @Test
    fun `Tags werden korrekt vergeben`() {
        val highProtein = Recipe(
            name = "Protein Shake", ingredients = "", kcal = 200,
            protein = 40.0, carbs = 10.0, fat = 3.0
        )
        val result = AiLogic.generateDayPlanWithContext(
            defaultProfile, listOf(highProtein), 0, 0.0, 0.0, 0.0
        )
        val scored = result.suggestions.first()
        assertTrue("High Protein Tag erwartet", scored.tags.contains("High Protein"))
        assertTrue("Low Fat Tag erwartet", scored.tags.contains("Low Fat"))
    }

    @Test
    fun `generateDayPlan gibt maximal 3 Rezepte zurueck`() {
        val recipes = (1..10).map {
            Recipe(name = "Rezept $it", ingredients = "", kcal = 300 + it * 50, protein = 20.0, carbs = 30.0, fat = 10.0)
        }
        val result = AiLogic.generateDayPlan(defaultProfile, recipes)
        assertTrue("Max 3 Rezepte erwartet, waren ${result.size}", result.size <= 3)
    }
}

