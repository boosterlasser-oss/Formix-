package com.fantasyfoodplanner.data

object DefaultRecipes {
    val items = listOf(
        // --- MUSKELAUFBAU (Build) ---
        Recipe(
            name = "Drachen-Steak mit Süßkartoffel-Gold", 
            category = "build",
            ingredients = "300g Rinderhüftsteak, 500g Süßkartoffeln, Rosmarin, Knoblauch, 1 EL Olivenöl",
            kcal = 850, protein = 65.0, carbs = 75.0, fat = 28.0,
            instructions = "Süßkartoffeln würfeln und 20 Min. kochen. Steak in heißer Pfanne mit Rosmarin und Knoblauch von jeder Seite 3-4 Min. braten. Kartoffeln zu Püree stampfen und Steak darauf anrichten."
        ),
        Recipe(
            name = "Lachs-Quinoa des Weisen", 
            category = "build",
            ingredients = "200g Lachsfilet, 100g Quinoa, 1 Zucchini, 1 Paprika, Zitronensaft",
            kcal = 720, protein = 48.0, carbs = 62.0, fat = 32.0,
            instructions = "Quinoa waschen und nach Anleitung kochen. Gemüse würfeln und in der Pfanne dünsten. Lachs auf der Hautseite knusprig braten. Alles vermengen und mit Zitrone abschmecken."
        ),
        Recipe(
            name = "Hähnchen-Reis-Pfanne des Kriegers",
            category = "build",
            ingredients = "200g Hähnchenbrust, 150g Reis, 200g Brokkoli, Sojasauce, Ingwer",
            kcal = 790, protein = 58.0, carbs = 95.0, fat = 12.0,
            instructions = "Reis kochen. Brokkoli dämpfen. Hähnchen würfeln, scharf anbraten, Ingwer dazu. Reis und Brokkoli unterheben und mit Sojasauce ablöschen."
        ),
        Recipe(
            name = "Titanen-Pasta mit Rinderhack",
            category = "build",
            ingredients = "150g Vollkornpasta, 200g Rinderhack (mager), Tomatensauce, Zwiebeln, Basilikum",
            kcal = 840, protein = 52.0, carbs = 98.0, fat = 24.0,
            instructions = "Pasta kochen. Hackfleisch mit Zwiebeln anbraten, Tomatensauce dazu und 10 Min. köcheln. Pasta mit der Sauce mischen."
        ),
        Recipe(
            name = "Oger-Omelett mit Speck",
            category = "build",
            ingredients = "5 Eier, 50g Speck, 30g Käse, eine Handvoll Spinat",
            kcal = 650, protein = 45.0, carbs = 8.0, fat = 48.0,
            instructions = "Eier verquirlen. Speck anbraten, Spinat dazu. Eiermasse in die Pfanne geben, Käse darüberstreuen und stocken lassen."
        ),
        Recipe(
            name = "Ritter-Gulasch mit Kartoffeln",
            category = "build",
            ingredients = "250g Rindfleisch, 300g Kartoffeln, Paprika, Zwiebeln, Rinderbrühe",
            kcal = 780, protein = 55.0, carbs = 65.0, fat = 30.0,
            instructions = "Fleisch und Zwiebeln anbraten. Kartoffeln und Paprika würfeln, dazu geben. Mit Brühe aufgießen und 60 Min. schmoren."
        ),
        Recipe(
            name = "Thunfisch-Festmahl des Kapitäns",
            category = "build",
            ingredients = "1 Dose Thunfisch, 100g Nudeln, 50g Mais, 1 EL Pesto Rosso",
            kcal = 690, protein = 42.0, carbs = 88.0, fat = 16.0,
            instructions = "Nudeln kochen. Thunfisch und Mais unter die heißen Nudeln rühren, mit Pesto verfeinern."
        ),

        // --- ABNEHMEN (Lose) ---
        Recipe(
            name = "Leichter Elfen-Salat", 
            category = "lose",
            ingredients = "150g Hähnchen, 200g Salat, Cherrytomaten, Gurke, Essig/Öl",
            kcal = 380, protein = 38.0, carbs = 12.0, fat = 18.0,
            instructions = "Hähnchen braten. Salat und Gemüse schneiden. Alles mischen und mit Dressing beträufeln."
        ),
        Recipe(
            name = "Beeren-Zauber mit Magerquark", 
            category = "lose",
            ingredients = "250g Magerquark, 150g Beeren, 1 EL Leinsamen",
            kcal = 310, protein = 32.0, carbs = 28.0, fat = 6.0,
            instructions = "Quark glatt rühren, Beeren und Leinsamen unterheben. Kalt genießen."
        ),
        Recipe(
            name = "Zoodles mit Garnelen-Sturm",
            category = "lose",
            ingredients = "2 Zucchini, 150g Garnelen, Knoblauch, Chili, Zitrone",
            kcal = 340, protein = 36.0, carbs = 15.0, fat = 14.0,
            instructions = "Zucchini zu Nudeln drehen. Garnelen mit Knoblauch braten. Zoodles 2 Min. mitgaren, mit Zitrone würzen."
        ),
        Recipe(
            name = "Waldläufer-Gemüsesuppe",
            category = "lose",
            ingredients = "Karotten, Lauch, Sellerie, 1 Kartoffel, Kräuter",
            kcal = 260, protein = 12.0, carbs = 40.0, fat = 2.0,
            instructions = "Gemüse würfeln und in 1L Brühe 20 Min. kochen. Mit frischen Kräutern servieren."
        ),
        Recipe(
            name = "See-Forelle mit Zitrone",
            category = "lose",
            ingredients = "1 Forelle, 1 Zitrone, Rosmarinzweig, Alufolie",
            kcal = 390, protein = 44.0, carbs = 2.0, fat = 22.0,
            instructions = "Fisch mit Zitronenscheiben füllen, in Folie wickeln und 25 Min. bei 200 Grad im Ofen garen."
        ),
        Recipe(
            name = "Phönix-Hähnchen (Scharf)",
            category = "lose",
            ingredients = "200g Hähnchenbrust, Cayennepfeffer, Paprikapulver, 200g Bohnen",
            kcal = 350, protein = 50.0, carbs = 10.0, fat = 12.0,
            instructions = "Hähnchen scharf würzen und braten. Bohnen in Salzwasser garen und als Beilage servieren."
        ),

        // --- SNACKS (Energie-Quests) ---
        Recipe(
            name = "Hüttenkäse-Brot der Kraft",
            category = "Snack",
            ingredients = "200g Hüttenkäse, 1 Scheibe Vollkornbrot, Schnittlauch",
            kcal = 250, protein = 24.0, carbs = 20.0, fat = 8.0,
            instructions = "Brot rösten, Hüttenkäse darauf verteilen und mit reichlich Schnittlauch bestreuen."
        ),
        Recipe(
            name = "Götter-Joghurt mit Nüssen",
            category = "Snack",
            ingredients = "150g Griechischer Joghurt, 20g Walnüsse, 1 TL Honig",
            kcal = 420, protein = 18.0, carbs = 30.0, fat = 25.0,
            instructions = "Nüsse hacken, über den Joghurt geben und Honig darüberträufeln."
        ),
        Recipe(
            name = "Protein-Trank (Schoko)",
            category = "Snack",
            ingredients = "30g Whey Protein, 300ml Wasser oder fettarme Milch",
            kcal = 180, protein = 35.0, carbs = 5.0, fat = 2.0,
            instructions = "Alles im Shaker 30 Sekunden kräftig schütteln. Ideal nach dem Training."
        ),
        Recipe(
            name = "Abenteurer-Mix (Nüsse)",
            category = "Snack",
            ingredients = "20g Mandeln, 20g Cashews, 10g Rosinen",
            kcal = 310, protein = 9.0, carbs = 18.0, fat = 22.0,
            instructions = "Alle Zutaten mischen. Perfekt für die Quest zwischendurch."
        ),

        // --- VEGETARISCH (Natur-Magie) ---
        Recipe(
            name = "Kichererbsen-Curry des Fakirs",
            category = "build",
            ingredients = "1 Dose Kichererbsen, Kokosmilch, Curry, 100g Reis",
            kcal = 740, protein = 24.0, carbs = 105.0, fat = 20.0,
            instructions = "Kichererbsen in Kokosmilch und Gewürzen kochen. Parallel Reis garen und dazu servieren."
        ),
        Recipe(
            name = "Tofu-Pfanne 'Grüner Wald'",
            category = "lose",
            ingredients = "200g Tofu, 300g Brokkoli, Sojasauce, Sesam",
            kcal = 380, protein = 32.0, carbs = 15.0, fat = 22.0,
            instructions = "Tofu würfeln und kross braten. Brokkoli kurz mitgaren, mit Sojasauce ablöschen und Sesam bestreuen."
        ),
        Recipe(
            name = "Linsen-Eintopf der Zwerge",
            category = "build",
            ingredients = "150g Rote Linsen, Karotten, Kartoffeln, Zwiebeln",
            kcal = 620, protein = 35.0, carbs = 90.0, fat = 8.0,
            instructions = "Zwiebeln und Gemüse anbraten. Linsen und Wasser dazu, 15-20 Min. köcheln bis die Linsen weich sind."
        )
    )
}

object DefaultProducts { 
    val items = listOf(
        Product(name="Apfel", kcal=52, protein=0.3, carbs=14.0, fat=0.2),
        Product(name="Banane", kcal=89, protein=1.1, carbs=23.0, fat=0.3),
        Product(name="Magerquark", kcal=68, protein=12.0, carbs=4.0, fat=0.2),
        Product(name="Hüttenkäse", kcal=102, protein=12.5, carbs=2.7, fat=4.5),
        Product(name="Vollkornbrot", kcal=247, protein=8.0, carbs=46.0, fat=3.0),
        Product(name="Mandeln", kcal=579, protein=21.0, carbs=22.0, fat=49.0),
        Product(name="Eier (Stück)", kcal=85, protein=7.0, carbs=0.6, fat=6.0),
        Product(name="Putenbrust", kcal=111, protein=24.0, carbs=0.0, fat=1.0)
    )
}
