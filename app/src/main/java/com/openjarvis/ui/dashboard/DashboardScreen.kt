package com.openjarvis.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openjarvis.graphify.GraphifyRepository
import com.openjarvis.graphify.nodes.AppNode
import com.openjarvis.graphify.nodes.TaskNode
import com.openjarvis.ui.theme.VoidColor
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onStartOverlay: () -> Unit,
    onOpenSettings: () -> Unit,
    graphifyRepo: GraphifyRepository,
    modifier: Modifier = Modifier
) {
    var recentTasks by remember { mutableStateOf<List<TaskNode>>(emptyList()) }
    var mostUsedApps by remember { mutableStateOf<List<AppNode>>(emptyList()) }
    var isSystemActive by remember { mutableStateOf(true) }
    
    var heroAlpha by remember { mutableFloatStateOf(0f) }
    var statsAlpha by remember { mutableFloatStateOf(0f) }
    var tasksAlpha by remember { mutableFloatStateOf(0f) }
    var appsAlpha by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(Unit) {
        recentTasks = graphifyRepo.getRecentTasks(10)
        mostUsedApps = graphifyRepo.getMostUsedApps(4)
    }
    
    LaunchedEffect(Unit) {
        heroAlpha = 1f
        delay(150)
        statsAlpha = 1f
        delay(150)
        tasksAlpha = 1f
        delay(200)
        appsAlpha = 1f
    }
    
    val scrollState = rememberScrollState()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidColor.Void950)
    ) {
        OrbBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(56.dp))
            
            HeroSection(isSystemActive = isSystemActive)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            StatsRow(tasks = recentTasks, alpha = statsAlpha)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            RecentTasksSection(tasks = recentTasks, alpha = tasksAlpha)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            QuickAppsSection(apps = mostUsedApps, alpha = appsAlpha)
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        BottomNavBar(
            onSettingsClick = onOpenSettings,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun OrbBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_y"
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, 200.dp.toPx() + offsetY.dp.toPx())
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    VoidColor.Violet.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                center = center,
                radius = 200.dp.toPx()
            ),
            center = center,
            radius = 200.dp.toPx()
        )
    }
}

@Composable
private fun HeroSection(isSystemActive: Boolean) {
    Column {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = VoidColor.Void800,
            border = androidx.compose.foundation.BorderStroke(1.dp, VoidColor.BorderGlow)
        ) {
            Text(
                text = "AI AGENT v0.1",
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight(500),
                    fontSize = 10.sp,
                    letterSpacing = 4.sp,
                    color = VoidColor.Violet
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val annotatedText = buildAnnotatedString {
            withStyle(androidx.compose.ui.text.SpanStyle(color = VoidColor.TextPrimary)) {
                append("OPEN")
            }
            append(" ")
            withStyle(androidx.compose.ui.text.SpanStyle(
                brush = Brush.linearGradient(
                    colors = listOf(VoidColor.Violet, VoidColor.Cyan)
                )
            )) {
                append("JARVIS")
            }
        }
        
        Text(
            text = annotatedText,
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight(700),
                fontSize = 38.sp,
                letterSpacing = (-0.5).sp
            )
        )
        
        Text(
            text = "Your device. Your commands.",
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight(400),
                fontSize = 13.sp,
                color = VoidColor.TextDisabled
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        StatusBadge(isActive = isSystemActive)
    }
}

@Composable
private fun StatusBadge(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge")
    
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale"
    )
    
    val (bgColor, borderColor, textColor, dotColor) = if (isActive) {
        listOf(
            VoidColor.Green.copy(alpha = 0.08f),
            VoidColor.Green.copy(alpha = 0.25f),
            VoidColor.Green,
            VoidColor.Green
        )
    } else {
        listOf(
            VoidColor.Red.copy(alpha = 0.08f),
            VoidColor.Red.copy(alpha = 0.25f),
            VoidColor.Red,
            VoidColor.Red
        )
    }
    
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .scale(if (isActive) dotScale else 1f)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = dotColor)
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = if (isActive) "System Active" else "Tap to Activate",
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight(500),
                    fontSize = 12.sp,
                    color = textColor
                )
            )
        }
    }
}

