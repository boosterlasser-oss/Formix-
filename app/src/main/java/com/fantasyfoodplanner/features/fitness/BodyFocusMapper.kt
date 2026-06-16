package com.fantasyfoodplanner.features.fitness

import com.fantasyfoodplanner.logic.BodyFocus

/**
 * V3.2.0: Mapper zwischen deutschen Fokusbereich-Namen (aus Onboarding/Profil)
 * und den internen BodyFocus Enum-Werten für die adaptive Fokus-Selektion.
 * 
 * Verwendung:
 * - Onboarding speichert: "bauch,beine,po" als String im Profil
 * - TrainingFlowScreen lädt dies und konvertiert zu Set<BodyFocus>
 * - WorkoutEntry speichert fokussierte Bereiche als String für späteres Lernen
 */
object BodyFocusMapper {
    
    /**
     * Konvertiert deutsche Fokusbereich-Namen (aus Profil) zu BodyFocus Set.
     * 
     * @param profileString Komma-getrennte deutsche Namen: "bauch,beine,po"
     * @return Set von BodyFocus Enums (z.B. {CORE, LEGS, GLUTES})
     */
    fun fromProfileString(profileString: String?): Set<BodyFocus> {
        if (profileString.isNullOrBlank()) return emptySet()
        
        return profileString.split(",")
            .map { it.trim().lowercase() }
            .mapNotNull { germanName ->
                when (germanName) {
                    "bauch" -> BodyFocus.CORE
                    "beine" -> BodyFocus.LEGS
                    "po", "gesäß" -> BodyFocus.GLUTES
                    "arme" -> BodyFocus.ARMS
                    "rücken", "ruecken" -> BodyFocus.BACK
                    "brust" -> BodyFocus.CHEST
                    "schultern" -> BodyFocus.SHOULDERS
                    else -> null // Unbekannte Namen ignorieren
                }
            }
            .toSet()
    }
    
    /**
     * Konvertiert BodyFocus Set zurück zu deutschen Namen für Profil-Speicherung.
     * 
     * @param focusSet Set von BodyFocus Enums
     * @return Komma-getrennte deutsche Namen: "bauch,beine,po"
     */
    fun toProfileString(focusSet: Set<BodyFocus>): String {
        return focusSet.mapNotNull { focus ->
            when (focus) {
                BodyFocus.CORE -> "bauch"
                BodyFocus.LEGS -> "beine"
                BodyFocus.GLUTES -> "po"
                BodyFocus.ARMS -> "arme"
                BodyFocus.BACK -> "rücken"
                BodyFocus.CHEST -> "brust"
                BodyFocus.SHOULDERS -> "schultern"
                else -> null
            }
        }.joinToString(",")
    }
    
    /**
     * Konvertiert BodyFocus Set zu String von BodyFocus Namen (für WorkoutEntry.focusAreas).
     * 
     * @param focusSet Set von BodyFocus Enums
     * @return Komma-getrennte BodyFocus Namen: "CORE,LEGS,GLUTES"
     */
    fun toEnumString(focusSet: Set<BodyFocus>): String {
        return focusSet.joinToString(",") { it.name }
    }
    
    /**
     * Parst BodyFocus Namen String zurück zu Set (aus WorkoutEntry.focusAreas).
     * 
     * @param enumString Komma-getrennte BodyFocus Namen: "CORE,LEGS,GLUTES"
     * @return Set von BodyFocus Enums
     */
    fun fromEnumString(enumString: String?): Set<BodyFocus> {
        if (enumString.isNullOrBlank()) return emptySet()
        
        return enumString.split(",")
            .map { it.trim() }
            .mapNotNull { name ->
                try {
                    BodyFocus.valueOf(name)
                } catch (e: IllegalArgumentException) {
                    null // Ungültige Namen ignorieren
                }
            }
            .toSet()
    }
}
