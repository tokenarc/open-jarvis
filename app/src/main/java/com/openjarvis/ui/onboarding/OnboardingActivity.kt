package com.openjarvis.ui.onboarding

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openjarvis.ui.theme.VoidColor

class OnboardingActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (isOnboardingComplete()) {
            finish()
            return
        }
        
        setContent { OnboardingScreen(onComplete = { finish() }) }
    }
    
    private fun isOnboardingComplete(): Boolean {
        val prefs = getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("onboarding_complete", false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var currentScreen by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    
    Scaffold(
        containerColor = VoidColor.Void950
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (currentScreen) {
                0 -> WelcomeScreen(
                    onGetStarted = { currentScreen = 1 }
                )
                1 -> PermissionsScreen(
                    onBack = { currentScreen = 0 },
                    onContinue = { currentScreen = 2 }
                )
                2 -> ProviderScreen(
                    onBack = { currentScreen = 1 },
                    onContinue = { currentScreen = 3 },
                    onSkip = { currentScreen = 3 }
                )
                3 -> TryItScreen(
                    onBack = { currentScreen = 2 },
                    onSuccess = {
                        completeOnboarding(context)
                        onComplete()
                    }
                )
                4 -> ReadyScreen(onStart = {
                    completeOnboarding(context)
                    onComplete()
                })
            }
            
            if (currentScreen in 1..3) {
                Spacer(modifier = Modifier.weight(1f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentScreen > 0 && currentScreen < 4) {
                        TextButton(onClick = { currentScreen-- }) {
                            Text("Back", color = VoidColor.TextDisabled)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    
                    if (currentScreen < 4) {
                        TextButton(onClick = { currentScreen++ }) {
                            Text("Skip", color = VoidColor.TextDisabled)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "OPEN\nJARVIS",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = VoidColor.Violet,
            textAlign = TextAlign.Center,
            lineHeight = 52.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Your AI. Your Phone.\nYour Control.",
            fontSize = 20.sp,
            color = VoidColor.TextPrimary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Give Jarvis a prompt.\nIt handles everything.",
            fontSize = 14.sp,
            color = VoidColor.TextDisabled,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Button(
            onClick = onGetStarted,
            colors = ButtonDefaults.buttonColors(
                containerColor = VoidColor.Violet
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Get Started",
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun PermissionsScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Permissions",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = VoidColor.TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Jarvis needs a few permissions to control your device",
            fontSize = 14.sp,
            color = VoidColor.TextDisabled
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        PermissionRow("Accessibility Service", "Required to tap, type, and control apps")
        PermissionRow("Overlay Permission", "Required to show the floating control pill")
        PermissionRow("Notification Access", "Required to read and reply to messages")
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(containerColor = VoidColor.Violet),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun PermissionRow(title: String, subtitle: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = VoidColor.Void800)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = VoidColor.Violet,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Medium, color = VoidColor.TextPrimary)
                Text(subtitle, fontSize = 12.sp, color = VoidColor.TextDisabled)
            }
        }
    }
}

@Composable
private fun ProviderScreen(onBack: () -> Unit, onContinue: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Choose Your AI",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = VoidColor.TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Connect an AI brain",
            fontSize = 14.sp,
            color = VoidColor.TextDisabled
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProviderCard("Groq", "Free · Fast", Modifier.weight(1f))
            ProviderCard("Gemini", "Free tier", Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProviderCard("OpenRouter", "Free models", Modifier.weight(1f))
            ProviderCard("Local", "Offline", Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(containerColor = VoidColor.Violet),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun ProviderCard(name: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = VoidColor.Void800),
        border = BorderStroke(1.dp, VoidColor.BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(name, fontWeight = FontWeight.Bold, color = VoidColor.TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = VoidColor.TextDisabled)
        }
    }
}

@Composable
private fun TryItScreen(onBack: () -> Unit, onSuccess: () -> Unit) {
    var command by remember { mutableStateOf("Open Chrome and search for weather") }
    var isRunning by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Try It",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = VoidColor.TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Let's try your first prompt",
            fontSize = 14.sp,
            color = VoidColor.TextDisabled
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = command,
            onValueChange = { command = it },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VoidColor.Violet,
                unfocusedBorderColor = VoidColor.BorderSubtle
            ),
            placeholder = { Text("Type your command...") }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(VoidColor.Void900, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isRunning) {
                CircularProgressIndicator(color = VoidColor.Violet)
            } else {
                Text(
                    "Press Run to execute",
                    color = VoidColor.TextDisabled
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                isRunning = true
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    kotlinx.coroutines.delay(2000)
                    isRunning = false
                    onSuccess()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = VoidColor.Violet),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRunning
        ) {
            Text(if (isRunning) "Running..." else "Run This")
        }
    }
}

@Composable
private fun ReadyScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = VoidColor.Violet,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "You're Ready",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = VoidColor.TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Jarvis is ready to help you",
            fontSize = 14.sp,
            color = VoidColor.TextDisabled
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = VoidColor.Violet),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Start Using Jarvis",
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )
        }
    }
}

private fun completeOnboarding(context: Context) {
    context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("onboarding_complete", true)
        .apply()
}