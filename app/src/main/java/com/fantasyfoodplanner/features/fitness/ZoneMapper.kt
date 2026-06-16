package com.fantasyfoodplanner.features.fitness

/**
 * Mappt 3D-Körperzonen (zone_*) zu Trainings-Fokusgruppen
 * Single Source of Truth für Zone → Muskelgruppe Zuordnung
 */
object ZoneMapper {

    // Interne Mappings: zone_name -> Fokus-Gruppe (wie im FocusDropdownField)
    private val zoneToFocus = mapOf(
        // Kopf/Nacken
        "zone_head" to "Nacken",
        "zone_neck" to "Nacken",

        // Brust & Oberer Rücken
        "zone_chest_front" to "Brust",
        "zone_upper_back" to "Rücken",

        // Bauch & Rücken unten
        "zone_abs_front" to "Bauch / Core",
        "zone_lower_back" to "Rücken",
        "zone_obliques_L" to "Bauch / Core",
        "zone_obliques_R" to "Bauch / Core",

        // Schultern
        "zone_shoulder_L" to "Schultern",
        "zone_shoulder_R" to "Schultern",

        // Arme
        "zone_biceps_L" to "Bizeps",
        "zone_biceps_R" to "Bizeps",
        "zone_triceps_L" to "Trizeps",
        "zone_triceps_R" to "Trizeps",
        "zone_forearm_L" to "Arme",
        "zone_forearm_R" to "Arme",

        // Unterkörper
        "zone_glutes_L" to "Po / Gesäß",
        "zone_glutes_R" to "Po / Gesäß",
        "zone_quads_L" to "Beine",
        "zone_quads_R" to "Beine",
        "zone_hamstrings_L" to "Beine",
        "zone_hamstrings_R" to "Beine",
        "zone_calves_L" to "Beine",
        "zone_calves_R" to "Beine",
        "zone_tibialis_L" to "Beine",
        "zone_tibialis_R" to "Beine"
    )

    /**
     * Mappt eine Zone zu einer Fokusgruppe
     * @param zoneName Name des Nodes im GLB-Modell, z.B. "zone_chest_front"
     * @return Fokusgruppen-Name oder null, wenn unbekannt
     */
    fun mapZoneToFocus(zoneName: String): String? {
        return zoneToFocus[zoneName]
    }

    /**
     * Gibt alle bekannten Zone-Namen zurück
     */
    fun getAllZoneNames(): List<String> = zoneToFocus.keys.toList()

    /**
     * Konvertiert mehrere Fokusgruppen zu einem kombinierten String
     * z.B. ["Brust", "Rücken"] → "Brust, Rücken"
     */
    fun formatFocusGroups(groups: Set<String>): String {
        return groups.sorted().joinToString(", ")
    }
}

