package com.fantasyfoodplanner.features.fitness

/**
 * MuscleZone Enum for the 3D Viewer.
 */
enum class MuscleZone(val label: String) {
    CHEST("Brust"),
    TRICEPS("Trizeps"),
    SHOULDERS_FRONT("Vordere Schulter"),
    SHOULDERS_SIDE("Seitliche Schulter"),
    SHOULDERS_REAR("Hintere Schulter"),
    BACK_UPPER("Oberer Rücken"),
    BACK_LATS("Latissimus"),
    BICEPS("Bizeps"),
    FOREARMS("Unterarme"),
    CORE("Rumpf / Bauch"),
    GLUTES("Gesäß"),
    QUADRICEPS("Oberschenkel vorne"),
    HAMSTRINGS("Oberschenkel hinten"),
    CALVES("Waden")
}

data class DetailedInstruction(
    val exerciseId: String,
    val primaryZones: List<MuscleZone>,
    val secondaryZones: List<MuscleZone>,
    val startPosition: String,
    val movement: List<String>,
    val breathing: String,
    val commonMistakes: List<String>,
    val coachingTips: List<String>,
    val variants: String,
    val whenToStop: String,
    val youtubeVideoId: String = ""
)

object ExerciseProProvider {
    private val data = mapOf(
        "Kniebeugen" to DetailedInstruction(
            exerciseId = "Kniebeugen",
            primaryZones = listOf(MuscleZone.QUADRICEPS, MuscleZone.GLUTES),
            secondaryZones = listOf(MuscleZone.HAMSTRINGS, MuscleZone.CORE, MuscleZone.CALVES),
            startPosition = "Füße etwas weiter als schulterbreit aufstellen, Zehen leicht nach außen gedreht.",
            movement = listOf("Hüfte nach hinten schieben", "Oberschenkel parallel zum Boden", "Rücken gerade halten"),
            breathing = "Einatmen beim Senken, Ausatmen beim Aufstehen.",
            commonMistakes = listOf("Knie knicken nach innen", "Runder Rücken"),
            coachingTips = listOf("Boden auseinanderreißen", "Bauchspannung"),
            variants = "Box Squats, Goblet Squats.",
            whenToStop = "Bei Rückenschmerzen.",
            youtubeVideoId = "YaXPRqUwItQ"
        ),
        "Dips" to DetailedInstruction(
            exerciseId = "Dips",
            primaryZones = listOf(MuscleZone.TRICEPS, MuscleZone.CHEST),
            secondaryZones = listOf(MuscleZone.SHOULDERS_FRONT, MuscleZone.CORE),
            startPosition = "An den Barren stützen, Arme gestreckt, Schultern tief.",
            movement = listOf("Kontrolliert absenken", "Ellbogen nah am Körper", "Kraftvoll hochdrücken"),
            breathing = "Einatmen beim Senken, Ausatmen beim Drücken.",
            commonMistakes = listOf("Schultern zu den Ohren", "Zu tiefes Gehen"),
            coachingTips = listOf("Brust raus", "Stabilisierung im Core"),
            variants = "Unterstützte Dips.",
            whenToStop = "Bei Schulterstechen.",
            youtubeVideoId = "2z8JmcrW-As"
        ),
        "Bankdrücken" to DetailedInstruction(
            exerciseId = "Bankdrücken",
            primaryZones = listOf(MuscleZone.CHEST, MuscleZone.TRICEPS),
            secondaryZones = listOf(MuscleZone.SHOULDERS_FRONT),
            startPosition = "Rückenlage auf der Bank, Füße fest am Boden.",
            movement = listOf("Stange zur unteren Brust führen", "Ellbogen 45 Grad", "Explosiv hochdrücken"),
            breathing = "Einatmen beim Senken, Ausatmen beim Drücken.",
            commonMistakes = listOf("Hintern hebt ab", "Handgelenke knicken"),
            coachingTips = listOf("Schulterblätter zusammen", "Stange zerbrechen"),
            variants = "Schrägbankdrücken.",
            whenToStop = "Bei Schulterschmerz.",
            youtubeVideoId = "gRVjAtPip0Y"
        ),
        "Kreuzheben" to DetailedInstruction(
            exerciseId = "Kreuzheben",
            primaryZones = listOf(MuscleZone.BACK_UPPER, MuscleZone.GLUTES, MuscleZone.HAMSTRINGS),
            secondaryZones = listOf(MuscleZone.CORE, MuscleZone.FOREARMS),
            startPosition = "Füße hüftbreit, Stange über Mittelfuß, Rücken gerade.",
            movement = listOf("Stange nah am Bein hochziehen", "Hüfte und Knie gleichzeitig strecken", "Oben aufrecht stehen"),
            breathing = "Einatmen vor dem Heben, oben ausatmen.",
            commonMistakes = listOf("Runder Rücken", "Stange zu weit weg"),
            coachingTips = listOf("Boden wegdrücken", "Achseln anspannen"),
            variants = "Sumo, Rumänisch.",
            whenToStop = "Wenn der Rücken einrundet.",
            youtubeVideoId = "op9kVnSso6Q"
        ),
        "Klimmzug" to DetailedInstruction(
            exerciseId = "Klimmzug",
            primaryZones = listOf(MuscleZone.BACK_LATS, MuscleZone.BICEPS),
            secondaryZones = listOf(MuscleZone.BACK_UPPER, MuscleZone.CORE),
            startPosition = "Vordergriff, Hände etwas weiter als schulterbreit.",
            movement = listOf("Brust zur Stange ziehen", "Ellbogen nach hinten unten", "Kontrolliert ablassen"),
            breathing = "Ausatmen beim Hochziehen.",
            commonMistakes = listOf("Zappeln mit Beinen", "Kein voller Hang"),
            coachingTips = listOf("Ellbogen in die Tasche", "Schultern tief"),
            variants = "Untergrief.",
            whenToStop = "Bei Kraftverlust.",
            youtubeVideoId = "eGo4IYlbE5g"
        ),
        "Liegestütze" to DetailedInstruction(
            exerciseId = "Liegestütze",
            primaryZones = listOf(MuscleZone.CHEST, MuscleZone.TRICEPS),
            secondaryZones = listOf(MuscleZone.SHOULDERS_FRONT, MuscleZone.CORE),
            startPosition = "Hände unter Schultern, Körper gerade wie ein Brett.",
            movement = listOf("Nase zum Boden", "Ellbogen 45 Grad", "Wieder hochdrücken"),
            breathing = "Einatmen tief, Ausatmen hoch.",
            commonMistakes = listOf("Hüfte hängt", "Ellbogen zu weit außen"),
            coachingTips = listOf("Gesäß anspannen", "Boden wegschieben"),
            variants = "Auf Knien.",
            whenToStop = "Bei Instabilität.",
            youtubeVideoId = "IODxDxX7oi4"
        ),
        "Schulterpresse" to DetailedInstruction(
            exerciseId = "Schulterpresse",
            primaryZones = listOf(MuscleZone.SHOULDERS_FRONT, MuscleZone.SHOULDERS_SIDE),
            secondaryZones = listOf(MuscleZone.TRICEPS, MuscleZone.CORE),
            startPosition = "Aufrechter Sitz/Stand, Hanteln auf Schulterhöhe.",
            movement = listOf("Gerade nach oben drücken", "Nicht ganz einrasten", "Langsam absenken"),
            breathing = "Ausatmen beim Drücken.",
            commonMistakes = listOf("Hohlkreuz", "Gewicht zu weit vorne"),
            coachingTips = listOf("Bauch fest", "Handgelenke stabil"),
            variants = "Arnold Press.",
            whenToStop = "Bei Schulterstechen.",
            youtubeVideoId = "2yjwXTZQDDI"
        ),
        "Latzug" to DetailedInstruction(
            exerciseId = "Latzug",
            primaryZones = listOf(MuscleZone.BACK_LATS, MuscleZone.BACK_UPPER),
            secondaryZones = listOf(MuscleZone.BICEPS, MuscleZone.FOREARMS),
            startPosition = "Sitzend, Stange breit greifen, Brust stolz.",
            movement = listOf("Stange zur oberen Brust ziehen", "Ellbogen führen nach unten", "Langsam wieder nach oben führen"),
            breathing = "Ausatmen beim Ziehen, Einatmen beim Ablassen.",
            commonMistakes = listOf("Stange in den Nacken ziehen", "Zu starkes Zurücklehnen"),
            coachingTips = listOf("Mit den Ellbogen ziehen", "Schulterblätter unten lassen"),
            variants = "Enger Parallelgriff, Untergriff.",
            whenToStop = "Wenn die Technik leidet.",
            youtubeVideoId = "CAwf7n6Lwqo"
        ),
        "Bizeps-Curls" to DetailedInstruction(
            exerciseId = "Bizeps-Curls",
            primaryZones = listOf(MuscleZone.BICEPS),
            secondaryZones = listOf(MuscleZone.FOREARMS),
            startPosition = "Stand, Ellbogen an den Rippen.",
            movement = listOf("Unterarme beugen", "Oben kurz halten", "Vollständig strecken"),
            breathing = "Ausatmen beim Beugen.",
            commonMistakes = listOf("Schwung aus der Hüfte", "Ellbogen wandern"),
            coachingTips = listOf("Ellbogen festtackern", "Kontrolle"),
            variants = "Hammercurls.",
            whenToStop = "Bei Formverlust.",
            youtubeVideoId = "ykJmrZ5v0Oo"
        ),
        "Trizepsdrücken" to DetailedInstruction(
            exerciseId = "Trizepsdrücken",
            primaryZones = listOf(MuscleZone.TRICEPS),
            secondaryZones = listOf(MuscleZone.FOREARMS),
            startPosition = "Stand am Kabel, Ellbogen fest am Körper.",
            movement = listOf("Arme voll strecken", "Langsam zurück kommen lassen"),
            breathing = "Ausatmen beim Drücken.",
            commonMistakes = listOf("Ellbogen bewegen sich", "Oberkörper wippt"),
            coachingTips = listOf("Nur Unterarme bewegen", "Spannung halten"),
            variants = "Seil, Stange.",
            whenToStop = "Bei Ellbogenschmerz.",
            youtubeVideoId = "2-LAMcpzODU"
        ),
        "Beinpresse" to DetailedInstruction(
            exerciseId = "Beinpresse",
            primaryZones = listOf(MuscleZone.QUADRICEPS),
            secondaryZones = listOf(MuscleZone.GLUTES, MuscleZone.HAMSTRINGS),
            startPosition = "Sitzend, Füße schulterbreit.",
            movement = listOf("Plattform absenken", "Explosiv wegdrücken", "Knie nicht einrasten"),
            breathing = "Ausatmen beim Drücken.",
            commonMistakes = listOf("Knie einrasten", "Hintern hebt ab"),
            coachingTips = listOf("Ferse belasten", "Stabil bleiben"),
            variants = "Einbeinig.",
            whenToStop = "Bei Knieschmerz.",
            youtubeVideoId = "IZxyjW7MPJQ"
        ),
        "Ausfallschritte" to DetailedInstruction(
            exerciseId = "Ausfallschritte",
            primaryZones = listOf(MuscleZone.QUADRICEPS, MuscleZone.GLUTES),
            secondaryZones = listOf(MuscleZone.HAMSTRINGS, MuscleZone.CORE, MuscleZone.CALVES),
            startPosition = "Stand, Füße hüftbreit.",
            movement = listOf("Großen Schritt vor", "Hinteres Knie zum Boden", "Vorderes Knie stabil halten"),
            breathing = "Ausatmen beim Zurückdrücken.",
            commonMistakes = listOf("Vorderes Knie schiebt zu weit vor", "Oberkörper kippt"),
            coachingTips = listOf("Stabilität im Core", "Gewicht auf der Ferse"),
            variants = "Reverse Lunges.",
            whenToStop = "Bei Instabilität.",
            youtubeVideoId = "QOVaHwm-Q6U"
        ),
        "Seitheben" to DetailedInstruction(
            exerciseId = "Seitheben",
            primaryZones = listOf(MuscleZone.SHOULDERS_SIDE),
            secondaryZones = listOf(MuscleZone.SHOULDERS_FRONT, MuscleZone.BACK_UPPER),
            startPosition = "Stand, Hanteln seitlich, leichter Knick im Ellbogen.",
            movement = listOf("Arme seitlich anheben bis Schulterhöhe", "Kontrolliert absenken"),
            breathing = "Ausatmen beim Anheben.",
            commonMistakes = listOf("Zu viel Gewicht", "Nacken zieht hoch"),
            coachingTips = listOf("Wasser aus Krügen gießen", "Schultern tief"),
            variants = "Kabelzug.",
            whenToStop = "Bei Nackenschmerz.",
            youtubeVideoId = "3VcKaXpzqRo"
        ),
        "Plank" to DetailedInstruction(
            exerciseId = "Plank",
            primaryZones = listOf(MuscleZone.CORE),
            secondaryZones = listOf(MuscleZone.GLUTES, MuscleZone.SHOULDERS_FRONT),
            startPosition = "Unterarmstütz, Blick zum Boden.",
            movement = listOf("Position halten", "Maximale Ganzkörperspannung"),
            breathing = "Ruhig weiteratmen.",
            commonMistakes = listOf("Hüfte hängt", "Hintern zu hoch"),
            coachingTips = listOf("Bauchnabel einziehen", "Gesäß fest"),
            variants = "Side Plank.",
            whenToStop = "Wenn die Hüfte absinkt.",
            youtubeVideoId = "ASdvN_XEl_c"
        ),
        "Crunch" to DetailedInstruction(
            exerciseId = "Crunch",
            primaryZones = listOf(MuscleZone.CORE),
            secondaryZones = listOf(),
            startPosition = "Rückenlage, Beine angewinkelt.",
            movement = listOf("Oberen Rücken leicht anheben", "Bauch fest anspannen", "Langsam abrollen"),
            breathing = "Ausatmen beim Hochkommen.",
            commonMistakes = listOf("Am Nacken ziehen", "Zu hohe Bewegung"),
            coachingTips = listOf("Blick zur Decke", "Kurze Bewegung"),
            variants = "Bicycle.",
            whenToStop = "Bei Nackenschmerzen.",
            youtubeVideoId = "Xyd_fa5zoEU"
        ),
        "Glute Bridge" to DetailedInstruction(
            exerciseId = "Glute Bridge",
            primaryZones = listOf(MuscleZone.GLUTES),
            secondaryZones = listOf(MuscleZone.HAMSTRINGS, MuscleZone.CORE),
            startPosition = "Rückenlage, Füße nah am Gesäß.",
            movement = listOf("Hüfte zur Decke drücken", "Oben Gesäß fest anspannen", "Langsam absenken"),
            breathing = "Ausatmen beim Hochdrücken.",
            commonMistakes = listOf("Hohlkreuz am höchsten Punkt", "Füße zu weit weg"),
            coachingTips = listOf("Durch die Fersen drücken", "Bauch anspannen"),
            variants = "Einbeinig.",
            whenToStop = "Bei Krämpfen.",
            youtubeVideoId = "wPM8icPu6H8"
        ),
        "Burpees" to DetailedInstruction(
            exerciseId = "Burpees",
            primaryZones = listOf(MuscleZone.QUADRICEPS, MuscleZone.CHEST, MuscleZone.CORE),
            secondaryZones = listOf(MuscleZone.CALVES, MuscleZone.SHOULDERS_FRONT),
            startPosition = "Aufrechter Stand.",
            movement = listOf("In die Hocke", "Beine nach hinten springen", "Liegestütz", "Nach vorne springen", "Strecksprung"),
            breathing = "Rhythmisch atmen.",
            commonMistakes = listOf("Keine Ganzkörperspannung", "Hüfte schlägt auf"),
            coachingTips = listOf("Flüssige Bewegung", "Sicher landen"),
            variants = "Ohne Sprung.",
            whenToStop = "Bei Schwindel.",
            youtubeVideoId = "TU8QYVW0gDU"
        ),
        "Butterfly" to DetailedInstruction(
            exerciseId = "Butterfly",
            primaryZones = listOf(MuscleZone.CHEST),
            secondaryZones = listOf(MuscleZone.SHOULDERS_FRONT),
            startPosition = "Sitzend, Rücken am Polster.",
            movement = listOf("Arme vor der Brust zusammenführen", "Brust aktiv anspannen", "Langsam wieder öffnen"),
            breathing = "Ausatmen beim Zusammenführen.",
            commonMistakes = listOf("Schultern ziehen vor", "Arme zu stark gebeugt"),
            coachingTips = listOf("Umarme einen Baum", "Brust stolz"),
            variants = "Flys.",
            whenToStop = "Bei Schulterschmerz.",
            youtubeVideoId = "Z6wy1vp0jQQ"
        ),
        "Beinstrecker" to DetailedInstruction(
            exerciseId = "Beinstrecker",
            primaryZones = listOf(MuscleZone.QUADRICEPS),
            secondaryZones = listOf(),
            startPosition = "Sitzend, Rolle auf Schienbein.",
            movement = listOf("Beine strecken", "Oben kurz halten", "Langsam absenksn"),
            breathing = "Ausatmen beim Strecken.",
            commonMistakes = listOf("Hintern hebt ab", "Schwung"),
            coachingTips = listOf("Festhalten an Griffen", "Kontrolliert"),
            variants = "Einbeinig.",
            whenToStop = "Bei Knieschmerz.",
            youtubeVideoId = "YyvSfVjQeL0"
        ),
        "Beinbeuger" to DetailedInstruction(
            exerciseId = "Beinbeuger",
            primaryZones = listOf(MuscleZone.HAMSTRINGS),
            secondaryZones = listOf(MuscleZone.CALVES),
            startPosition = "Liegend oder sitzend, Rolle an Ferse.",
            movement = listOf("Fersen zum Gesäß ziehen", "Oben halten", "Langsam zurück"),
            breathing = "Ausatmen beim Beugen.",
            commonMistakes = listOf("Hüfte hebt ab", "Ruckartig"),
            coachingTips = listOf("Hüfte ins Polster drücken", "Volle Dehnung"),
            variants = "Einbeinig.",
            whenToStop = "Bei Krämpfen.",
            youtubeVideoId = "1Tq3QdYUuHs"
        ),
        "Wadenheben" to DetailedInstruction(
            exerciseId = "Wadenheben",
            primaryZones = listOf(MuscleZone.CALVES),
            secondaryZones = listOf(),
            startPosition = "Stand auf Kante.",
            movement = listOf("Ferse tief absenken", "Explosiv hoch auf Zehenspitzen", "Oben halten"),
            breathing = "Ausatmen beim Hochdrücken.",
            commonMistakes = listOf("Zu schnell", "Keine Dehnung"),
            coachingTips = listOf("Kraft aus dem Ballen", "Voller Weg"),
            variants = "Sitzend.",
            whenToStop = "Bei Krämpfen.",
            youtubeVideoId = "gwLzBJYoWlI"
        ),
        "Thruster" to DetailedInstruction(
            exerciseId = "Thruster",
            primaryZones = listOf(MuscleZone.QUADRICEPS, MuscleZone.SHOULDERS_FRONT, MuscleZone.GLUTES),
            secondaryZones = listOf(MuscleZone.TRICEPS, MuscleZone.CORE),
            startPosition = "Stand, Hanteln auf Schultern.",
            movement = listOf("Tiefe Kniebeuge", "Explosiv aufstehen", "Druck über Kopf"),
            breathing = "Ausatmen beim Drücken.",
            commonMistakes = listOf("Keine flüssige Bewegung", "Kippen nach vorne"),
            coachingTips = listOf("Schwung aus Beinen", "Oben stabil"),
            variants = "Wall Balls.",
            whenToStop = "Bei Formverlust.",
            youtubeVideoId = "L219ltL15zk"
        ),
        "Mountain Climbers" to DetailedInstruction(
            exerciseId = "Mountain Climbers",
            primaryZones = listOf(MuscleZone.CORE, MuscleZone.SHOULDERS_FRONT),
            secondaryZones = listOf(MuscleZone.QUADRICEPS),
            startPosition = "Liegestütz-Position.",
            movement = listOf("Knie explosiv zur Brust ziehen", "Hüfte tief halten", "Flüssiger Wechsel"),
            breathing = "Rhythmisch.",
            commonMistakes = listOf("Hüfte wippt", "Gewicht zu weit hinten"),
            coachingTips = listOf("Core stabil", "Tempo"),
            variants = "Slow Climbers.",
            whenToStop = "Bei Kraftverlust.",
            youtubeVideoId = "nmwgirgXLYM"
        )
    )

    fun get(exerciseId: String): DetailedInstruction? {
        val cleanId = exerciseId.lowercase()
            .replace(" (", " ")
            .replace(")", "")
            .replace("lh", "")
            .replace("kh", "")
            .replace("hantel", "")
            .replace("gewicht", "")
            .replace("maschine", "")
            .replace("-", " ")
            .trim()
        
        return data.entries.find { cleanId.contains(it.key.lowercase()) }?.value
            ?: data.entries.find { it.key.lowercase().contains(cleanId) }?.value
    }
}
