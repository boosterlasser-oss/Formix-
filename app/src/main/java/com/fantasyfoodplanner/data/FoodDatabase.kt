package com.fantasyfoodplanner.data

data class FoodItem(
    val name: String,
    val category: String,
    val caloriesPer100g: Double,
    val carbsPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double,
    val vitaminC: Double,
    val potassium: Double,
    val magnesium: Double,
    val iron: Double,
    val performanceTags: List<String> = emptyList()
) {
    companion object {
        fun create(
            name: String,
            category: String,
            caloriesPer100g: Double,
            carbsPer100g: Double,
            proteinPer100g: Double,
            fatPer100g: Double,
            fiberPer100g: Double,
            vitaminC: Double,
            potassium: Double,
            magnesium: Double,
            iron: Double
        ): FoodItem {
            val tags = PerformanceTagEngine.computeTags(
                kcal = caloriesPer100g,
                protein = proteinPer100g,
                carbs = carbsPer100g,
                fat = fatPer100g,
                vitaminC = vitaminC,
                potassium = potassium,
                magnesium = magnesium,
                iron = iron
            )
            
            return FoodItem(
                name, category, caloriesPer100g, carbsPer100g, proteinPer100g, fatPer100g, fiberPer100g,
                vitaminC, potassium, magnesium, iron, tags
            )
        }
    }
}

