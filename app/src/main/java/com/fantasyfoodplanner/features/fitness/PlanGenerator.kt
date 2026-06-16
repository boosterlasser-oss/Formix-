package com.fantasyfoodplanner.features.fitness

import com.fantasyfoodplanner.logic.ExerciseType
import com.fantasyfoodplanner.logic.TrainingType
import kotlin.math.roundToInt
import kotlin.random.Random

object PlanGenerator {

    private data class WarmUpExercise(
        val name: String,
        val category: String,
        val isHomeFriendly: Boolean = true,
        val weight: Int = 1
    )

    private val WARMUP_POOL = listOf(
        // CARDIO
        WarmUpExercise("Locker laufen", "CARDIO", false),
        WarmUpExercise("Seilspringen", "CARDIO", true, 3), 
        WarmUpExercise("Crosstrainer", "CARDIO", false, 2),
        WarmUpExercise("Jumping Jacks", "CARDIO", true),
        WarmUpExercise("High Knees", "CARDIO", true),
        WarmUpExercise("Fahrradergometer", "CARDIO", false, 2),
        WarmUpExercise("Bergsteiger langsam", "CARDIO", true),
        WarmUpExercise("Stepper", "CARDIO", false),
        WarmUpExercise("Burpees locker", "CARDIO", true),

        // MOBILITY
        WarmUpExercise("Schulterkreisen", "MOBILITY"),
        WarmUpExercise("Armkreisen", "MOBILITY"),
        WarmUpExercise("Hüftkreisen", "MOBILITY"),
        WarmUpExercise("Rumpfrotation", "MOBILITY"),
        WarmUpExercise("Katzen-Kuh", "MOBILITY"),
        WarmUpExercise("Sprunggelenk mobilisieren", "MOBILITY"),
        WarmUpExercise("Brustwirbelsäule rotieren", "MOBILITY"),
        WarmUpExercise("Becken kippen", "MOBILITY"),

        // ACTIVATION
        WarmUpExercise("Glute Bridges", "ACTIVATION"),
        WarmUpExercise("Plank leicht", "ACTIVATION"),
        WarmUpExercise("Dead Bug", "ACTIVATION"),
        WarmUpExercise("Mini Squats", "ACTIVATION"),
        WarmUpExercise("Wandstütz", "ACTIVATION"),
        WarmUpExercise("Leichte Kniebeugen", "ACTIVATION"),

        // DYNAMIC_STRETCH
        WarmUpExercise("Beinpendeln", "DYNAMIC_STRETCH"),
        WarmUpExercise("Ausfallschritte vor/zurück", "DYNAMIC_STRETCH"),
        WarmUpExercise("Arm-Swings", "DYNAMIC_STRETCH"),
        WarmUpExercise("Hüftöffner", "DYNAMIC_STRETCH"),
        WarmUpExercise("Oberkörper Side Bends", "DYNAMIC_STRETCH")
    )

    val COOLDOWN_LIB = listOf("Statisches Dehnen", "Tiefe Atmung", "Entspannung")

