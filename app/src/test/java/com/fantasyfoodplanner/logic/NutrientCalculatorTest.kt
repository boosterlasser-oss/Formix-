package com.fantasyfoodplanner.logic

import com.fantasyfoodplanner.data.UserProfile
import com.fantasyfoodplanner.utils.NutrientCalculator
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests für NutrientCalculator – TDEE und Protein-Berechnung.
 */
class NutrientCalculatorTest {

    @Test
    fun `TDEE fuer 75kg Mann moderat aktiv korrekt`() {
        val profile = UserProfile(
            name = "Max", age = 25, weightKg = 75.0, heightCm = 180,
            sex = "male", activityLevel = "moderate", goal = "fit"
        )
        // Mifflin-St Jeor: BMR = 10*75 + 6.25*180 - 5*25 + 5 = 750+1125-125+5 = 1755
        // × 1.55 (moderate) = 2720.25 → gerundet 2720
        val tdee = NutrientCalculator.tdee(profile)
        assertEquals(2720, tdee)
    }

    @Test
    fun `TDEE fuer Frau mit sedentary korrekt`() {
        val profile = UserProfile(
            name = "Anna", age = 30, weightKg = 60.0, heightCm = 165,
            sex = "female", activityLevel = "sedentary", goal = "fit"
        )
        // BMR = 10*60 + 6.25*165 - 5*30 - 161 = 600+1031.25-150-161 = 1320.25
        // × 1.2 = 1584.3 → 1584
        val tdee = NutrientCalculator.tdee(profile)
        assertEquals(1584, tdee)
    }

    @Test
    fun `TDEE Aufbau hat 10 Prozent Ueberschuss`() {
        val profileFit = UserProfile(
            name = "Test", age = 25, weightKg = 75.0, heightCm = 180,
            sex = "male", activityLevel = "moderate", goal = "fit"
        )
        val profileBuild = profileFit.copy(goal = "build")

        val tdeeFit = NutrientCalculator.tdee(profileFit)
        val tdeeBuild = NutrientCalculator.tdee(profileBuild)

        // build = baseTdee * 1.1, fit = baseTdee * 1.0
        assertTrue("Build TDEE ($tdeeBuild) sollte > Fit TDEE ($tdeeFit)", tdeeBuild > tdeeFit)
    }

    @Test
    fun `TDEE Abnehmen hat 20 Prozent Defizit`() {
        val profileFit = UserProfile(
            name = "Test", age = 25, weightKg = 75.0, heightCm = 180,
            sex = "male", activityLevel = "moderate", goal = "fit"
        )
        val profileLose = profileFit.copy(goal = "lose")

        val tdeeFit = NutrientCalculator.tdee(profileFit)
        val tdeeLose = NutrientCalculator.tdee(profileLose)

        assertTrue("Lose TDEE ($tdeeLose) sollte < Fit TDEE ($tdeeFit)", tdeeLose < tdeeFit)
    }

    @Test
    fun `Protein Ziel ist 2g pro kg Koerpergewicht`() {
        val profile = UserProfile(name = "Test", weightKg = 80.0)
        val protein = NutrientCalculator.targetProtein(profile)
        assertEquals(160, protein)
    }

    @Test
    fun `Protein bei 65kg`() {
        val profile = UserProfile(name = "Test", weightKg = 65.0)
        val protein = NutrientCalculator.targetProtein(profile)
        assertEquals(130, protein)
    }
}

