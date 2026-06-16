package com.fantasyfoodplanner.logic

import com.fantasyfoodplanner.data.*
import java.util.Locale

object SearchProcessor {

    private val synonyms = mapOf(
        "fleisch" to listOf("hähnchen", "rind", "hack", "pute", "steak", "schnitzel", "geflügel", "rinderhüfte", "putenbrust"),
        "fisch" to listOf("lachs", "thunfisch", "dorsch", "garnelen", "forelle", "scholle", "meeresfrüchte"),
        "eiweiß" to listOf("protein", "quark", "skyr", "eier", "bohnen", "linsen", "fleisch", "whey", "hüttenkäse", "tofu"),
        "protein" to listOf("eiweiß", "quark", "skyr", "eier", "bohnen", "linsen", "fleisch", "whey", "hüttenkäse", "tofu"),
        "gemüse" to listOf("brokkoli", "karotte", "spinat", "zucchini", "paprika", "gurke", "tomate", "champignons", "zwiebel", "knoblauch", "lauch", "kürbis"),
        "obst" to listOf("apfel", "banane", "kiwi", "ananas", "erdbeere", "mango", "birne", "orange", "mandarine", "heidelbeere", "himbeere", "pfirsich", "avocado"),
        "nüsse" to listOf("mandeln", "walnüsse", "erdnussmus", "cashew", "haselnüsse"),
        "carbs" to listOf("kohlenhydrate", "reis", "nudeln", "pasta", "haferflocken", "kartoffel", "brot", "couscous", "bulgur", "toast"),
        "kohlenhydrate" to listOf("carbs", "reis", "nudeln", "pasta", "haferflocken", "kartoffel", "brot", "couscous", "bulgur", "toast"),
        "frühstück" to listOf("oats", "porridge", "quark", "skyr", "eier", "pancakes", "müsli", "toast"),
        "geflügel" to listOf("hähnchen", "pute", "huhn", "putenbrust", "chicken"),
        "salat" to listOf("eisberg", "rucola", "feldsalat", "tomate", "gurke", "romana")
    )

    // Keywords für Vor/Nach Training (Zusatz-Interpretation)
    private val PRE_WORKOUT_KEYWORDS = listOf("banane", "haferflocken", "reis", "couscous", "toast", "honig", "datteln", "obst", "smoothie", "joghurt", "skyr", "elektrolyt", "isotonic", "salz", "oats", "waffel")
    private val POST_WORKOUT_KEYWORDS = listOf("quark", "skyr", "whey", "protein", "eier", "huhn", "hähnchen", "thunfisch", "lachs", "tofu", "hüttenkäse", "reis", "kartoffel", "süßkartoffel", "nudeln", "pasta", "rind", "steak", "linsen", "curry")

