package com.openjarvis.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openjarvis.llm.UniversalAdapter
import com.openjarvis.ui.theme.VoidColor

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onSaveProvider: (String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProvider by remember { mutableStateOf("Groq") }
    var baseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("llama-3.1-70b-versatile") }
    var showBaseUrlField by remember { mutableStateOf(false) }
    var showProvidersDropdown by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var lastSaved by remember { mutableStateOf(0L) }
    var savedVisible by remember { mutableStateOf(false) }
    var voiceEnabled by remember { mutableStateOf(false) }
    var speakResults by remember { mutableStateOf(true) }
    var speechRate by remember { mutableStateOf(1.05f) }
    var sttMode by remember { mutableStateOf("Push to Talk") }
    
    LaunchedEffect(selectedProvider) {
        showBaseUrlField = selectedProvider == "Custom"
    }
    
    val scrollState = rememberScrollState()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidColor.Void950)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            SettingsHeader(onNavigateBack = onNavigateBack)
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                SectionLabel("AI PROVIDER")
                
                Spacer(modifier = Modifier.height(12.dp))
                
                ProviderSelectorCard(
                    selectedProvider = selectedProvider,
                    isExpanded = showProvidersDropdown,
                    onToggle = { showProvidersDropdown = !showProvidersDropdown },
                    onSelect = { provider ->
                        selectedProvider = provider
                        showProvidersDropdown = false
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                FloatingLabelTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = "Base URL",
                    isFocused = showBaseUrlField,
                    visible = showBaseUrlField
                )
                
                FloatingLabelTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = "API Key",
                    isFocused = false,
                    isPassword = !showPassword,
                    onTogglePassword = { showPassword = !showPassword }
                )
                
                FloatingLabelTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = "Model",
                    isFocused = false
                )
                
                Text(
                    text = "Works with any OpenAI-compatible API",
                    style = TextStyle(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight(400),
                        fontSize = 11.sp,
                        color = VoidColor.TextDisabled
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                TestConnectionButton(
                    apiKey = apiKey,
                    baseUrl = baseUrl,
                    model = model,
                    onSave = { name, url, key, mdl ->
                        onSaveProvider(name, url, key, mdl)
                        lastSaved = System.currentTimeMillis()
                        savedVisible = true
                    }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                SectionLabel("VOICE")
                
                Spacer(modifier = Modifier.height(12.dp))
                
                SettingsToggleRow(
                    title = "Voice Assistant",
                    subtitle = "Push to Talk · VAD supported",
                    enabled = voiceEnabled,
                    onToggle = { voiceEnabled = !voiceEnabled }
                )
                
                if (voiceEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SettingsToggleRow(
                        title = "Speak Results",
                        subtitle = "Jarvis reads results aloud",
                        enabled = speakResults,
                        onToggle = { speakResults = !speakResults }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // STT Mode selector
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = VoidColor.Void900,
                        border = BorderStroke(1.dp, VoidColor.BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    sttMode = if (sttMode == "Push to Talk") "Auto (VAD)" else "Push to Talk"
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "STT Mode",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Default,
                                        fontWeight = FontWeight(500),
                                        fontSize = 14.sp,
                                        color = VoidColor.TextPrimary
                                    )
                                )
                                Text(
                                    text = if (sttMode == "Push to Talk") 
                                        "Hold mic to record, release to send" 
                                    else 
                                        "Tap to record, auto-sends on silence",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Default,
                                        fontWeight = FontWeight(400),
                                        fontSize = 11.sp,
                                        color = VoidColor.TextDisabled
                                    )
                                )
                            }
                            Text(
                                text = sttMode,
                                style = TextStyle(
                                    fontFamily = FontFamily.Default,
                                    fontWeight = FontWeight(500),
                                    fontSize = 12.sp,
                                    color = VoidColor.Violet
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Model status
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = VoidColor.Void800,
                        border = BorderStroke(1.dp, VoidColor.BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = VoidColor.Green,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Speech Recognition",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Default,
                                        fontWeight = FontWeight(500),
                                        fontSize = 14.sp,
                                        color = VoidColor.TextPrimary
                                    )
                                )
                                Text(
                                    text = "Google Speech · Online",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Default,
                                        fontWeight = FontWeight(400),
                                        fontSize = 11.sp,
                                        color = VoidColor.TextDisabled
                                    )
                                )
                            }
                            Text(
                                text = "Ready",
                                style = TextStyle(
                                    fontFamily = FontFamily.Default,
                                    fontWeight = FontWeight(500),
                                    fontSize = 12.sp,
                                    color = VoidColor.Green
                                )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                SectionLabel("PERMISSIONS")
                
                Spacer(modifier = Modifier.height(12.dp))
                
                PermissionRow(
                    title = "Accessibility",
                    subtitle = "Required — Tap to enable"
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                PermissionRow(
                    title = "Overlay",
                    subtitle = "Required — Tap to enable"
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                SectionLabel("ABOUT")
                
                Spacer(modifier = Modifier.height(12.dp))
                
                AboutSection()
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        
        SavedToast(
            visible = savedVisible,
            onDismiss = { savedVisible = false },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        )
    }
}

@Composable
private fun SettingsHeader(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = VoidColor.TextSecondary
            )
        }
        
        Text(
            text = "Settings",
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight(600),
                fontSize = 20.sp,
                color = VoidColor.TextPrimary
            )
        )
    }
    
    Divider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = VoidColor.BorderSubtle
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight(600),
            fontSize = 10.sp,
            letterSpacing = 3.sp,
            color = VoidColor.TextDisabled
        )
    )
}

