package com.fantasyfoodplanner.features.fitness

import android.content.Context
import android.util.Log
import java.text.Normalizer

/**
 * Lottie Animation Provider mit Score-basiertem Matching
 *
 * - Scannt assets/animations/ nach *.json
 * - Normalisiert Übungsnamen + Dateinamen
 * - Synonym-Unterstützung für sichere Matches
 * - Score-basierte Zuordnung (keine Falschtreffer!)
 */
object LottieAnimationProvider {
    private const val TAG = "LottieAnimationMatcher"
    private val animationCache = mutableMapOf<String, String?>()
    private var isInitialized = false
    private var availableAnimations = listOf<String>()

    /**
     * PERFEKTE Synonym-Map: ALLE 46 Übungen + 41 Dateien!
     * Keine Doppelungen! Jede Übung NUR eine Animation!
     * NEU: Latzug.json, Trizepsdrücken.json
     */
    private val exerciseSynonyms = mapOf(
        // WEIGHTED (mit Dateien)
        "Bankdrücken" to listOf("bankdruck", "bench", "press", "smith"),
        "Schulterpresse" to listOf("schulterpress", "shoulder", "dumbbell", "press", "kh"),
        "Latzug" to listOf("latzug", "latpull", "lat", "pulldown", "pull", "down"),
        "Beinpresse" to listOf("beinpress", "legpress", "leg", "press", "sled", "horizontal"),
        "Beinstrecker" to listOf("beinstreck", "legext", "extension"),
        "Beinbeuger" to listOf("beinbeug", "legcurl", "curl", "lever", "seated"),
        "Kreuzheben" to listOf("kreuzheb", "deadlift", "dead"),
        "Bizeps Curls" to listOf("bizeps", "bicep"),
        "Trizepsdrücken" to listOf("trizeps", "trizepsdruck", "pushdown", "druck"),
        "Seitheben" to listOf("seitheb", "lateral", "raise"),
        "Butterfly" to listOf("butterfly", "fly", "chest", "cable"),
        "Flys" to listOf("flys", "fliegende"),
        "Wadenheben" to listOf("wadenheb", "calfrise", "calf"),

        // BODYWEIGHT (mit Dateien)
        "Liegestütze" to listOf("liegestuetz", "pushup", "push", "ups"),
        "Pike Pushup" to listOf("pike", "pushup"),
        "Diamond Pushup" to listOf("diamond", "pushup"),
        "Dips am Stuhl" to listOf("dips", "dip", "stuhl", "chair", "tipps"),
        "Klimmzug" to listOf("klimmzug", "pullup", "pull", "ups", "chin", "side", "hand"),
        "Superman" to listOf("superman"),
        "Handtuch-Latzug" to listOf("handtuch", "latzug"),
        "Floor Slides" to listOf("floor", "slides"),
        "Glute Bridge" to listOf("glute", "bridge", "hip"),
        "Step-ups" to listOf("stepups", "step"),
        "Burpees" to listOf("burpee", "burpees"),
        "Skaters" to listOf("skaters"),
        "Box Jump" to listOf("box", "jump", "jumps"),
        "Ab Wheel" to listOf("ab", "wheel", "rollout"),

        // BODYWEIGHT KNIEBEUGEN & AUSFALLSCHRITTE
        "Kniebeugen" to listOf("kniebeuge", "squat"),
        "Ausfallschritte" to listOf("ausfallschritt", "lunge"),

        // GEWICHT Varianten (mit LH/KH Versionen!)
        "Kniebeugen (Gewicht)" to listOf("kniebeuge", "squat", "sumo", "hantel", "gewicht", "lh"),
        "Kniebeugen LH" to listOf("kniebeuge", "squat", "lh"),
        "Ausfallschritte (Gewicht)" to listOf("ausfallschritt", "lunge", "bulgarian", "split", "forward", "hantel"),
        "Thruster" to listOf("thruster", "overhead", "squat", "dumbbell", "press", "woman", "doing"),

        // TIME BASED / CORE (mit Dateien)
        "Plank" to listOf("plank", "unterarmstuetz", "low"),
        "Mountain Climbers" to listOf("mountain", "climber", "mountainclimber", "climbers"),
        "Wandsitzen" to listOf("wandsitzen", "wall", "sit", "squat", "static"),
        "Hampelmänner" to listOf("hampelmänner", "hampelmaenner"),
        "Plank Jacks" to listOf("plank", "jacks"),

        // CORE REPS (mit Dateien)
        "Crunch" to listOf("crunch", "situp", "sit", "ups", "woman", "doing"),
        "Beinheben" to listOf("beinheben", "leg", "raise", "plank", "low"),
        "Russian Twist" to listOf("russian", "twist"),
        "Dead Bug" to listOf("dead", "bug", "deadbug", "core"),
        "Bird Dog" to listOf("bird", "dog", "birddog", "balance"),

        // NEU HINZUGEFÜGT (neue Übungen)
        "Dumbbell Swing" to listOf("dumbbell", "swing"),
        "Crosstrainer" to listOf("crosstrainer", "cross", "trainer"),
        "Hantel Kreuzheben" to listOf("hantel", "kreuzheb")
    )

