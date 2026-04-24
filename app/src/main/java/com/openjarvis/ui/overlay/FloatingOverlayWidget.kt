package com.openjarvis.ui.overlay

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openjarvis.agent.AgentState
import com.openjarvis.graphify.nodes.TaskNode
import com.openjarvis.ui.theme.VoidColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FloatingOverlayWidget(
    agentState: AgentState,
    recentTasks: List<TaskNode>,
    onCommand: (String) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var commandText by remember { mutableStateOf("") }
    var hasAnimatedOnce by remember { mutableStateOf(false) }
    var placeholderText by remember { mutableStateOf("") }
    
    LaunchedEffect(isExpanded) {
        if (isExpanded && !hasAnimatedOnce) {
            hasAnimatedOnce = true
            val full = "command..."
            for (char in full) {
                placeholderText += char
                delay(30)
            }
        }
    }
    
    AnimatedContent(
        targetState = isExpanded,
        transitionSpec = {
            if (targetState) {
                (expandHorizontally(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMedium,
                        dampingRatio = 0.8f
                    )
                ) + expandVertically(
                    animationSpec = spring(
                        stiffness = 260f,
                        dampingRatio = 0.8f
                    )
                ) + fadeIn(animationSpec = tween(150, delayMillis = 240)))
            } else {
                (shrinkHorizontally(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMedium,
                        dampingRatio = 0.8f
                    )
                ) + shrinkVertically(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMedium,
                        dampingRatio = 0.8f
                    )
                ) + fadeOut(animationSpec = tween(100)))
            }
        },
        label = "overlay_expand"
    ) { expanded ->
        if (expanded) {
            ExpandedOverlay(
                agentState = agentState,
                commandText = commandText,
                onCommandTextChange = { commandText = it },
                placeholderText = placeholderText,
                recentTasks = recentTasks,
                onSend = {
                    if (commandText.isNotBlank()) {
                        onCommand(commandText)
                        commandText = ""
                    }
                },
                onCollapse = {
                    isExpanded = false
                    onCollapse()
                },
                modifier = modifier
            )
        } else {
            CollapsedPill(
                agentState = agentState,
                onClick = { isExpanded = true },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun CollapsedPill(
    agentState: AgentState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var glowAlpha by remember { mutableFloatStateOf(0.15f) }
    
    LaunchedEffect(Unit) {
        infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.45f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        ) { glowAlpha = this }
    }
    
    val statusScale by rememberInfiniteTransition(label = "status").animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val statusColor = when (agentState) {
        is AgentState.Idle -> VoidColor.Violet
        is AgentState.Running -> VoidColor.Cyan
        is AgentState.Done -> VoidColor.Green
        is AgentState.Error -> VoidColor.Red
    }
    
    Surface(
        modifier = modifier
            .width(140.dp)
            .height(48.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            },
        shape = RoundedCornerShape(999.dp),
        color = VoidColor.Void900.copy(alpha = 0.95f),
        tonalElevation = 0.dp
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = VoidColor.Violet.copy(alpha = glowAlpha),
                style = Stroke(width = 1.dp.toPx()),
                cornerRadius = CornerRadius(999.dp.toPx())
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .scale(statusScale),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = statusColor)
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "JARVIS",
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight(600),
                    fontSize = 11.sp,
                    letterSpacing = 4.sp,
                    color = VoidColor.TextDisabled
                )
            )
        }
    }
}