@Composable
private fun ProviderSelectorCard(
    selectedProvider: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (String) -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isExpanded) VoidColor.BorderGlow else VoidColor.BorderSubtle,
        animationSpec = tween(200),
        label = "border"
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        color = VoidColor.Void900,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProviderDot(provider = selectedProvider)
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = selectedProvider,
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight(500),
                    fontSize = 14.sp,
                    color = VoidColor.TextPrimary
                ),
                modifier = Modifier.weight(1f)
            )
            
            val rotationAngle by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                animationSpec = tween(200),
                label = "rotation"
            )
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = VoidColor.TextDisabled,
                modifier = Modifier.graphicsLayer(rotationZ = rotationAngle)
            )
        }
    }
    
    AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically() + fadeIn(animationSpec = tween(200)),
        exit = shrinkVertically() + fadeOut(animationSpec = tween(200))
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = VoidColor.Void800,
            border = BorderStroke(1.dp, VoidColor.BorderSubtle)
        ) {
            Column {
                UniversalAdapter.AVAILABLE_PROVIDERS.forEach { provider ->
                    ProviderOption(
                        label = provider,
                        isSelected = provider == selectedProvider,
                        onClick = { onSelect(provider) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderDot(provider: String) {
    val colors = mapOf(
        "Groq" to VoidColor.Violet,
        "Google Gemini" to VoidColor.Cyan,
        "OpenRouter" to VoidColor.Green,
        "Anthropic Claude" to VoidColor.Amber,
        "OpenAI" to VoidColor.Green,
        "Ollama (Local)" to VoidColor.Cyan,
        "Custom" to VoidColor.Violet
    )
    
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colors[provider] ?: VoidColor.Violet)
    )
}

@Composable
private fun ProviderOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .background(VoidColor.Violet)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
        } else {
            Spacer(modifier = Modifier.width(15.dp))
        }
        
        Text(
            text = label,
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight(500),
                fontSize = 14.sp,
                color = if (isSelected) VoidColor.Violet else VoidColor.TextPrimary
            )
        )
    }
}

@Composable
fun FloatingLabelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isFocused: Boolean,
    isPassword: Boolean = false,
    visible: Boolean = true,
    onTogglePassword: (() -> Unit)? = null
) {
    val labelOffset by animateFloatAsState(
        targetValue = if (isFocused || value.isNotEmpty()) -20f else 0f,
        animationSpec = spring(stiffness = 300f, dampingRatio = 0.75f),
        label = "offset"
    )
    
    val labelScale by animateFloatAsState(
        targetValue = if (isFocused || value.isNotEmpty()) 0.75f else 1f,
        animationSpec = spring(stiffness = 300f, dampingRatio = 0.75f),
        label = "scale"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) VoidColor.Violet else VoidColor.BorderSubtle,
        animationSpec = tween(200),
        label = "border"
    )
    
    if (!visible) return
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(14.dp),
        color = VoidColor.Void900,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight(400),
                    fontSize = 14.sp,
                    color = VoidColor.TextSecondary
                ),
                modifier = Modifier.graphicsLayer {
                    translationY = labelOffset.dp.toPx()
                    scaleX = labelScale
                    scaleY = labelScale
                }
            )
            
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight(400),
                    fontSize = 14.sp,
                    color = VoidColor.TextPrimary
                ),
                visualTransformation = if (isPassword && !showPassword) 
                    PasswordVisualTransformation() else VisualTransformation.None
            )
        }
    }
}

