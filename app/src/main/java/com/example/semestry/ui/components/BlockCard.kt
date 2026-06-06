package com.example.semestry.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.semestry.data.CourseBlock
import com.example.semestry.data.GradeEntry

@Composable
fun BlockCard(
    block: CourseBlock,
    showCoeff: Boolean,
    canDeleteBlock: Boolean,
    minGradeMap: Map<String, Double?>,
    targetAvgParsed: Double?,
    onBlockNameChange: (String) -> Unit,
    onToggleExpand: () -> Unit,
    onDeleteBlock: () -> Unit,
    onAddGrade: () -> Unit,
    onMatiereChange: (Int, String) -> Unit,
    onNoteChange: (Int, String) -> Unit,
    onCoeffChange: (Int, String) -> Unit,
    onDeleteGrade: (Int) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Block header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                    .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onToggleExpand, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (block.isExpanded) Icons.Default.KeyboardArrowUp
                                      else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (block.isExpanded) "Réduire" else "Développer",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                OutlinedTextField(
                    value = block.name,
                    onValueChange = onBlockNameChange,
                    label = { Text("Nom du bloc") },
                    placeholder = { Text("ex: Sciences") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )
                if (canDeleteBlock) {
                    IconButton(onClick = onDeleteBlock) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Supprimer le bloc",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Block courses (collapsible)
            AnimatedVisibility(
                visible = block.isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    block.grades.forEachIndexed { gi, entry ->
                        if (gi > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                        CourseRow(
                            index = gi + 1,
                            entry = entry,
                            showCoeff = showCoeff,
                            canDelete = block.grades.size > 1,
                            minGrade = minGradeMap[entry.id],
                            targetAvgParsed = targetAvgParsed,
                            onMatiereChange = { onMatiereChange(gi, it) },
                            onNoteChange    = { onNoteChange(gi, it) },
                            onCoeffChange   = { onCoeffChange(gi, it) },
                            onDelete        = { onDeleteGrade(gi) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onAddGrade,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" Ajouter un cours", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