    fun buildPlan(
        profile: FitnessProfile, 
        type: TrainingType, 
        modifier: DailyModifier = DailyModifier(),
        dailyFocus: String? = null,
        sorenessFocus: String? = null
    ): WorkoutPlan {
        val targetMin = 45
        val s = setsPlan(profile, modifier)
        
        val baseRest = when (profile.experience) {
            "new" -> 90
            "some" -> 75
            else -> 60
        }
        val restSec = (baseRest / modifier.intensityFactor).roundToInt().coerceIn(30, 180)
        
        val planBlocks = mutableListOf<WorkoutBlock>()
        
        // --- DYNAMISCHES AUFWÄRMEN ---
        val warmupItems = generateWarmup(type)
        planBlocks.add(WorkoutBlock(
            type = "warmup",
            title = "Aufwärmen",
            items = warmupItems,
            minutes = if (type == TrainingType.CROSSFIT) 12 else 8
        ))

        var exerciseSpecs = when(type) {
            TrainingType.CROSSFIT -> listOf(
                ExSpec("Explosiv", "cross", listOf("Burpees", "Thruster", "Box Jumps"), s.main, repRange(profile, 15, modifier)),
                ExSpec("Beine", "legs", exOptions("legs", "minimal"), s.main, repRange(profile, 12, modifier)),
                ExSpec("Ziehen", "pull", exOptions("pull", "minimal"), s.main, repRange(profile, 12, modifier)),
                ExSpec("Drücken", "push", exOptions("push", "minimal"), s.main, repRange(profile, 12, modifier)),
                ExSpec("Core", "core", listOf("Plank", "Russian Twist"), s.core, repRange(profile, 15, modifier))
            )
            TrainingType.STRENGTH -> listOf(
                ExSpec("Beine", "legs", listOf("Kniebeugen LH", "Beinpresse", "Kreuzheben"), s.main, repRange(profile, 8, modifier)),
                ExSpec("Drücken", "push", listOf("Bankdrücken", "Schulterpresse", "Dips"), s.main, repRange(profile, 8, modifier)),
                ExSpec("Ziehen", "pull", listOf("Latzug", "Klimmzüge", "Bizeps Curls"), s.main, repRange(profile, 8, modifier)),
                ExSpec("Beine Zusatz", "legs", listOf("Beinstrecker", "Beinbeuger"), s.extra, repRange(profile, 10, modifier)),
                ExSpec("Oberkörper Zusatz", "push", listOf("Butterfly", "Seitheben", "Trizepsdrücken"), s.extra, repRange(profile, 10, modifier)),
                ExSpec("Core", "core", listOf("Beinheben", "Ab-Wheel"), s.core, repRange(profile, 12, modifier))
            )
            TrainingType.BASICS -> listOf(
                ExSpec("Grundlage Beine", "legs", listOf("Kniebeugen", "Ausfallschritte"), s.main, repRange(profile, 12, modifier)),
                ExSpec("Grundlage Drücken", "push", listOf("Liegestütze", "Schulterpresse KH"), s.main, repRange(profile, 12, modifier)),
                ExSpec("Grundlage Ziehen", "pull", listOf("Latzug", "Superman"), s.main, repRange(profile, 12, modifier)),
                ExSpec("Core", "core", listOf("Plank", "Sit-ups"), s.core, repRange(profile, 15, modifier))
            )
            TrainingType.HOME -> listOf(
                ExSpec("Beine", "legs", listOf("Bulgarian Split Squats", "Ausfallschritte"), s.main, repRange(profile, 12, modifier)),
                ExSpec("Push", "push", listOf("Liegestütze", "Pike Pushups"), s.main, repRange(profile, 12, modifier)),
                ExSpec("Pull", "pull", listOf("Superman", "Handtuch-Latzug"), s.main, repRange(profile, 12, modifier)),
                ExSpec("Core", "core", listOf("Plank", "Mountain Climbers"), s.core, repRange(profile, 15, modifier))
            )
            TrainingType.OTHER_ACTIVITY -> emptyList() // Andere Aktivitäten nutzen nicht den PlanGenerator
        }

        // --- INTELLIGENTE FOKUS-ANPASSUNG (Additiv) ---
        if (dailyFocus != null || sorenessFocus != null) {
            val focusCat = mapFocusToCat(dailyFocus)
            val soreCat = mapFocusToCat(sorenessFocus)
            
            // 1. Sortierung: Fokusierte Specs nach vorne, schmerzende nach hinten
            exerciseSpecs = exerciseSpecs.sortedWith { a, b ->
                when {
                    a.cat == focusCat && b.cat != focusCat -> -1
                    a.cat != focusCat && b.cat == focusCat -> 1
                    a.cat == soreCat && b.cat != soreCat -> 1
                    a.cat != soreCat && b.cat == soreCat -> -1
                    else -> 0
                }
            }
            
            // 2. Volumen-Anpassung: Fokus +1 Satz (max 6), Soreness -1 Satz (min 1)
            exerciseSpecs = exerciseSpecs.map { spec ->
                var finalSets = spec.sets
                if (spec.cat == focusCat) finalSets = (finalSets + 1).coerceAtMost(6)
                if (spec.cat == soreCat) finalSets = (finalSets - 1).coerceAtLeast(1)
                spec.copy(sets = finalSets)
            }
        }

        exerciseSpecs.forEach { spec ->
            val name = spec.opts.shuffled().first()
            val def = ExerciseDefinitions.get(name)
            planBlocks.add(WorkoutBlock(
                type = "ex",
                ex = name,
                title = def.desc,
                sets = spec.sets,
                reps = spec.reps,
                weight = if (def.type == ExerciseType.WEIGHTED) {
                    val baseWeight = 20.0 * modifier.intensityFactor
                    // Goal-Anpassung: Abnehmen → leichteres Gewicht
                    when (profile.mainGoal) {
                        "lose" -> (baseWeight * 0.75).coerceAtLeast(5.0)
                        "build" -> baseWeight
                        else -> baseWeight * 0.9
                    }
                } else 0.0,
                durationSeconds = if (def.type == ExerciseType.TIME) (spec.reps * 3 * modifier.timeFactor).roundToInt() else 0
            ))
        }

        planBlocks.add(WorkoutBlock(type = "cooldown", title = "Abkühlen", items = COOLDOWN_LIB.shuffled().take(3), minutes = 5))

        return WorkoutPlan(targetMinutes = targetMin, estTotalMinutes = targetMin, restSeconds = restSec, exercises = planBlocks)
    }