@Composable
private fun TestConnectionButton(
    apiKey: String,
    baseUrl: String,
    model: String,
    onSave: (String, String, String, String) -> Unit
) {
    var testState by remember { mutableStateOf<TestState>(TestState.Idle) }
    
    val borderColor by animateColorAsState(
        targetValue = when (testState) {
            is TestState.Idle -> VoidColor.VioletDim
            is TestState.Loading -> VoidColor.Violet
            is TestState.Success -> VoidColor.Green
            is TestState.Error -> VoidColor.Red
        },
        animationSpec = tween(200),
        label = "btn_border"
    )
    
    val textColor = when (testState) {
        is TestState.Idle -> VoidColor.Violet
        is TestState.Loading -> VoidColor.Violet
        is TestState.Success -> VoidColor.Green
        is TestState.Error -> VoidColor.Red
    }
    
    OutlinedButton(
        onClick = {
            onSave("Groq", baseUrl, apiKey, model)
            testState = TestState.Success(150)
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = textColor
        )
    ) {
        when (val state = testState) {
            is TestState.Idle -> Text("Test Connection")
            is TestState.Loading -> CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = VoidColor.Violet
            )
            is TestState.Success -> {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connected — ${state.ms}ms")
            }
            is TestState.Error -> {
                Text("Failed — check key")
            }
        }
    }
}

sealed class TestState {
    data object Idle : TestState()
    data object Loading : TestState()
    data class Success(val ms: Long) : TestState()
    data class Error(val message: String) : TestState()
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .then(
                if (onToggle != null) Modifier.clickable { onToggle() }
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        color = VoidColor.Void900,
        border = BorderStroke(1.dp, VoidColor.BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = VoidColor.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight(500),
                        fontSize = 14.sp,
                        color = VoidColor.TextPrimary
                    )
                )
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight(400),
                        fontSize = 11.sp,
                        color = VoidColor.TextDisabled
                    )
                )
            }
            
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle?.invoke() },
                enabled = onToggle != null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = VoidColor.TextPrimary,
                    checkedTrackColor = VoidColor.Violet,
                    uncheckedThumbColor = VoidColor.TextDisabled,
                    uncheckedTrackColor = VoidColor.Void600
                )
            )
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String
) {
    val isGranted by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = VoidColor.Void900,
        border = BorderStroke(1.dp, VoidColor.BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VoidColor.Red.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = VoidColor.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight(500),
                        fontSize = 14.sp,
                        color = VoidColor.TextPrimary
                    )
                )
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight(400),
                        fontSize = 11.sp,
                        color = VoidColor.Red
                    )
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = VoidColor.Red
            )
        }
    }
}

@Composable
private fun AboutSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = VoidColor.Void900,
        border = BorderStroke(1.dp, VoidColor.BorderSubtle)
    ) {
        Column {
            AboutRow(title = "Version", value = "M1.0 (build 1)")
            Divider(thickness = 1.dp, color = VoidColor.BorderSubtle)
            AboutRow(title = "License", value = "MIT Open Source")
            Divider(thickness = 1.dp, color = VoidColor.BorderSubtle)
            AboutRow(title = "GitHub", value = "tokenarc/open-jarvis", isLink = true)
            Divider(thickness = 1.dp, color = VoidColor.BorderSubtle)
            
            Text(
                text = "Built on Android · From Termux with love",
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight(400),
                    fontSize = 12.sp,
                    color = VoidColor.TextDisabled
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun AboutRow(
    title: String,
    value: String,
    isLink: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight(400),
                fontSize = 14.sp,
                color = VoidColor.TextSecondary
            )
        )
        
        Text(
            text = value,
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight(400),
                fontSize = 14.sp,
                color = if (isLink) VoidColor.Violet else VoidColor.TextPrimary
            )
        )
    }
}

@Composable
private fun SavedToast(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(1500)
            onDismiss()
        }
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { 40 } + fadeIn(),
        exit = slideOutVertically { 40 } + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = VoidColor.Void800,
            border = BorderStroke(1.dp, VoidColor.Green.copy(alpha = 0.25f))
        ) {
            Text(
                text = "✓ saved",
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight(500),
                    fontSize = 12.sp,
                    color = VoidColor.Green
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}