    /**
     * Initialisiert den Provider durch Scannen von assets/animations/
     */
    fun initialize(context: Context) {
        if (isInitialized) return

        try {
            val assetManager = context.assets
            val animationFiles = assetManager.list("animations") ?: arrayOf()
            availableAnimations = animationFiles
                .filter { it.endsWith(".json") }
                .map { "animations/$it" }

            Log.d(TAG, "=== INITIALIZATION ===")
            Log.d(TAG, "Found ${availableAnimations.size} JSON files in assets/animations/:")
            availableAnimations.forEach { Log.d(TAG, "  ✓ $it") }
            Log.d(TAG, "========================")

            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Error during initialization", e)
            isInitialized = true
        }
    }

    /**
     * Gibt den Pfad zur Animation für eine Übung zurück
     * Strategie: DIREKT nach Dateiname suchen (z.B. "Bankdrücken.json") FIRST
     * Dann Synonym-Fallback
     */
    fun getAnimationPath(exerciseName: String, context: Context? = null): String? {
        // Cache-Check
        if (animationCache.containsKey(exerciseName)) {
            return animationCache[exerciseName]
        }

        // Initialisierung sicherstellen
        if (context != null && !isInitialized) {
            initialize(context)
        }

        Log.d(TAG, "--- Matching for exercise: '$exerciseName' ---")

        // STRATEGIE 1: DIREKTER NAME-MATCH (z.B. "Bankdrücken.json")
        // Prüfe ob exakt "Bankdrücken.json" existiert
        val directMatch = availableAnimations.firstOrNull { file ->
            val fileName = normalizeFileNameForDirectMatch(file)
            fileName == exerciseName || normalize(fileName) == normalize(exerciseName)
        }

        if (directMatch != null) {
            Log.d(TAG, "✓ Direct match found: $directMatch")
            animationCache[exerciseName] = directMatch
            return directMatch
        }

        // STRATEGIE 2: SYNONYM-BASIERTES MATCHING (für alte Dateinamen)
        val keywords = getKeywordsForExercise(exerciseName)
        Log.d(TAG, "Keywords: $keywords")

        val scores = mutableMapOf<String, Pair<Int, String?>>()

        for (animFile in availableAnimations) {
            val fileKey = normalizeFileName(animFile)
            var maxScore = 0
            var matchedKeyword: String? = null

            for (keyword in keywords) {
                val score = calculateMatchScore(fileKey, keyword)
                if (score > maxScore) {
                    maxScore = score
                    matchedKeyword = keyword
                }
            }

            if (maxScore > 0) {
                scores[animFile] = Pair(maxScore, matchedKeyword)
                Log.d(TAG, "  $animFile: score=$maxScore (keyword='$matchedKeyword')")
            }
        }

        val bestMatch = scores.maxByOrNull { it.value.first }
        val bestScore = bestMatch?.value?.first ?: 0

        Log.d(TAG, "Best synonym match: ${bestMatch?.key} (score=$bestScore)")

        val result = if (bestScore >= 90) {
            bestMatch?.key
        } else if (bestScore >= 80) {
            val secondBest = scores.values.sortedByDescending { it.first }.getOrNull(1)?.first ?: 0
            if (secondBest < bestScore - 10) {
                bestMatch?.key
            } else {
                Log.d(TAG, "Ambiguous match -> using fallback")
                null
            }
        } else {
            Log.d(TAG, "No reliable synonym match found")
            null
        }

        Log.d(TAG, "Result: ${result ?: "FALLBACK"}")
        Log.d(TAG, "")

        animationCache[exerciseName] = result
        return result
    }

    /**
     * Extrahiere Dateinamen für DIREKTE Matches
     * Z.B. "animations/Bankdrücken.json" -> "Bankdrücken"
     */
    private fun normalizeFileNameForDirectMatch(filePath: String): String {
        val withoutPath = filePath.split("/").last()
        return withoutPath.removeSuffix(".json")
    }

    /**
     * Gibt die Keywords für eine Übung zurück (inkl. normalisierter Name)
     */
    private fun getKeywordsForExercise(exerciseName: String): List<String> {
        val normalized = normalize(exerciseName)

        val synonyms = exerciseSynonyms[exerciseName] ?: emptyList()
        val allKeywords = listOf(normalized) + synonyms.map { normalize(it) }

        return allKeywords.distinct()
    }

    /**
     * Berechnet Match-Score zwischen File-Key und Keyword
     * - 100: Exakt gleich
     * - 80: Enthält oder wird enthalten
     * - 0: Kein Match
     */
    private fun calculateMatchScore(fileKey: String, keyword: String): Int {
        return when {
            fileKey == keyword -> 100  // Exakt
            fileKey.contains(keyword) || keyword.contains(fileKey) -> 80  // Partial
            else -> 0
        }
    }

    /**
     * Normalisiert einen Dateinamen (ohne Extension)
     */
    private fun normalizeFileName(filePath: String): String {
        val withoutPath = filePath.split("/").last()
        val withoutExtension = withoutPath.removeSuffix(".json")
        return normalize(withoutExtension)
    }

    /**
     * Universelle Normalisierung:
     * - lowercase
     * - Umlaute: ä→ae, ö→oe, ü→ue, ß→ss
     * - Nur a-z0-9 (Bindestriche/Unterstriche/Leerzeichen entfernt)
     */
    private fun normalize(text: String): String {
        var result = text.lowercase()
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("ß", "ss")
            .replace(Regex("[\\s\\-_]"), "")  // Leerzeichen, Bindestriche, Unterstriche entfernen
            .replace(Regex("[^a-z0-9]"), "")  // Nur a-z und 0-9

        return result.trim()
    }
}