    private fun mapFocusToCat(focus: String?): String? {
        return when (focus) {
            "Brust", "Schultern", "Trizeps", "Arme" -> "push"
            "Rücken", "Bizeps" -> "pull"
            "Beine", "Po/Gesäß" -> "legs"
            "Bauch/Core" -> "core"
            else -> null
        }
    }

    fun generateWarmup(type: TrainingType): List<String> {
        val result = mutableListOf<String>()
        val isHome = type == TrainingType.HOME

        // 1. CARDIO (Immer 1)
        val cardioPool = WARMUP_POOL.filter { it.category == "CARDIO" && (!isHome || it.isHomeFriendly) }
        val weightedCardio = mutableListOf<WarmUpExercise>()
        cardioPool.forEach { ex -> repeat(ex.weight) { weightedCardio.add(ex) } }
        
        val cardio = weightedCardio.shuffled().first()
        val cardioTime = if (cardio.name == "Seilspringen") "3-5 Min" else "5-8 Min"
        result.add("$cardioTime ${cardio.name}")

        // 2. MOBILITY (Immer 1)
        result.add(WARMUP_POOL.filter { it.category == "MOBILITY" }.shuffled().first().name)

        // 3. ACTIVATION (Häufiger bei Kraft/CrossFit)
        val activationChance = if (type == TrainingType.STRENGTH || type == TrainingType.CROSSFIT) 0.8 else 0.4
        if (Random.nextDouble() < activationChance) {
            result.add(WARMUP_POOL.filter { it.category == "ACTIVATION" }.shuffled().first().name)
        }

        // 4. DYNAMIC STRETCH (Häufiger bei CrossFit)
        val stretchChance = if (type == TrainingType.CROSSFIT) 0.8 else 0.3
        if (Random.nextDouble() < stretchChance) {
            result.add(WARMUP_POOL.filter { it.category == "DYNAMIC_STRETCH" }.shuffled().first().name)
        }

        return result
    }

    private fun repRange(p: FitnessProfile, base: Int, mod: DailyModifier): Int {
        val expFactor = when(p.experience) {
            "new" -> 1.2f
            "pro" -> 0.8f
            else -> 1.0f
        }
        // Goal-Anpassung: Abnehmen → mehr Reps, Aufbau → weniger Reps
        val goalFactor = when(p.mainGoal) {
            "lose" -> 1.2f   // Höhere Reps → mehr Kalorienverbrauch
            "build" -> 0.85f // Niedrigere Reps → Hypertrophie/Kraft
            else -> 1.0f     // "fit" → Standard
        }
        return (base * expFactor * goalFactor * mod.volumeFactor).roundToInt().coerceAtLeast(1)
    }

    private fun setsPlan(p: FitnessProfile, mod: DailyModifier): Sets {
        val base = when (p.experience) { "new" -> 2; "some" -> 3; else -> 4 }
        // Goal-Anpassung: Aufbau → mehr Sätze, Abnehmen → Standard
        val goalBonus = if (p.mainGoal == "build" && p.experience != "new") 1 else 0
        val main = ((base + goalBonus) * mod.volumeFactor).roundToInt().coerceIn(1, 6)
        return Sets(main, if (main > 1) main - 1 else 1, if (main > 1) main - 1 else 1)
    }

    private data class Sets(val main: Int, val extra: Int, val core: Int)
    private data class ExSpec(val title: String, val cat: String, val opts: List<String>, val sets: Int, val reps: Int)