val foodDatabase: List<FoodItem> = listOf(
    // --- OBST ---
    FoodItem.create("Mandarine", "Obst", 53.0, 13.0, 0.8, 0.3, 1.8, 26.7, 166.0, 12.0, 0.2),
    FoodItem.create("Banane", "Obst", 89.0, 23.0, 1.1, 0.3, 2.6, 8.7, 358.0, 27.0, 0.3),
    FoodItem.create("Apfel (rot)", "Obst", 52.0, 14.0, 0.3, 0.2, 2.4, 4.6, 107.0, 5.0, 0.1),
    FoodItem.create("Apfel (grün)", "Obst", 50.0, 13.0, 0.3, 0.2, 2.5, 5.0, 110.0, 6.0, 0.1),
    FoodItem.create("Erdbeeren", "Obst", 32.0, 7.7, 0.7, 0.3, 2.0, 58.8, 153.0, 13.0, 0.4),
    FoodItem.create("Kiwi", "Obst", 61.0, 15.0, 1.1, 0.5, 3.0, 92.7, 312.0, 17.0, 0.3),
    FoodItem.create("Ananas", "Obst", 50.0, 13.0, 0.5, 0.1, 1.4, 47.8, 109.0, 12.0, 0.3),
    FoodItem.create("Heidelbeeren", "Obst", 57.0, 14.0, 0.7, 0.3, 2.4, 9.7, 77.0, 6.0, 0.3),
    FoodItem.create("Himbeeren", "Obst", 52.0, 12.0, 1.2, 0.7, 6.5, 26.0, 151.0, 22.0, 0.7),
    FoodItem.create("Brombeeren", "Obst", 43.0, 10.0, 1.4, 0.5, 5.0, 21.0, 162.0, 20.0, 0.6),
    FoodItem.create("Weintrauben", "Obst", 69.0, 18.0, 0.7, 0.2, 0.9, 3.2, 191.0, 7.0, 0.4),
    FoodItem.create("Kirschen", "Obst", 63.0, 16.0, 1.1, 0.2, 2.1, 7.0, 222.0, 11.0, 0.4),
    FoodItem.create("Mango", "Obst", 60.0, 15.0, 0.8, 0.4, 1.6, 36.4, 168.0, 10.0, 0.2),
    FoodItem.create("Birne", "Obst", 57.0, 15.0, 0.4, 0.1, 3.1, 4.3, 116.0, 7.0, 0.2),
    FoodItem.create("Wassermelone", "Obst", 30.0, 8.0, 0.6, 0.2, 0.4, 8.1, 112.0, 10.0, 0.2),
    FoodItem.create("Zitrone", "Obst", 29.0, 9.0, 1.1, 0.3, 2.8, 53.0, 138.0, 8.0, 0.6),
    FoodItem.create("Limette", "Obst", 30.0, 11.0, 0.7, 0.2, 2.8, 29.0, 102.0, 6.0, 0.6),
    FoodItem.create("Datteln", "Obst", 282.0, 75.0, 2.5, 0.4, 8.0, 0.0, 696.0, 54.0, 1.0),
    FoodItem.create("Orange", "Obst", 47.0, 12.0, 0.9, 0.1, 2.4, 53.2, 181.0, 10.0, 0.1),
    FoodItem.create("Pfirsich", "Obst", 39.0, 10.0, 0.9, 0.3, 1.5, 6.6, 190.0, 9.0, 0.3),
    FoodItem.create("Nektarine", "Obst", 44.0, 11.0, 1.1, 0.3, 1.7, 5.4, 201.0, 9.0, 0.3),
    FoodItem.create("Granatapfel", "Obst", 83.0, 19.0, 1.7, 1.2, 4.0, 10.2, 236.0, 12.0, 0.3),
    FoodItem.create("Avocado", "Obst", 160.0, 9.0, 2.0, 15.0, 7.0, 10.0, 485.0, 29.0, 0.6),

    // --- GEMÜSE ---
    FoodItem.create("Brokkoli", "Gemüse", 34.0, 7.0, 2.8, 0.4, 2.6, 89.0, 316.0, 21.0, 0.7),
    FoodItem.create("Blumenkohl", "Gemüse", 25.0, 5.0, 1.9, 0.3, 2.0, 48.0, 299.0, 15.0, 0.4),
    FoodItem.create("Karotte", "Gemüse", 41.0, 10.0, 0.9, 0.2, 2.8, 5.9, 320.0, 12.0, 0.3),
    FoodItem.create("Spinat", "Gemüse", 23.0, 3.6, 2.9, 0.4, 2.2, 28.0, 558.0, 79.0, 2.7),
    FoodItem.create("Kartoffel", "Gemüse", 77.0, 17.0, 2.0, 0.1, 2.2, 19.0, 421.0, 23.0, 0.8),
    FoodItem.create("Süßkartoffel", "Gemüse", 86.0, 20.0, 1.6, 0.1, 3.0, 2.4, 337.0, 25.0, 0.6),
    FoodItem.create("Paprika (rot)", "Gemüse", 31.0, 6.0, 1.0, 0.3, 2.1, 127.0, 211.0, 12.0, 0.4),
    FoodItem.create("Tomate", "Gemüse", 18.0, 3.9, 0.9, 0.2, 1.2, 13.7, 237.0, 11.0, 0.3),
    FoodItem.create("Gurke", "Gemüse", 15.0, 3.6, 0.7, 0.1, 0.5, 2.8, 147.0, 13.0, 0.3),
    FoodItem.create("Zucchini", "Gemüse", 17.0, 3.1, 1.2, 0.3, 1.0, 17.9, 261.0, 18.0, 0.4),
    FoodItem.create("Aubergine", "Gemüse", 25.0, 6.0, 1.0, 0.2, 3.0, 2.2, 229.0, 14.0, 0.2),
    FoodItem.create("Champignons", "Gemüse", 22.0, 3.3, 3.1, 0.3, 1.0, 2.1, 318.0, 9.0, 0.5),
    FoodItem.create("Zwiebel", "Gemüse", 40.0, 9.0, 1.1, 0.1, 1.7, 7.4, 146.0, 10.0, 0.2),
    FoodItem.create("Knoblauch", "Gemüse", 149.0, 33.0, 6.4, 0.5, 2.1, 31.0, 401.0, 25.0, 1.7),
    FoodItem.create("Lauch", "Gemüse", 61.0, 14.0, 1.5, 0.3, 1.8, 12.0, 180.0, 28.0, 2.1),
    FoodItem.create("Kürbis (Hokkaido)", "Gemüse", 63.0, 12.0, 1.7, 0.5, 2.5, 12.0, 350.0, 20.0, 0.8),
    FoodItem.create("Eisbergsalat", "Gemüse", 14.0, 3.0, 0.9, 0.1, 1.2, 2.8, 141.0, 7.0, 0.4),
    FoodItem.create("Romanasalat", "Gemüse", 17.0, 3.3, 1.2, 0.3, 2.1, 4.0, 247.0, 14.0, 1.0),
    FoodItem.create("Feldsalat", "Gemüse", 14.0, 0.7, 2.0, 0.4, 1.5, 35.0, 421.0, 13.0, 2.0),
    FoodItem.create("Rucola", "Gemüse", 25.0, 3.7, 2.6, 0.7, 1.6, 15.0, 369.0, 47.0, 1.5),

    // --- PROTEIN ---
    FoodItem.create("Magerquark", "Protein", 68.0, 4.0, 12.0, 0.2, 0.0, 0.0, 120.0, 12.0, 0.1),
    FoodItem.create("Skyr (natur)", "Protein", 63.0, 4.0, 11.0, 0.2, 0.0, 0.0, 110.0, 10.0, 0.1),
    FoodItem.create("Hüttenkäse (light)", "Protein", 70.0, 2.5, 12.0, 1.0, 0.0, 0.0, 80.0, 8.0, 0.1),
    FoodItem.create("Naturjoghurt (0.1%)", "Protein", 40.0, 5.0, 4.5, 0.1, 0.0, 1.0, 150.0, 12.0, 0.1),
    FoodItem.create("Hähnchenbrust", "Protein", 110.0, 0.0, 23.0, 1.5, 0.0, 0.0, 256.0, 28.0, 0.7),
    FoodItem.create("Putenbrust", "Protein", 105.0, 0.0, 24.0, 1.0, 0.0, 0.0, 260.0, 30.0, 0.8),
    FoodItem.create("Rinderhüfte (mager)", "Protein", 120.0, 0.0, 22.0, 3.0, 0.0, 0.0, 340.0, 25.0, 2.5),
    FoodItem.create("Thunfisch (Eigensaft)", "Protein", 116.0, 0.0, 26.0, 1.0, 0.0, 0.0, 323.0, 27.0, 1.1),
    FoodItem.create("Lachsfilet", "Protein", 208.0, 0.0, 20.0, 13.0, 0.0, 0.0, 363.0, 27.0, 0.3),
    FoodItem.create("Ei (mittel)", "Protein", 155.0, 1.1, 13.0, 11.0, 0.0, 0.0, 126.0, 10.0, 1.2),
    FoodItem.create("Eiklar", "Protein", 50.0, 0.7, 11.0, 0.2, 0.0, 0.0, 160.0, 11.0, 0.1),
    FoodItem.create("Whey Protein (Pulver)", "Protein", 370.0, 5.0, 80.0, 2.0, 0.0, 0.0, 100.0, 50.0, 0.5),
    FoodItem.create("Tofu (natur)", "Protein", 76.0, 1.9, 8.1, 4.8, 0.3, 0.1, 121.0, 37.0, 5.4),
    FoodItem.create("Linsen (gekocht)", "Protein", 116.0, 20.0, 9.0, 0.4, 8.0, 1.5, 369.0, 36.0, 3.3),
    FoodItem.create("Kichererbsen (Gals)", "Protein", 164.0, 27.0, 8.9, 2.6, 7.6, 4.0, 291.0, 48.0, 2.9),
    FoodItem.create("Kidneybohnen", "Protein", 127.0, 22.8, 8.7, 0.5, 7.4, 1.2, 405.0, 45.0, 2.2),
    FoodItem.create("Edamame (gekocht)", "Protein", 122.0, 10.0, 11.0, 5.0, 5.0, 6.0, 435.0, 64.0, 2.3),

    // --- KOHLENHYDRATE ---
    FoodItem.create("Reis (Basmati)", "Kohlenhydrate", 350.0, 77.0, 7.5, 0.6, 1.3, 0.0, 115.0, 25.0, 0.5),
    FoodItem.create("Reis (Vollkorn)", "Kohlenhydrate", 345.0, 72.0, 7.8, 2.2, 3.5, 0.0, 230.0, 120.0, 1.5),
    FoodItem.create("Nudeln (Hartweizen)", "Kohlenhydrate", 360.0, 72.0, 12.0, 1.5, 3.0, 0.0, 170.0, 45.0, 1.2),
    FoodItem.create("Nudeln (Vollkorn)", "Kohlenhydrate", 340.0, 65.0, 13.0, 2.5, 7.0, 0.0, 250.0, 130.0, 3.5),
    FoodItem.create("Haferflocken (zart)", "Kohlenhydrate", 368.0, 58.7, 13.5, 7.0, 10.0, 0.0, 350.0, 130.0, 4.5),
    FoodItem.create("Couscous (trocken)", "Kohlenhydrate", 355.0, 72.0, 12.0, 0.6, 5.0, 0.0, 160.0, 40.0, 1.1),
    FoodItem.create("Bulgur", "Kohlenhydrate", 342.0, 63.0, 12.0, 1.3, 12.0, 0.0, 410.0, 160.0, 2.5),
    FoodItem.create("Vollkornbrot", "Kohlenhydrate", 247.0, 41.0, 13.0, 3.4, 6.0, 0.0, 250.0, 75.0, 3.3),
    FoodItem.create("Vollkorntoast", "Kohlenhydrate", 255.0, 45.0, 9.0, 3.5, 5.0, 0.0, 180.0, 40.0, 1.5),
    FoodItem.create("Tortilla Wrap (Vollkorn)", "Kohlenhydrate", 310.0, 50.0, 9.0, 7.0, 4.0, 0.0, 150.0, 30.0, 1.2),
    FoodItem.create("Reiswaffel (natur)", "Kohlenhydrate", 380.0, 80.0, 8.0, 3.0, 3.5, 0.0, 200.0, 100.0, 1.5),
    FoodItem.create("Knäckebrot", "Kohlenhydrate", 330.0, 60.0, 10.0, 2.0, 15.0, 0.0, 400.0, 120.0, 3.0),

    // --- SNACKS ---
    FoodItem.create("Proteinriegel", "Snacks", 380.0, 35.0, 30.0, 12.0, 2.0, 0.0, 100.0, 30.0, 1.0),
    FoodItem.create("Beef Jerky", "Snacks", 310.0, 5.0, 60.0, 5.0, 0.0, 0.0, 500.0, 40.0, 4.0),
    FoodItem.create("Mandeln (natur)", "Snacks", 579.0, 22.0, 21.0, 49.0, 12.0, 0.0, 733.0, 270.0, 3.7),
    FoodItem.create("Walnüsse", "Snacks", 654.0, 14.0, 15.0, 65.0, 7.0, 1.3, 441.0, 158.0, 2.9),
    FoodItem.create("Erdnussmus (100%)", "Snacks", 590.0, 12.0, 26.0, 49.0, 8.0, 0.0, 650.0, 160.0, 2.0),
    FoodItem.create("Chiasamen", "Snacks", 486.0, 42.0, 17.0, 31.0, 34.0, 1.6, 407.0, 335.0, 7.7),
    FoodItem.create("Leinsamen", "Snacks", 534.0, 29.0, 18.0, 42.0, 27.0, 0.6, 813.0, 392.0, 5.7),
    FoodItem.create("Olivenöl", "Snacks", 884.0, 0.0, 0.0, 100.0, 0.0, 0.0, 1.0, 0.0, 0.1),
    FoodItem.create("Isodrink", "Snacks", 25.0, 6.0, 0.0, 0.0, 0.0, 10.0, 50.0, 20.0, 0.1)
)
