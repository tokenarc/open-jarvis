package com.openjarvis.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.openjarvis.agent.OllamaHealthMonitor
import com.openjarvis.agent.RepairCenter
import kotlinx.coroutines.launch

@Composable
fun SelfRepairDashboardDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repairCenter = remember { RepairCenter(context) }
    val health = remember { OllamaHealthMonitor(context) }
    val scope = rememberCoroutineScope()
    var ollamaStatus by remember { mutableStateOf("Not checked") }
    var patchStatus by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var events by remember { mutableStateOf(repairCenter.events()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AUTONOMOUS SELF-REPAIR") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                Text("Persistent Error Knowledge Base")
                Text(repairCenter.dashboardSummary())
                Text("Ollama Local: $ollamaStatus")
                Row {
                    OutlinedButton(enabled = !busy, onClick = {
                        busy = true
                        scope.launch {
                            ollamaStatus = repairCenter.checkOllama().fold({ it }, { "Check failed: ${it.message}" })
                            busy = false
                        }
                    }) { Text("Check Ollama") }
                    OutlinedButton(enabled = !busy, onClick = {
                        busy = true
                        scope.launch {
                            ollamaStatus = repairCenter.pullConfiguredModel().fold({ it }, { "Model recovery failed: ${it.message}" })
                            busy = false
                        }
                    }) { Text("Pull / Recover Model") }
                    OutlinedButton(onClick = { health.openAccessibilitySettings() }) {
                        Text(if (health.accessibilityEnabled()) "Accessibility ON" else "Fix Accessibility")
                    }
                }
                Button(enabled = !busy && events.isNotEmpty(), onClick = {
                    busy = true
                    scope.launch {
                        patchStatus = repairCenter.generatePatchProposal(events.first()).fold(
                            { it.take(2200) }, { "Patch generation failed: ${it.message}" }
                        )
                        events = repairCenter.events()
                        busy = false
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Analyze Latest Error + Generate Patch") }
                if (busy) LinearProgressIndicator()
                if (patchStatus.isNotBlank()) Text(patchStatus)
                val hasPatch = patchStatus.contains("--- ") && patchStatus.contains("+++ ") && !patchStatus.contains("NO_SAFE_PATCH")
                if (hasPatch) {
                    OutlinedButton(
                        onClick = {
                            val payload = Base64.encodeToString(patchStatus.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Open Jarvis repair patch (base64)", payload))
                            patchStatus = "Patch payload copied. Open the GitHub repair workflow and paste it into patch_b64."
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Copy GitHub Patch Payload") }
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/sandipsonawane73618-coder/open-jarvis/actions/workflows/jarvis-repair-branch.yml")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Open GitHub Repair Workflow") }
                }
                Divider()
                Text("Repair History")
                if (events.isEmpty()) Text("No errors recorded.")
                events.take(12).forEach { event ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(8.dp)) {
                            Text("${event.code} • ${event.status}")
                            Text("${event.stage}: ${event.summary}")
                            if (event.stackTrace.isNotBlank()) Text(event.stackTrace.take(300))
                        }
                    }
                }
                Text("Patch proposals are review-only. GitHub CI must pass before update. Android installation requires user approval.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