    private fun exOptions(cat: String, eq: String): List<String> {
        return when (cat) {
            "pull" -> if (eq == "gym") listOf("Latzug", "Klimmzüge", "Superman") else listOf("Klimmzüge", "Handtuch-Latzug", "Superman")
            "push" -> if (eq == "gym") listOf("Bankdrücken", "Schulterpresse", "Dips") else listOf("Liegestütze", "Dips am Stuhl")
            "legs" -> if (eq == "gym") listOf("Beinpresse", "Kniebeugen LH", "Ausfallschritte KH") else listOf("Kniebeugen", "Ausfallschritte", "Glute Bridges")
            else -> listOf("Plank", "Sit-ups")
        }
    }

    // === NEUE FILTER-FUNKTIONEN (V3.2.0 - Personalisierungs-Update) ===

    /**
     * Filtert Übungen basierend auf verfügbarem Equipment.
     * @param plan Original-Plan
     * @param equipment "full" = Gym, "minimal" = Hanteln/Bänder, "bodyweight" = nur Körpergewicht
     * @return Gefilterter Plan mit passenden Übungen
     */
    fun filterByEquipment(plan: WorkoutPlan, equipment: String): WorkoutPlan {
        val filteredBlocks = plan.exercises.map { block ->
            if (block.type == "ex" && block.ex != null) {
                val def = ExerciseDefinitions.get(block.ex)
                // Check if exercise equipment matches user's available equipment
                val isCompatible = when (equipment) {
                    "bodyweight" -> def.equipment == "bodyweight"
                    "minimal" -> def.equipment == "bodyweight" || def.equipment == "minimal"
                    "full" -> true // All exercises available
                    else -> true
                }
                if (!isCompatible) {
                    // Replace with compatible alternative from same category
                    val alternativeEx = findAlternativeExercise(block.ex, equipment, def.category)
                    block.copy(ex = alternativeEx, title = ExerciseDefinitions.get(alternativeEx).desc)
                } else {
                    block
                }
            } else {
                block
            }
        }
        return plan.copy(exercises = filteredBlocks)
    }

    /**
     * Filtert Übungen basierend auf gesundheitlichen Einschränkungen.
     * @param plan Original-Plan
     * @param restrictions Komma-getrennte Liste: "knie,ruecken,schulter,herz,keine"
     * @return Plan ohne problematische Übungen
     */
    fun filterByHealthRestrictions(plan: WorkoutPlan, restrictions: String): WorkoutPlan {
        if (restrictions.isBlank() || restrictions.contains("keine")) return plan
        
        val restrictionList = restrictions.split(",").map { it.trim().lowercase() }
        val problematicExercises = mutableSetOf<String>()
        
        // Map restrictions to problematic exercises
        if ("knie" in restrictionList) {
            problematicExercises.addAll(listOf("kniebeuge", "squat", "beinpresse", "leg-?press", "ausfallschritte", "lunges", "box jump"))
        }
        if ("ruecken" in restrictionList || "rücken" in restrictionList) {
            problematicExercises.addAll(listOf("kreuzheben", "deadlift", "hyperextension"))
        }
        if ("schulter" in restrictionList) {
            problematicExercises.addAll(listOf("schulterpresse", "shoulder press", "ohp", "seitheben", "lateral-?raise", "pike pushup"))
        }
        if ("herz" in restrictionList) {
            problematicExercises.addAll(listOf("burpees", "thruster", "box jump", "skaters"))
        }
        
        val filteredBlocks = plan.exercises.map { block ->
            if (block.type == "ex" && block.ex != null) {
                val exLower = block.ex.lowercase()
                val isProblematic = problematicExercises.any { pattern -> 
                    Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(exLower)
                }
                if (isProblematic) {
                    val def = ExerciseDefinitions.get(block.ex)
                    val safeAlternative = findSafeAlternative(block.ex, def.category, restrictionList)
                    block.copy(ex = safeAlternative, title = ExerciseDefinitions.get(safeAlternative).desc)
                } else {
                    block
                }
            } else {
                block
            }
        }
        return plan.copy(exercises = filteredBlocks)
    }

