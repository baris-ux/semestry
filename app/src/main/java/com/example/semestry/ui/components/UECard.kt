package com.example.semestry.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.semestry.data.GradeEntry
import com.example.semestry.data.MoyenneType
import com.example.semestry.data.SubGrade
import com.example.semestry.data.UE
import com.example.semestry.utils.computeUEAverage
import com.example.semestry.utils.effectiveCoeff

@Composable
fun UECard(
    ue: UE,
    canDelete: Boolean,
    onNameChange: (String) -> Unit,
    onMoyenneTypeChange: (MoyenneType) -> Unit,
    onDeleteUE: () -> Unit,
    onAddCourse: () -> Unit,
    onCourseUpdate: (courseIndex: Int, GradeEntry) -> Unit,
    onDeleteCourse: (courseIndex: Int) -> Unit,
    onSubGradeAdd: (courseIndex: Int) -> Unit,
    onSubGradeLabelChange: (courseIndex: Int, subIndex: Int, String) -> Unit,
    onSubGradeNoteChange: (courseIndex: Int, subIndex: Int, String) -> Unit,
    onSubGradeDelete: (courseIndex: Int, subIndex: Int) -> Unit
) {
    val ueAvg      = remember(ue) { computeUEAverage(ue) }
    val totalCredits = remember(ue) {
        val coeffs = ue.courses.map { effectiveCoeff(it) }
        if (coeffs.any { it == null }) null
        else coeffs.filterNotNull().sum().takeIf { it > 0 }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // ── En-tête UE : nom + delete ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = ue.name,
                    onValueChange = onNameChange,
                    label = { Text("Nom de l'UE") },
                    placeholder = { Text("ex: Mathématiques") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDeleteUE, enabled = canDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Supprimer l'UE",
                        tint = if (canDelete) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

            // ── Moyenne UE + crédits + toggle A/G ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (ueAvg != null) {
                        val avgColor = gradeColor(ueAvg)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = avgColor.copy(alpha = 0.13f)
                        ) {
                            Text(
                                "Moy. UE : %.2f / 20".format(ueAvg),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                color = avgColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Text(
                            "Moy. UE : —",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                    if (totalCredits != null) {
                        val creditsLabel = if (totalCredits % 1.0 == 0.0)
                            "${totalCredits.toInt()} ECTS" else "%.1f ECTS".format(totalCredits)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                creditsLabel,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Toggle A (arithmétique) / G (géométrique) pour la moyenne UE
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    MoyenneType.entries.forEach { type ->
                        val sel = type == ue.moyenneType
                        Surface(
                            onClick = { onMoyenneTypeChange(type) },
                            shape = RoundedCornerShape(4.dp),
                            color = if (sel) MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Text(
                                text = if (type == MoyenneType.ARITHMETIQUE) "A" else "G",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color = if (sel) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // ── Cours ─────────────────────────────────────────────────────────
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            ue.courses.forEachIndexed { ci, course ->
                if (ci > 0) HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
                CourseRow(
                    index               = ci + 1,
                    entry               = course,
                    canDelete           = ue.courses.size > 1,
                    onMatiereChange     = { onCourseUpdate(ci, course.copy(matiere = it)) },
                    onNoteChange        = { onCourseUpdate(ci, course.copy(note = it)) },
                    onCoeffChange       = { onCourseUpdate(ci, course.copy(coefficient = it)) },
                    onDelete            = { onDeleteCourse(ci) },
                    onToggleComposite   = { onCourseUpdate(ci, course.copy(isComposite = !course.isComposite)) },
                    onSubGradeAdd       = { onSubGradeAdd(ci) },
                    onSubGradeLabelChange  = { si, v -> onSubGradeLabelChange(ci, si, v) },
                    onSubGradeNoteChange   = { si, v -> onSubGradeNoteChange(ci, si, v) },
                    onSubGradeDelete       = { si -> onSubGradeDelete(ci, si) }
                )
            }

            // ── Ajouter un cours ──────────────────────────────────────────────
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            TextButton(onClick = onAddCourse, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Ajouter un cours")
            }
        }
    }
}
