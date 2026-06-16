package com.fantasyfoodplanner.logic

import com.fantasyfoodplanner.data.PerformanceTagEngine
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests für PerformanceTagEngine – Tag-Generierung für Lebensmittel.
 */
class PerformanceTagEngineTest {

    @Test
    fun `hohe Carbs niedrig Fett ergibt pre_workout`() {
        val tags = PerformanceTagEngine.computeTags(
            kcal = 200.0, protein = 5.0, carbs = 40.0, fat = 3.0
        )
        assertTrue("pre_workout Tag erwartet", tags.contains("pre_workout"))
    }

    @Test
    fun `hohes Protein ergibt post_workout`() {
        val tags = PerformanceTagEngine.computeTags(
            kcal = 300.0, protein = 25.0, carbs = 30.0, fat = 5.0
        )
        assertTrue("post_workout Tag erwartet", tags.contains("post_workout"))
    }

    @Test
    fun `sehr hohes Protein ergibt regeneration`() {
        val tags = PerformanceTagEngine.computeTags(
            kcal = 200.0, protein = 20.0, carbs = 10.0, fat = 5.0
        )
        assertTrue("regeneration Tag erwartet", tags.contains("regeneration"))
    }

    @Test
    fun `hoher VitaminC Gehalt ergibt regeneration`() {
        val tags = PerformanceTagEngine.computeTags(
            kcal = 50.0, protein = 1.0, carbs = 10.0, fat = 0.5, vitaminC = 50.0
        )
        assertTrue("regeneration Tag erwartet bei Vitamin C", tags.contains("regeneration"))
    }

    @Test
    fun `hoher Kalium Gehalt ergibt Elektrolyt`() {
        val tags = PerformanceTagEngine.computeTags(
            kcal = 100.0, protein = 2.0, carbs = 25.0, fat = 0.5, potassium = 350.0
        )
        assertTrue("Elektrolyt Tag erwartet", tags.contains("Elektrolyt"))
    }

    @Test
    fun `hohes Protein ergibt Muskelaufbau`() {
        val tags = PerformanceTagEngine.computeTags(
            kcal = 200.0, protein = 15.0, carbs = 10.0, fat = 5.0
        )
        assertTrue("Muskelaufbau Tag erwartet", tags.contains("Muskelaufbau"))
    }

    @Test
    fun `niedrige Kalorien niedrig Fett ergibt Diaetfreundlich`() {
        val tags = PerformanceTagEngine.computeTags(
            kcal = 30.0, protein = 2.0, carbs = 5.0, fat = 0.5
        )
        assertTrue("Diätfreundlich Tag erwartet", tags.contains("Diätfreundlich"))
    }

    @Test
    fun `hohe Kalorien und Fett ergibt nicht Diaetfreundlich`() {
        val tags = PerformanceTagEngine.computeTags(
            kcal = 500.0, protein = 10.0, carbs = 30.0, fat = 35.0
        )
        assertFalse("Diätfreundlich sollte NICHT vergeben werden", tags.contains("Diätfreundlich"))
    }

    @Test
    fun `Tags sind distinct`() {
        val tags = PerformanceTagEngine.computeTags(
            kcal = 300.0, protein = 30.0, carbs = 40.0, fat = 5.0
        )
        assertEquals("Tags sollten keine Duplikate haben", tags.size, tags.distinct().size)
    }

    @Test
    fun `zu viel Fett ergibt kein pre_workout`() {
        val tags = PerformanceTagEngine.computeTags(
            kcal = 500.0, protein = 10.0, carbs = 30.0, fat = 35.0
        )
        assertFalse("pre_workout sollte bei hohem Fett nicht vergeben werden", tags.contains("pre_workout"))
    }
}

