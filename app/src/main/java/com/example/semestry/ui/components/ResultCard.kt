package com.example.semestry.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.semestry.data.Stats

@Composable
fun ResultCard(result: Double, label: String, stats: Stats?, credits: Pair<Int, Int>? = null) {
    val (mention, mentionColor) = when {
        result >= 16.0 -> "Très Bien"   to Color(0xFF2E7D32)
        result >= 14.0 -> "Bien"        to Color(0xFF388E3C)
        result >= 12.0 -> "Assez Bien"  to Color(0xFF689F38)
        result >= 10.0 -> "Passable"    to Color(0xFFF57C00)
        else           -> "Insuffisant" to Color(0xFFC62828)
    }

    var animatedProgress by remember { mutableFloatStateOf(0f) }
    val arcProgress by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = 1200),
        label = "gradeArc"
    )
    LaunchedEffect(result) { animatedProgress = (result / 20.0).toFloat().coerceIn(0f, 1f) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
            )

            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(152.dp)) {
                    val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(
                        color = mentionColor.copy(alpha = 0.12f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = stroke
                    )
                    if (arcProgress > 0f) {
                        drawArc(
                            color = mentionColor,
                            startAngle = 135f,
                            sweepAngle = arcProgress * 270f,
                            useCenter = false,
                            style = stroke
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "%.2f".format(result),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = mentionColor
                    )
                    Text(
                        "/ 20",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = mentionColor.copy(alpha = 0.15f)
            ) {
                Text(
                    mention,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 7.dp),
                    color = mentionColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            if (credits != null) {
                val (obtained, total) = credits
                val creditsColor = if (obtained >= total) Color(0xFF2E7D32) else Color(0xFFF57C00)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = creditsColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        "$obtained / $total ECTS validés",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        color = creditsColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            if (stats != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Minimum",    "%.2f".format(stats.min))
                    StatItem("Maximum",    "%.2f".format(stats.max))
                    StatItem("Écart-type", "%.2f".format(stats.stdDev))
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            value,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
        )
    }
}
