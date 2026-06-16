package com.fantasyfoodplanner.features.fitness

import com.fantasyfoodplanner.logic.ExerciseType

object ExerciseDefinitions {
    private val definitionCache = mutableMapOf<String, ExerciseDefinition>()

    // DATA AUDIT: ExerciseType ist Source of Truth für die Logik (Gewicht/Zeit/Reps).
    // category dient der UI-Steuerung (Animationen).
    private val rules = listOf(
        // --- WEIGHTED (STUDIO / KRAFT) ---
        Rule("(bankdrücken|benchpress)", ExerciseType.WEIGHTED, "push", "Bankdrücken: Fokus auf die Brust.", listOf("Schulterblätter fest", "Stange kontrolliert zur Brust", "Füße am Boden"), "https://www.youtube.com/watch?v=gRVjAtPip0Y", "gRVjAtPip0Y", "full", false),
        Rule("(schulterpresse|shoulder press|ohp)", ExerciseType.WEIGHTED, "push", "Schulterpresse: Kraft für die Schultern.", listOf("Bauch anspannen", "Kein Hohlkreuz", "Ellbogen leicht vorne"), null, "2yjwXTZQDDI", "minimal", true),
        Rule("(latzug|lat-?pull)", ExerciseType.WEIGHTED, "pull", "Latzug: Fokus breiter Rücken.", listOf("Brust raus", "Schultern unten lassen", "Kein Schwung"), null, "CAwf7n6Lwqo", "full", false),
        Rule("(beinpresse|leg-?press)", ExerciseType.WEIGHTED, "legs", "Beinpresse: Beinkraft an der Maschine.", listOf("Rücken fest ans Polster", "Knie nicht einrasten", "Volle Range"), null, "IZxyjW7MPJQ", "full", false),
        Rule("(beinstrecker|leg-?extension)", ExerciseType.WEIGHTED, "legs", "Beinstrecker: Isolation Quadrizeps.", listOf("Oberkörper stabil", "Kurz halten am obersten Punkt"), null, "YyvSfVjQeL0", "full", false),
        Rule("(beinbeuger|leg-?curl)", ExerciseType.WEIGHTED, "legs", "Beinbeuger: Rückseite Oberschenkel.", listOf("Hüfte am Polster lassen", "Fersen zum Gesäß"), null, "1Tq3QdYUuHs", "full", false),
        Rule("(kreuzheben|deadlift)", ExerciseType.WEIGHTED, "legs", "Kreuzheben: Ganzkörper-Power.", listOf("Rücken gerade", "Stange nah am Körper", "Kraft aus den Beinen"), null, "op9kVnSso6Q", "minimal", true),
        Rule("(bizeps-?curl|bicep-?curl)", ExerciseType.WEIGHTED, "pull", "Bizeps Curls: Isolation der Oberarme.", listOf("Ellbogen fixiert", "Kein Schwung aus der Hüfte"), null, "ykJmrZ5v0Oo", "minimal", true),
        Rule("(trizeps-?drücken|pushdown)", ExerciseType.WEIGHTED, "push", "Trizepsdrücken: Fokus Armrückseite.", listOf("Ellbogen stabil", "Vollständig strecken"), null, "2-LAMcpzODU", "full", false),
        Rule("(seitheben|lateral-?raise)", ExerciseType.WEIGHTED, "push", "Seitheben: Seitliche Schultern.", listOf("Ellbogen leicht gebeugt", "Hände bis Schulterhöhe"), null, "3VcKaXpzqRo", "minimal", true),
        Rule("(butterfly|peck-?deck)", ExerciseType.WEIGHTED, "push", "Butterfly: Brust-Isolation.", listOf("Arme leicht gebeugt", "Hände vorne zusammenführen"), null, "Z6wy1vp0jQQ", "full", false),
        Rule("(flys|fliegende)", ExerciseType.WEIGHTED, "push", "Flys: Dehnung der Brust.", listOf("Großer Bogen", "Spannung halten"), null, "eozdVDA78K0", "minimal", true),
        Rule("(wadenheben) (maschine|gewicht|lh|kh)", ExerciseType.WEIGHTED, "legs", "Wadenheben: Fokus Waden.", listOf("Volle Dehnung", "Kraftvoll hoch"), null, "gwLzBJYoWlI", "minimal", true),
        Rule("(kniebeuge|squat) (lh|kh|gewicht|hantel)", ExerciseType.WEIGHTED, "legs", "Kniebeugen (Weighted): Kraft für die Beine.", listOf("Rücken gerade", "Fersen am Boden", "Tiefe stabilisieren"), null, "ultWZbUMPL8", "minimal", true),
        Rule("(ausfallschritte|lunges) (kh|lh|gewicht|hantel)", ExerciseType.WEIGHTED, "legs", "Ausfallschritte (Weighted): Einbeinige Beinkraft.", listOf("Aufrechter Stand", "Knie stabil"), null, "QOVaHwm-Q6U", "minimal", true),
        Rule("(thruster)", ExerciseType.WEIGHTED, "cross", "Thruster: Beugen und Drücken.", listOf("Flüssige Bewegung", "Ganzkörper-Power"), null, "L219ltL15zk", "minimal", true),

        // --- BODYWEIGHT (ZUHAUSE / BASICS) ---
        Rule("(liegestütze|push-?up)", ExerciseType.BODYWEIGHT, "push", "Liegestütze: Körperspannung halten.", listOf("Ellbogen 45 Grad", "Nase zum Boden"), null, "IODxDxX7oi4", "bodyweight", true),
        Rule("(pike pushup)", ExerciseType.BODYWEIGHT, "push", "Pike Pushups: Fokus auf Schultern.", listOf("Hüfte hoch", "Ellbogen nach hinten"), null, "x4YNi4nRboU", "bodyweight", true),
        Rule("(diamond pushup)", ExerciseType.BODYWEIGHT, "push", "Diamond Pushups: Fokus auf Trizeps.", listOf("Hände bilden einen Diamant", "Eng am Körper"), null, "J0DnG1_S92I", "bodyweight", true),
        Rule("(dips am stuhl)", ExerciseType.BODYWEIGHT, "push", "Dips am Stuhl: Kraft aus dem Trizeps.", listOf("Rücken nah am Stuhl", "Tief gehen"), null, "0326dy_-CzM", "bodyweight", true),
        Rule("(klimmzug|pull-?up)", ExerciseType.BODYWEIGHT, "pull", "Klimmzüge: Klassische Rückenübung.", listOf("Brust zur Stange", "Schultern unten"), null, "eGo4IYlbE5g", "minimal", true),
        Rule("(superman)", ExerciseType.BODYWEIGHT, "pull", "Superman: Kräftigung Rückenstrecker.", listOf("Blick zum Boden", "Arme/Beine heben"), null, "cc6UVRS7PW4", "bodyweight", true),
        Rule("(handtuch-latzug)", ExerciseType.BODYWEIGHT, "pull", "Handtuch-Latzug: Isometrische Spannung.", listOf("Handtuch auseinanderziehen", "Zur Brust führen"), null, "", "bodyweight", true),
        Rule("(floor slides)", ExerciseType.BODYWEIGHT, "pull", "Floor Slides: Schultermobilität.", listOf("Rücken am Boden", "Arme gleiten lassen"), null, "", "bodyweight", true),
        Rule("(kniebeuge|squat)", ExerciseType.BODYWEIGHT, "legs", "Kniebeugen: Körpergewicht-Beuge.", listOf("Fersen am Boden", "Rücken gerade"), null, "YaXPRqUwItQ", "bodyweight", true),
        Rule("(ausfallschritte|lunges)", ExerciseType.BODYWEIGHT, "legs", "Ausfallschritte: Klassische Lunges.", listOf("Oberkörper aufrecht", "Knie stabil"), null, "QOVaHwm-Q6U", "bodyweight", true),
        Rule("(glute bridge)", ExerciseType.BODYWEIGHT, "legs", "Glute Bridges: Fokus Gesäß.", listOf("Fersen drücken", "Oben anspannen"), null, "wPM8icPu6H8", "bodyweight", true),
        Rule("(step-ups)", ExerciseType.BODYWEIGHT, "legs", "Step-ups: Aufsteiger.", listOf("Kraft aus dem Vorderbein", "Stabil landen"), null, "dQqApCGd5Ss", "bodyweight", true),
        Rule("(burpees)", ExerciseType.BODYWEIGHT, "cross", "Burpees: Ganzkörper-HIIT.", listOf("Ganz ablegen", "Explosiv springen"), null, "TU8QYVW0gDU", "bodyweight", true),
        Rule("(skaters)", ExerciseType.BODYWEIGHT, "cross", "Skaters: Seitliche Sprünge.", listOf("Tief landen", "Balance"), null, "qvLR6VHrXKU", "bodyweight", true),
        Rule("(box jump)", ExerciseType.BODYWEIGHT, "cross", "Box Jumps: Explosivsprung.", listOf("Sanft landen", "Oben aufrichten"), null, "NBY9-kTuHEk", "bodyweight", true),

        // --- TIME BASED (CARDIO / CORE) ---
        Rule("(plank|unterarmstütz)", ExerciseType.TIME, "core", "Plank: Den Körper steif halten.", listOf("Kein Hohlkreuz", "Ellbogen unter Schulter"), null, "ASdvN_XEl_c", "bodyweight", true),
        Rule("(mountain climber)", ExerciseType.TIME, "core", "Mountain Climbers: Dynamischer Core.", listOf("Hüfte tief", "Schnelle Knie"), null, "nmwgirgXLYM", "bodyweight", true),
        Rule("(wandsitzen|wall sit)", ExerciseType.TIME, "legs", "Wandsitzen: Isometrische Beinkraft.", listOf("90 Grad Winkel", "Rücken fest an Wand"), null, "y-wV4Venusw", "bodyweight", true),
        Rule("(hampelmänner)", ExerciseType.TIME, "cross", "Hampelmänner: Aktivierung.", listOf("Leichtfüßig", "Arme über Kopf"), null, "c4DAnQ6DtF8", "bodyweight", true),
        Rule("(plank jacks)", ExerciseType.TIME, "cross", "Plank Jacks: Puls & Core.", listOf("Wie Hampelmänner in Plank", "Rücken stabil"), null, "", "bodyweight", true),

        // --- ALLGEMEINE REPS ---
        Rule("(crunch)", ExerciseType.BODYWEIGHT, "core", "Crunches: Fokus Bauch.", listOf("LWS am Boden", "Kurze Bewegung"), null, "Xyd_fa5zoEU", "bodyweight", true),
        Rule("(beinheben)", ExerciseType.BODYWEIGHT, "core", "Beinheben: Fokus unterer Bauch.", listOf("Hände unter Gesäß", "Beine gestreckt"), null, "JB2oyawG9KI", "bodyweight", true),
        Rule("(russian twist)", ExerciseType.BODYWEIGHT, "core", "Russian Twist: Core-Rotation.", listOf("Rücken gerade", "Füße in der Luft"), null, "wkD8rjkodUI", "bodyweight", true),
        Rule("(dead bug)", ExerciseType.BODYWEIGHT, "core", "Dead Bug: Diagonale Stabilität.", listOf("Rücken flach", "Langsame Bewegung"), null, "g_BYB0R-4Ws", "bodyweight", true),
        Rule("(bird dog)", ExerciseType.BODYWEIGHT, "core", "Bird Dog: Balance & Rücken.", listOf("Diagonal strecken", "Nicht wackeln"), null, "wiFNA3sqjCA", "bodyweight", true)
    )

