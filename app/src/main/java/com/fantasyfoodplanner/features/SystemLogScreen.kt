package com.fantasyfoodplanner.features

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.fantasyfoodplanner.logic.SystemLogger
import com.fantasyfoodplanner.ui.*
import java.io.File

@Composable
fun SystemLogScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var logText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        logText = SystemLogger.readLog(ctx)
    }

    FantasySurface {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            MainAppBar("System Validierungsprotokoll", onBack = onBack)
            
            Spacer(Modifier.height(16.dp))

            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                color = Color(0xFF0A0A0A),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
            ) {
                Box(Modifier.padding(8.dp)) {
                    val scrollState = rememberScrollState()
                    Column(Modifier.verticalScroll(scrollState)) {
                        logText.split("\n").forEach { line ->
                            val isErklaerung = line.contains("|")
                            if (isErklaerung) {
                                val parts = line.split("|")
                                Row(Modifier.fillMaxWidth()) {
                                    Text(
                                        text = parts[0],
                                        color = if (line.contains("PASS")) Color.Green 
                                                else if (line.contains("WARNING") || line.contains("Issue")) Color.Red 
                                                else Color.White,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.weight(0.6f)
                                    )
                                    Text(
                                        text = "| " + parts[1].trim(),
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.weight(0.4f)
                                    )
                                }
                            } else {
                                Text(
                                    text = line,
                                    color = if (line.startsWith("---")) FantasyColors.Accent else Color.White,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FantasyButton("Log exportieren", Modifier.weight(1f)) {
                    try {
                        val file = File(ctx.filesDir, "system_validation.log")
                        if (file.exists()) {
                            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            ctx.startActivity(Intent.createChooser(intent, "Log exportieren"))
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
                FantasyButton("Log leeren", Modifier.weight(1f), alpha = 0.6f) {
                    SystemLogger.clearLog(ctx)
                    logText = "Log geleert."
                }
            }
        }
    }
}
