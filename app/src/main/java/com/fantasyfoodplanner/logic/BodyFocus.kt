package com.fantasyfoodplanner.logic

/**
 * Typsichere Enum für Körper-Fokus-Bereiche.
 * Ersetzt die String-basierten Mappings im BodySelector3D und TrainingFlowScreen.
 */
enum class BodyFocus(val displayName: String, val categories: List<ExerciseCategory>) {
    CHEST("Brust", listOf(ExerciseCategory.PUSH)),
    SHOULDERS("Schultern", listOf(ExerciseCategory.PUSH)),
    TRICEPS("Trizeps", listOf(ExerciseCategory.PUSH)),
    BACK("Rücken", listOf(ExerciseCategory.PULL)),
    BICEPS("Bizeps", listOf(ExerciseCategory.PULL)),
    LEGS("Beine", listOf(ExerciseCategory.LEGS)),
    GLUTES("Po / Gesäß", listOf(ExerciseCategory.LEGS)),
    CORE("Bauch / Core", listOf(ExerciseCategory.CORE)),
    NECK("Nacken", listOf(ExerciseCategory.CORE)),
    ARMS("Arme", listOf(ExerciseCategory.PUSH, ExerciseCategory.PULL));

    companion object {
        /**
         * Findet den BodyFocus anhand des Display-Namens (z.B. "Brust", "Rücken").
         * Fallback: null wenn nicht gefunden.
         */
        fun fromDisplayName(name: String): BodyFocus? {
            return entries.find { it.displayName.equals(name, ignoreCase = true) }
        }

        /**
         * Konvertiert eine Liste von Display-Namen zu BodyFocus-Set.
         */
        fun fromDisplayNames(names: Collection<String>): Set<BodyFocus> {
            return names.mapNotNull { fromDisplayName(it) }.toSet()
        }

        /**
         * Extrahiert alle Übungskategorien aus einem Set von BodyFocus.
         */
        fun toCategories(focusSet: Set<BodyFocus>): Set<ExerciseCategory> {
            return focusSet.flatMap { it.categories }.toSet()
        }

        /**
         * Konvertiert String-Set (alte API) zu Kategorie-Strings (für Kompatibilität).
         */
        fun toCategoryStrings(focusNames: Set<String>): Set<String> {
            val focuses = fromDisplayNames(focusNames)
            return toCategories(focuses).map { it.categoryId }.toSet()
        }
    }
}

/**
 * Übungskategorien für das Filtern von Übungen.
 */
enum class ExerciseCategory(val categoryId: String, val displayName: String) {
    PUSH("push", "Drücken"),
    PULL("pull", "Ziehen"),
    LEGS("legs", "Beine"),
    CORE("core", "Core"),
    CROSS("cross", "CrossFit");

    companion object {
        fun fromId(id: String): ExerciseCategory? {
            return entries.find { it.categoryId == id }
        }
    }
}