@Composable
private fun ExpandedOverlay(
    agentState: AgentState,
    commandText: String,
    onCommandTextChange: (String) -> Unit,
    placeholderText: String,
    recentTasks: List<TaskNode>,
    onSend: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stepText = when (agentState) {
        is AgentState.Running -> agentState.step
        is AgentState.Done -> agentState.result
        is AgentState.Error -> agentState.message
        else -> ""
    }
    
    LaunchedEffect(agentState) {
        when (agentState) {
            is AgentState.Done -> {
                delay(2000)
                onCollapse()
            }
            is AgentState.Error -> {
                delay(4000)
                onCollapse()
            }
            else -> {}
        }
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = VoidColor.Void900,
        tonalElevation = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(VoidColor.Violet)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(VoidColor.Violet.copy(alpha = 0.2f))
            )
            
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusOrb(state = agentState)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "JARVIS",
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight(600),
                                fontSize = 12.sp,
                                color = VoidColor.TextPrimary
                            )
                        )
                    }
                    
                    IconButton(onClick = onCollapse) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = VoidColor.TextDisabled
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                StatusLine(state = agentState, stepText = stepText)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                DividerLine()
                
                Spacer(modifier = Modifier.height(12.dp))
                
                InputRow(
                    commandText = commandText,
                    onCommandTextChange = onCommandTextChange,
                    placeholderText = if (commandText.isEmpty()) placeholderText else "",
                    onSend = onSend
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                DividerLine()
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TaskLogWidget(tasks = recentTasks.take(3))
            }
        }
    }
}

@Composable
private fun StatusOrb(state: AgentState) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_scale"
    )
    
    val color = when (state) {
        is AgentState.Idle -> VoidColor.Violet
        is AgentState.Running -> VoidColor.Cyan
        is AgentState.Done -> VoidColor.Green
        is AgentState.Error -> VoidColor.Red
    }
    
    Box(
        modifier = Modifier
            .size(8.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = color)
        }
    }
}

@Composable
private fun StatusLine(state: AgentState, stepText: String) {
    val cursorAlpha by rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )
    
    val textColor = when (state) {
        is AgentState.Idle -> VoidColor.TextDisabled
        is AgentState.Running -> VoidColor.Cyan
        is AgentState.Done -> VoidColor.Green
        is AgentState.Error -> VoidColor.Red
    }
    
    Row(
        modifier = Modifier.height(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (state) {
                is AgentState.Idle -> "_ ready for command"
                is AgentState.Running -> "◉ $stepText"
                is AgentState.Done -> "✓ done in ${stepText}"
                is AgentState.Error -> "✗ $stepText"
                else -> ""
            },
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = textColor
            )
        )
        
        if (state is AgentState.Idle) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(12.dp)
                    .background(VoidColor.TextDisabled.copy(alpha = cursorAlpha))
            )
        }
    }
}

@Composable
private fun InputRow(
    commandText: String,
    onCommandTextChange: (String) -> Unit,
    placeholderText: String,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "›",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                color = VoidColor.Violet
            )
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        BasicTextField(
            value = commandText,
            onValueChange = onCommandTextChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = VoidColor.TextPrimary
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrect = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            decorationBox = { innerTextField ->
                Box {
                    if (commandText.isEmpty()) {
                        Text(
                            text = placeholderText,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = VoidColor.TextDisabled.copy(alpha = 0.5f)
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        val sendButtonScale = remember { Animatable(1f) }
        val sendButtonColor = if (commandText.isEmpty()) VoidColor.Void700 else VoidColor.Violet
        
        Surface(
            modifier = Modifier
                .size(36.dp)
                .scale(sendButtonScale.value),
            shape = RoundedCornerShape(8.dp),
            color = sendButtonColor,
            onClick = onSend
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowUp,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DividerLine() {
    Divider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = VoidColor.Void600
    )
}

@Composable
private fun TaskLogWidget(tasks: List<TaskNode>) {
    Column(modifier = Modifier.heightIn(max = 80.dp)) {
        tasks.forEachIndexed { index, task ->
            var visible by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                delay((index * 80).toLong())
                visible = true
            }
            
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically { -24 } + fadeIn()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (task.result.contains("Error") || task.result.contains("Failed"))
                                    VoidColor.Red else VoidColor.Green,
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = task.command,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = VoidColor.TextDisabled
                        ),
                        maxLines = 1
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Text(
                        text = formatRelativeTime(task.timestamp),
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontSize = 10.sp,
                            color = VoidColor.TextDisabled
                        )
                    )
                }
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60000
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        else -> "${minutes / 60}h"
    }
}