    fun get(name: String): ExerciseDefinition {
        val lowerName = name.lowercase()
        definitionCache[lowerName]?.let { return it }
        
        // 1. Suche nach exakten Regeln
        for (rule in rules) {
            if (Regex(rule.match, RegexOption.IGNORE_CASE).containsMatchIn(lowerName)) {
                val newDef = ExerciseDefinition(rule.exerciseType, rule.category, rule.desc, rule.cues, rule.videoUrl, rule.youtubeVideoId, rule.equipment, rule.homeFriendly)
                definitionCache[lowerName] = newDef
                return newDef
            }
        }
        
        // 2. AUTO-ERKENNUNG für Gewicht (falls keine Regel griff)
        if (lowerName.contains("lh") || lowerName.contains("kh") || lowerName.contains("hantel") || 
            lowerName.contains("gewicht") || lowerName.contains("maschine") || lowerName.contains("kabelzug") || 
            lowerName.contains("presse") || lowerName.contains("curl") || lowerName.contains("deadlift") || 
            lowerName.contains("flys")) {
            return ExerciseDefinition(ExerciseType.WEIGHTED, "core", "Kraftübung mit Gewicht.", listOf("Auf saubere Technik achten", "Gewicht kontrolliert bewegen"), null, "", "minimal", true)
        }

        // 3. FALLBACK
        return ExerciseDefinition(ExerciseType.BODYWEIGHT, "core", "Standard-Übung", listOf("Kontrolliert ausführen", "Auf die Atmung achten"), null, "", "bodyweight", true)
    }

    private data class Rule(val match: String, val exerciseType: ExerciseType, val category: String, val desc: String, val cues: List<String>, val videoUrl: String?, val youtubeVideoId: String = "", val equipment: String = "full", val homeFriendly: Boolean = false)
}

data class ExerciseDefinition(
    val type: ExerciseType,
    val category: String,
    val desc: String,
    val cues: List<String>,
    val videoUrl: String? = null,
    val youtubeVideoId: String = "", // YouTube Video ID für eingebettetes Video
    val equipment: String = "full", // "full" = gym equipment, "minimal" = dumbbells/bands, "bodyweight" = no equipment
    val homeFriendly: Boolean = false // Can be done at home
)
