package com.fantasyfoodplanner.features

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.fantasyfoodplanner.data.ScannedFoodEntity
import com.fantasyfoodplanner.data.repository.FoodScanRepository
import com.fantasyfoodplanner.logic.OcrEngine
import com.fantasyfoodplanner.ui.FantasyButton
import com.fantasyfoodplanner.ui.FantasyColors
import com.fantasyfoodplanner.ui.FantasySurface
import com.fantasyfoodplanner.ui.FText
import com.fantasyfoodplanner.ui.UpgradeDialog
import com.fantasyfoodplanner.logic.SubscriptionManager
import com.fantasyfoodplanner.logic.SubscriptionTier
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * LifecycleOwner robust finden – auch in Dialog-Contexts.
 */
private fun findLifecycleOwner(context: android.content.Context): androidx.lifecycle.LifecycleOwner? {
    var ctx: android.content.Context? = context
    while (ctx != null) {
        if (ctx is androidx.lifecycle.LifecycleOwner) return ctx
        ctx = if (ctx is android.content.ContextWrapper) ctx.baseContext else null
    }
    return null
}

/**
 * LIVE SCANNER – Kamera-Barcode-Scanner.
 *
 * Nutzt ML Kit Barcode + Open Food Facts API.
 */
@Composable
fun LiveScannerScreen(
    onResult: (OcrEngine.ScanResult) -> Unit,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current

    // Feature-Gate: Barcode-Scanner ist Premium-Feature
    if (!SubscriptionManager.hasBarcodeScanner(ctx)) {
        UpgradeDialog(SubscriptionTier.PREMIUM, onDismiss = onDismiss)
        return
    }

    val green = Color(0xFF00FF7F)
    val scope = rememberCoroutineScope()
    val foodRepo = remember { FoodScanRepository(ctx) }

    // ── Permission ──
    var hasCamPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamPerm = it }
    LaunchedEffect(Unit) { if (!hasCamPerm) permLauncher.launch(Manifest.permission.CAMERA) }

    // ── Shared State ──
    var liveResult by remember { mutableStateOf<OcrEngine.ScanResult?>(null) }
    var isStableState by remember { mutableStateOf(false) }
    var isConfirmed by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Richte die Kamera auf den Barcode…") }

    // ── Kamera-Hinweis (einmalig beim Öffnen) ──
    var showCameraHint by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(4000L)
        showCameraHint = false
    }

    // ── Barcode State ──
    var barcodeResult by remember { mutableStateOf<ScannedFoodEntity?>(null) }

    // ── Scan-Linie Animation ──
    val transition = rememberInfiniteTransition(label = "scan")
    val scanY by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "scanY"
    )

    // ── Keine Berechtigung ──
    if (!hasCamPerm) {
        FantasySurface {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FText("📷 Kamerazugriff benötigt", sizeSp = 20, bold = true, color = FantasyColors.Accent)
                    Spacer(Modifier.height(16.dp))
                    FantasyButton("Berechtigung erteilen") { permLauncher.launch(Manifest.permission.CAMERA) }
                    Spacer(Modifier.height(8.dp))
                    FantasyButton("Abbrechen", alpha = 0.6f) { onDismiss() }
                }
            }
        }
        return
    }

    // ── LifecycleOwner ──
    val lifecycleOwner = remember(ctx) { findLifecycleOwner(ctx) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        // ═══════ KAMERA ═══════
        if (lifecycleOwner != null) {
            AndroidView(
                factory = { context ->
                    val previewView = androidx.camera.view.PreviewView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        implementationMode = androidx.camera.view.PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
                    }
                    val activityCtx = findLifecycleOwner(context) as? android.content.Context ?: context
                    val future = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(activityCtx)
                    future.addListener({
                        try {
                            val provider = future.get()
                            val preview = androidx.camera.core.Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val barcodeOptions = BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(
                                    Barcode.FORMAT_QR_CODE,
                                    Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8,
                                    Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
                                    Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39
                                ).build()
                            val barcodeScanner = BarcodeScanning.getClient(barcodeOptions)
                            val executor = Executors.newSingleThreadExecutor()

                            val analysis = androidx.camera.core.ImageAnalysis.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            analysis.setAnalyzer(executor) analyzer@{ proxy ->
                                if (isConfirmed) { proxy.close(); return@analyzer }
                                @Suppress("UnsafeOptInUsageError")
                                val media = proxy.image
                                if (media == null) { proxy.close(); return@analyzer }
                                val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)

                                barcodeScanner.process(input)
                                    .addOnSuccessListener { barcodes ->
                                        if (barcodes.isNotEmpty() && !isConfirmed) {
                                            val bc = barcodes.first()
                                            val rawValue = bc.rawValue ?: return@addOnSuccessListener
                                            val formatStr = when (bc.format) {
                                                Barcode.FORMAT_QR_CODE -> "QR_CODE"
                                                Barcode.FORMAT_EAN_13 -> "EAN13"
                                                Barcode.FORMAT_EAN_8 -> "EAN8"
                                                Barcode.FORMAT_UPC_A -> "UPC_A"
                                                Barcode.FORMAT_UPC_E -> "UPC_E"
                                                else -> "UNKNOWN"
                                            }
                                            statusText = "📦 Code erkannt: $rawValue"
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    val entity = foodRepo.handleScan(rawValue, formatStr)
                                                    if (entity != null) {
                                                        barcodeResult = entity
                                                        when (entity.status) {
                                                            "OK" -> {
                                                                liveResult = OcrEngine.ScanResult(
                                                                    kcal100 = entity.kcal100g ?: 0.0,
                                                                    protein100 = entity.protein100g ?: 0.0,
                                                                    carbs100 = entity.carbs100g ?: 0.0,
                                                                    fat100 = entity.fat100g ?: 0.0,
                                                                    isPlausible = true,
                                                                    hasKcal = entity.kcal100g != null,
                                                                    hasProtein = entity.protein100g != null,
                                                                    hasCarbs = entity.carbs100g != null,
                                                                    hasFat = entity.fat100g != null,
                                                                    name = entity.name
                                                                )
                                                                isStableState = true
                                                                statusText = "✅ ${entity.name ?: "Produkt gefunden"}"
                                                            }
                                                            "NOT_FOUND" -> statusText = if (entity.lastError == "non_numeric_qr")
                                                                "⚠️ Kein Barcode im QR gefunden" else "❌ Produkt nicht in Datenbank"
                                                            "PENDING" -> statusText = "⏳ Gespeichert – warte auf API…"
                                                            "ERROR" -> statusText = "❌ API-Fehler – Erneut versuchen"
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("LiveScanner", "Barcode scan error", e)
                                                    statusText = "❌ Fehler beim Abruf"
                                                }
                                            }
                                        }
                                    }
                                    .addOnCompleteListener { proxy.close() }
                            }

                            val owner = findLifecycleOwner(context) ?: lifecycleOwner
                            provider.unbindAll()
                            provider.bindToLifecycle(owner, androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                        } catch (e: Exception) {
                            Log.e("LiveScanner", "Camera failed", e)
                        }
                    }, ContextCompat.getMainExecutor(activityCtx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FText("❌ Kamera nicht verfügbar", sizeSp = 18, bold = true, color = Color.Red)
                    Spacer(Modifier.height(16.dp))
                    FantasyButton("Schließen") { onDismiss() }
                }
            }
        }

        // ═══════ SCAN-RAHMEN OVERLAY ═══════
        val isStable = isStableState && liveResult != null
        val isDetecting = barcodeResult != null

        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().fillMaxHeight(0.15f).align(Alignment.TopCenter).background(Color.Black.copy(alpha = 0.6f)))
            Box(Modifier.fillMaxWidth().fillMaxHeight(0.35f).align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.7f)))
            Box(
                Modifier.fillMaxWidth(0.85f).fillMaxHeight(0.45f).align(Alignment.Center)
                    .border(2.dp, when {
                        isStable -> green; isDetecting -> FantasyColors.Accent
                        else -> Color.White.copy(alpha = 0.4f)
                    }, RoundedCornerShape(16.dp))
            ) {
                if (!isConfirmed) {
                    Box(Modifier.fillMaxWidth(0.9f).height(2.dp).align(Alignment.TopStart)
                        .offset(y = (scanY * 250).dp).padding(horizontal = 8.dp)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, green.copy(alpha = 0.8f), Color.Transparent))))
                }
            }
        }

        // ═══════ OBEN: STATUS ═══════
        Column(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Schließen", tint = Color.White) }
                FText("BARCODE-SCANNER", sizeSp = 16, bold = true, color = green)
                Spacer(Modifier.size(48.dp))
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                FText(statusText, sizeSp = 13, color = Color.White)
            }
            AnimatedVisibility(visible = showCameraHint, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().background(Color(0xFF1A2A1A).copy(alpha = 0.85f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                        FText("Die Kamera wird ausschließlich zum Scannen von Barcodes verwendet.", sizeSp = 11, color = Color.White.copy(alpha = 0.75f))
                    }
                }
            }
        }

        // ═══════ UNTEN: LIVE-WERTE ═══════
        Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp)) {
            val r = liveResult
            val bc = barcodeResult
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color(0xDD0A0F0A), Color(0xEE0A0A0A))))
                .border(1.dp, if (isStable) green.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .padding(16.dp)
            ) {
                Column {
                    if (bc != null && bc.name != null) {
                        FText(bc.name!!, sizeSp = 14, bold = true, color = FantasyColors.Accent)
                        if (bc.brand != null) FText(bc.brand!!, sizeSp = 11, color = FantasyColors.GrayText)
                        Spacer(Modifier.height(6.dp))
                    }
                    FText("PRO 100 g", sizeSp = 10, bold = true, color = FantasyColors.GrayText)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        NutrientCol("🔥", "kcal", if (r != null) "${r.kcal100.toInt()}" else "—", green)
                        NutrientCol("🥩", "Protein", if (r != null) "%.1f".format(r.protein100) + "g" else "—", Color(0xFF64B5F6))
                        NutrientCol("🍞", "Carbs", if (r != null) "%.1f".format(r.carbs100) + "g" else "—", Color(0xFFFFD54F))
                        NutrientCol("🧈", "Fett", if (r != null) "%.1f".format(r.fat100) + "g" else "—", Color(0xFFFF8A65))
                    }
                    if (r?.portionSize != null) {
                        Spacer(Modifier.height(6.dp))
                        FText("Portionsgröße: ${r.portionSize!!.toInt()}g", sizeSp = 11, color = FantasyColors.Accent)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (bc?.status) {
                            "OK" -> { Icon(Icons.Default.Check, null, tint = green, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); FText("Produkt gefunden", sizeSp = 11, color = green) }
                            "PENDING" -> FText("⏳ Lade…", sizeSp = 11, color = Color(0xFFFFD54F))
                            "NOT_FOUND" -> FText("❌ Nicht gefunden", sizeSp = 11, color = Color(0xFFFF6B6B))
                            "ERROR" -> FText("❌ Fehler", sizeSp = 11, color = Color(0xFFFF6B6B))
                            else -> {}
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            AnimatedVisibility(visible = bc?.status == "OK" && r != null, enter = fadeIn(), exit = fadeOut()) {
                FantasyButton("✅ Werte übernehmen", Modifier.fillMaxWidth()) { if (r != null) { isConfirmed = true; onResult(r) } }
            }
            if (bc != null && (bc.status == "ERROR" || (bc.status == "PENDING" && bc.lastError == "offline"))) {
                Spacer(Modifier.height(4.dp))
                FantasyButton("🔄 Erneut versuchen", Modifier.fillMaxWidth(), alpha = 0.7f) {
                    scope.launch(Dispatchers.IO) {
                        statusText = "⏳ Lade erneut…"
                        val retry = foodRepo.retryFetch(bc.code)
                        if (retry != null) {
                            barcodeResult = retry
                            if (retry.status == "OK") {
                                liveResult = OcrEngine.ScanResult(
                                    kcal100 = retry.kcal100g ?: 0.0, protein100 = retry.protein100g ?: 0.0,
                                    carbs100 = retry.carbs100g ?: 0.0, fat100 = retry.fat100g ?: 0.0,
                                    isPlausible = true, hasKcal = retry.kcal100g != null, hasProtein = retry.protein100g != null,
                                    hasCarbs = retry.carbs100g != null, hasFat = retry.fat100g != null, name = retry.name
                                )
                                isStableState = true; statusText = "✅ ${retry.name ?: "Produkt gefunden"}"
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NutrientCol(emoji: String, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FText(emoji, sizeSp = 18)
        FText(value, sizeSp = 18, bold = true, color = color)
        FText(label, sizeSp = 9, color = FantasyColors.GrayText)
    }
}