    /**
     * Passt den Plan an die verfügbare Trainingszeit an.
     * @param plan Original-Plan
     * @param targetMinutes 30, 45, 60, oder 90 Minuten
     * @return Plan mit angepasster Übungsanzahl
     */
    fun adjustTimeTarget(plan: WorkoutPlan, targetMinutes: Int): WorkoutPlan {
        val factor = when (targetMinutes) {
            30 -> 0.6f  // 60% der Übungen
            45 -> 1.0f  // Standard
            60 -> 1.2f  // 20% mehr Übungen
            90 -> 1.5f  // 50% mehr Übungen
            else -> 1.0f
        }
        
        val exerciseBlocks = plan.exercises.filter { it.type == "ex" }
        val otherBlocks = plan.exercises.filter { it.type != "ex" }
        
        val targetExCount = (exerciseBlocks.size * factor).roundToInt().coerceAtLeast(3)
        val adjustedExercises = if (factor < 1.0f) {
            // Reduce: Keep most important exercises (first in list)
            exerciseBlocks.take(targetExCount)
        } else if (factor > 1.0f) {
            // Increase: Add more sets to existing exercises
            exerciseBlocks.map { block ->
                if (block.sets != null) {
                    block.copy(sets = (block.sets * factor).roundToInt().coerceAtMost(6))
                } else {
                    block
                }
            }
        } else {
            exerciseBlocks
        }
        
        val newBlocks = otherBlocks.filter { it.type == "warmup" } + adjustedExercises + otherBlocks.filter { it.type == "cooldown" }
        val estMinutes = (targetMinutes * 0.9).roundToInt() // Slight underestimate
        
        return plan.copy(exercises = newBlocks, targetMinutes = targetMinutes, estTotalMinutes = estMinutes)
    }

    /**
     * Findet eine alternative Übung mit kompatiblem Equipment.
     */
    private fun findAlternativeExercise(originalEx: String, equipment: String, category: String): String {
        val alternatives = when (category) {
            "push" -> when (equipment) {
                "bodyweight" -> listOf("Liegestütze", "Pike Pushups", "Diamond Pushups", "Dips am Stuhl")
                "minimal" -> listOf("Liegestütze", "Schulterpresse KH", "Fliegende KH")
                else -> listOf(originalEx)
            }
            "pull" -> when (equipment) {
                "bodyweight" -> listOf("Superman", "Handtuch-Latzug", "Floor Slides")
                "minimal" -> listOf("Klimmzüge", "Bizeps Curls KH", "Superman")
                else -> listOf(originalEx)
            }
            "legs" -> when (equipment) {
                "bodyweight" -> listOf("Kniebeugen", "Ausfallschritte", "Glute Bridges", "Step-ups")
                "minimal" -> listOf("Kniebeugen KH", "Ausfallschritte KH", "Glute Bridges")
                else -> listOf(originalEx)
            }
            "core" -> listOf("Plank", "Crunches", "Beinheben", "Dead Bug", "Bird Dog") // All bodyweight
            "cross" -> when (equipment) {
                "bodyweight" -> listOf("Burpees", "Hampelmänner", "Mountain Climbers", "Skaters")
                "minimal" -> listOf("Burpees", "Thruster KH", "Skaters")
                else -> listOf(originalEx)
            }
            else -> listOf("Plank") // Fallback
        }
        return alternatives.shuffled().first()
    }

    /**
     * Findet eine sichere Alternative bei gesundheitlichen Einschränkungen.
     */
    private fun findSafeAlternative(originalEx: String, category: String, restrictions: List<String>): String {
        val safeAlternatives = when (category) {
            "legs" -> if ("knie" in restrictions) {
                listOf("Glute Bridges", "Step-ups", "Wandsitzen") // Low-impact
            } else {
                listOf("Kniebeugen", "Ausfallschritte")
            }
            "pull" -> listOf("Superman", "Floor Slides", "Handtuch-Latzug") // Safe for back
            "push" -> if ("schulter" in restrictions) {
                listOf("Liegestütze", "Dips am Stuhl") // No overhead
            } else {
                listOf("Liegestütze", "Pike Pushups")
            }
            "core" -> listOf("Dead Bug", "Bird Dog", "Plank") // Safe for most restrictions
            "cross" -> if ("herz" in restrictions) {
                listOf("Hampelmänner", "Skaters") // Lower intensity
            } else {
                listOf("Burpees", "Mountain Climbers")
            }
            else -> listOf("Plank")
        }
        return safeAlternatives.shuffled().first()
    }
}

data class DailyModifier(
    val intensityFactor: Float = 1.0f,
    val volumeFactor: Float = 1.0f,
    val timeFactor: Float = 1.0f,
    val readinessScore: Int = 70
)
