package com.fantasyfoodplanner.data
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Seeder {
  suspend fun seedAll(db: AppDb){
    withContext(Dispatchers.IO){
      // FIX: Wir löschen NICHTS mehr. Wir nutzen Upsert (Update or Insert).
      // Dadurch bleiben bestehende IDs erhalten und der Tagesplaner wird nicht geleert.
      db.recipeDao().insertAll(DefaultRecipes.items)
      db.productDao().insertAll(DefaultProducts.items)
    }
  }
}