    fun normalize(text: String): String {
        return text.lowercase(Locale.GERMANY)
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("ß", "ss")
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun fuzzyMatch(query: String, target: String): Boolean {
        if (query.length < 3) return target.contains(query)
        if (target.contains(query)) return true
        
        for (i in 0 until target.length - query.length + 1) {
            val sub = target.substring(i, i + query.length)
            var dist = 0
            for (j in query.indices) {
                if (query[j] != sub[j]) dist++
                if (dist > 1) break
            }
            if (dist <= 1) return true
        }
        return false
    }

    data class SearchQuery(
        val positiveTerms: List<String>,
        val negativeTerms: List<String>,
        val isOrLogic: Boolean,
        val detectedSort: String? = null,
        val isPreWorkoutRequested: Boolean = false,
        val isPostWorkoutRequested: Boolean = false
    )

    fun parseQuery(query: String): SearchQuery {
        val normalized = normalize(query)
        if (normalized.isEmpty()) return SearchQuery(emptyList(), emptyList(), false)

        val parts = normalized.split(" ")
        val isOr = parts.contains("oder")
        
        val pos = mutableListOf<String>()
        val neg = mutableListOf<String>()
        var detectedSort: String? = null
        
        // Workout Kontext Erkennung
        val isPre = normalized.contains("vor training") || normalized.contains("pre workout") || 
                    normalized.contains("preworkout") || normalized.contains("vor dem training")
        val isPost = normalized.contains("nach training") || normalized.contains("post workout") || 
                     normalized.contains("postworkout") || normalized.contains("nach dem training")

        parts.forEach { part ->
            when {
                part == "oder" -> {}
                part.startsWith("-") && part.length > 1 -> neg.add(part.substring(1))
                else -> {
                    // Strukturelle Begriffe nicht als harte Suchbegriffe werten
                    if (part !in listOf("vor", "nach", "training", "dem", "pre", "post", "workout", "preworkout", "postworkout")) {
                        pos.add(part)
                    }
                    // Intent Erkennung
                    when (part) {
                        "eiweiss", "protein", "muskelaufbau" -> detectedSort = "Protein"
                        "lowcarb" -> detectedSort = "KohlenhydrateAsc"
                        "kalorienarm", "diaet" -> detectedSort = "KalorienAsc"
                    }
                }
            }
        }
        
        if (normalized.contains("low carb")) detectedSort = "KohlenhydrateAsc"

        return SearchQuery(pos, neg, isOr, detectedSort, isPre, isPost)
    }

    fun calculateScore(item: Any, searchQuery: SearchQuery): Int {
        val itemName = normalize(getName(item))
        val itemCategory = normalize(getCategory(item))
        val itemIngredients = normalize(getIngredients(item))
        val itemTags = getTags(item).map { normalize(it) }

        // Negativfilter
        for (neg in searchQuery.negativeTerms) {
            if (itemName.contains(neg) || itemIngredients.contains(neg)) return 0
        }

        var totalScore = 0
        val matches = mutableSetOf<String>()

        if (searchQuery.positiveTerms.isNotEmpty()) {
            for (term in searchQuery.positiveTerms) {
                var termScore = 0
                var matched = false

                if (itemName == term) {
                    termScore = maxOf(termScore, 100)
                    matched = true
                } else if (itemName.contains(term)) {
                    termScore = maxOf(termScore, 80)
                    matched = true
                }
                if (itemIngredients.contains(term)) {
                    termScore = maxOf(termScore, 60)
                    matched = true
                }
                if (itemCategory.contains(term) || itemTags.any { it.contains(term) }) {
                    termScore = maxOf(termScore, 40)
                    matched = true
                }
                val expanded = synonyms[term] ?: emptyList()
                if (expanded.any { itemName.contains(it) || itemIngredients.contains(it) }) {
                    termScore = maxOf(termScore, 30)
                    matched = true
                }
                if (!matched && fuzzyMatch(term, itemName)) {
                    termScore = maxOf(termScore, 20)
                    matched = true
                }

                if (matched) {
                    totalScore += termScore
                    matches.add(term)
                }
            }

            // AND-Logik check
            if (!searchQuery.isOrLogic && matches.size < searchQuery.positiveTerms.size) {
                return 0
            }
            if (searchQuery.isOrLogic && matches.isEmpty()) return 0
        } else {
            // Keine positiven Begriffe, aber Workout-Kontext vorhanden
            if (searchQuery.isPreWorkoutRequested || searchQuery.isPostWorkoutRequested) {
                totalScore = 1 
            } else {
                return 1
            }
        }

        // Boost für Vor/Nach Training
        if (searchQuery.isPreWorkoutRequested) {
            if (isSuitable(itemName, itemIngredients, itemTags, PRE_WORKOUT_KEYWORDS)) {
                totalScore += 500 
            }
        }
        if (searchQuery.isPostWorkoutRequested) {
            if (isSuitable(itemName, itemIngredients, itemTags, POST_WORKOUT_KEYWORDS)) {
                totalScore += 400
            }
        }

        return totalScore
    }

    private fun isSuitable(name: String, ingredients: String, tags: List<String>, keywords: List<String>): Boolean {
        return keywords.any { name.contains(it) || ingredients.contains(it) || tags.any { tag -> tag.contains(it) } }
    }

    private fun getName(item: Any): String = when (item) {
        is FoodItem -> item.name
        is Recipe -> item.name
        is Product -> item.name
        else -> ""
    }

    private fun getCategory(item: Any): String = when (item) {
        is FoodItem -> item.category
        is Recipe -> item.category
        is Product -> "Produkte"
        else -> ""
    }

    private fun getIngredients(item: Any): String = when (item) {
        is Recipe -> item.ingredients
        else -> ""
    }

    private fun getTags(item: Any): List<String> = when (item) {
        is FoodItem -> item.performanceTags
        is Recipe -> PerformanceTagEngine.computeTags(item.kcal.toDouble(), item.protein, item.carbs, item.fat)
        is Product -> PerformanceTagEngine.computeTags(item.kcal.toDouble(), item.protein, item.carbs, item.fat)
        else -> emptyList()
    }
}
