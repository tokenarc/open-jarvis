package com.openjarvis.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openjarvis.graphify.GraphifyRepository
import com.openjarvis.graphify.nodes.AppNode
import com.openjarvis.graphify.nodes.ContactNode
import com.openjarvis.graphify.nodes.ProviderNode
import com.openjarvis.ui.theme.VoidColor
import kotlinx.coroutines.flow.first

@Composable
fun MemoryInsightCard(
    graphifyRepo: GraphifyRepository,
    modifier: Modifier = Modifier
) {
    var patternCount by remember { mutableIntStateOf(0) }
    var topApp by remember { mutableStateOf<AppNode?>(null) }
    var topContact by remember { mutableStateOf<ContactNode?>(null) }
    var topProvider by remember { mutableStateOf<ProviderNode?>(null) }

    LaunchedEffect(Unit) {
        patternCount = graphifyRepo.getActivePatternCountFlow().first()
        topApp = graphifyRepo.getTopAppsFlow(1).first().firstOrNull()
        topContact = graphifyRepo.getTopContactFlow().first().firstOrNull()
        topProvider = graphifyRepo.getTopProviderFlow().first().firstOrNull()
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = VoidColor.Void800,
        border = androidx.compose.foundation.BorderStroke(1.dp, VoidColor.BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "MEMORY INSIGHTS",
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight(600),
                    fontSize = 10.sp,
                    letterSpacing = 3.sp,
                    color = VoidColor.TextDisabled
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InsightItem(
                    label = "Patterns",
                    value = patternCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                InsightItem(
                    label = "Top App",
                    value = topApp?.label ?: "-",
                    modifier = Modifier.weight(1f)
                )
                InsightItem(
                    label = "Contact",
                    value = topContact?.name ?: "-",
                    modifier = Modifier.weight(1f)
                )
                InsightItem(
                    label = "Provider",
                    value = topProvider?.providerName ?: "-",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun InsightItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.take(8),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight(600),
                fontSize = 13.sp,
                color = VoidColor.TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight(400),
                fontSize = 9.sp,
                color = VoidColor.TextDisabled
            )
        )
    }
}