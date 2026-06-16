package com.fantasyfoodplanner.features

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fantasyfoodplanner.data.AppDb
import com.fantasyfoodplanner.data.Product
import com.fantasyfoodplanner.ui.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ProductsScreen(onBack: () -> Unit, onPick: (Product) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDb.get(ctx) }
    var products by remember { mutableStateOf(emptyList<Product>()) }
    var q by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.productDao().getAll().collectLatest { products = it }
    }

    FantasySurface {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            MainAppBar("Produktdatenbank", onBack = onBack)
            Spacer(Modifier.height(8.dp))
            FantasyTextField(q, { q = it }, "Suchen...", Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            val filtered = products.filter { it.name.contains(q, ignoreCase = true) }

            LazyColumn(Modifier.weight(1f)) {
                items(filtered) { p ->
                    FantasyCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row {
                            Column(Modifier.weight(1f)) {
                                FText(p.name, bold = true)
                                FText("${p.kcal} kcal / 100g", sizeSp = 14)
                            }
                            FantasyButton("Auswählen") { onPick(p) }
                        }
                    }
                }
            }
        }
    }
}