@Composable
private fun StatsRow(tasks: List<TaskNode>, alpha: Float) {
    val animatedCount by animateIntAsState(
        targetValue = tasks.size.coerceAtLeast(0),
        animationSpec = tween(500, easing = EaseOutQuart),
        label = "count"
    )
    
    Column(alpha = alpha) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                count = animatedCount,
                label = "Tasks Done",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                count = 0,
                label = "Apps Used",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                count = 0,
                label = "Patterns",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(count: Int, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(14.dp),
        color = VoidColor.Void800,
        border = androidx.compose.foundation.BorderStroke(1.dp, VoidColor.BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = count.toString(),
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight(700),
                    fontSize = 22.sp,
                    color = VoidColor.TextPrimary
                )
            )
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight(400),
                    fontSize = 11.sp,
                    color = VoidColor.TextDisabled
                )
            )
        }
    }
}

@Composable
private fun RecentTasksSection(tasks: List<TaskNode>, alpha: Float) {
    Column(alpha = alpha) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "RECENT TASKS",
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight(600),
                    fontSize = 10.sp,
                    letterSpacing = 3.sp,
                    color = VoidColor.TextDisabled
                )
            )
            Text(
                text = "see all →",
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight(500),
                    fontSize = 11.sp,
                    color = VoidColor.Violet
                )
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (tasks.isEmpty()) {
            Text(
                text = "No tasks yet. Try opening an app!",
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight(400),
                    fontSize = 13.sp,
                    color = VoidColor.TextDisabled
                )
            )
        } else {
            tasks.take(5).forEachIndexed { index, task ->
                TaskCard(task = task)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun TaskCard(task: TaskNode) {
    val isSuccess = task.result.contains("Opened") || task.result.contains("Success")
    val outcomeColor = if (isSuccess) VoidColor.Green else VoidColor.Red
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(16.dp),
        color = VoidColor.Void900,
        border = androidx.compose.foundation.BorderStroke(1.dp, VoidColor.BorderSubtle)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        color = outcomeColor,
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = task.command,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = VoidColor.TextPrimary
                        ),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = formatRelativeTime(task.timestamp),
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontSize = 10.sp,
                            color = VoidColor.TextDisabled
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppInitialBadge(label = task.command)
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "System App",
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontSize = 11.sp,
                            color = VoidColor.TextDisabled
                        )
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = VoidColor.Green.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (isSuccess) "done" else "failed",
                            style = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight(500),
                                fontSize = 10.sp,
                                color = outcomeColor
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppInitialBadge(label: String) {
    val colors = listOf(VoidColor.Violet, VoidColor.Cyan, VoidColor.Green, VoidColor.Amber)
    val colorIndex = label.hashCode().mod(colors.size)
    val bgColor = colors[colorIndex]
    val initial = label.firstOrNull()?.uppercaseChar() ?: 'J'
    
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.toString(),
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight(700),
                fontSize = 8.sp,
                color = Color.White
            )
        )
    }
}

@Composable
private fun QuickAppsSection(apps: List<AppNode>, alpha: Float) {
    Column(alpha = alpha) {
        Text(
            text = "QUICK APPS",
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight(600),
                fontSize = 10.sp,
                letterSpacing = 3.sp,
                color = VoidColor.TextDisabled
            )
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.height(220.dp)
        ) {
            items(apps.ifEmpty { emptyList() }) { app ->
                AppTile(app = app)
            }
        }
    }
}

@Composable
private fun AppTile(app: AppNode) {
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .height(100.dp),
        shape = RoundedCornerShape(20.dp),
        color = VoidColor.Void900,
        border = androidx.compose.foundation.BorderStroke(1.dp, VoidColor.BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppInitialBadge(label = app.label)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = app.label,
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight(500),
                    fontSize = 12.sp,
                    color = VoidColor.TextSecondary
                )
            )
        }
    }
}

@Composable
private fun BottomNavBar(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = VoidColor.Void900
    ) {
        Column {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = VoidColor.BorderSubtle
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val items = listOf("Home", "Tasks", "Settings")
                
                items.forEachIndexed { index, item ->
                    val isSelected = item == "Home" || item == "Tasks"
                    val isSettings = item == "Settings"
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            if (isSettings) onSettingsClick()
                        }
                    ) {
                        if (isSelected) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = VoidColor.Violet.copy(alpha = 0.08f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (item == "Home") Icons.Filled.Task else Icons.Filled.Task,
                                        contentDescription = item,
                                        tint = VoidColor.Violet,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = item,
                                tint = VoidColor.TextDisabled,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
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