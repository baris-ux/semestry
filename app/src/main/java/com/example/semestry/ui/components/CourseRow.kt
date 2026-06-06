package com.example.semestry.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.semestry.data.GradeEntry
import com.example.semestry.data.SubGrade
import com.example.semestry.utils.coeffError
import com.example.semestry.utils.computeCompositeNote
import com.example.semestry.utils.noteError

private fun gradeColor(value: Double): Color = when {
    value >= 16 -> Color(0xFF2E7D32)
    value >= 14 -> Color(0xFF388E3C)
    value >= 12 -> Color(0xFF689F38)
    value >= 10 -> Color(0xFFF57C00)
    else        -> Color(0xFFC62828)
}

@Composable
fun CourseRow(
    index: Int,
    entry: GradeEntry,
    showCoeff: Boolean,
    canDelete: Boolean,
    minGrade: Double?,
    targetAvgParsed: Double?,
    onMatiereChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onCoeffChange: (String) -> Unit,
    onDelete: () -> Unit,
    onToggleComposite: () -> Unit,
    onSubGradeAdd: () -> Unit,
    onSubGradeLabelChange: (Int, String) -> Unit,
    onSubGradeNoteChange: (Int, String) -> Unit,
    onSubGradeWeightChange: (Int, String) -> Unit,
    onSubGradeDelete: (Int) -> Unit
) {
    val nErr = if (!entry.isComposite) noteError(entry.note) else null
    val cErr = if (showCoeff) coeffError(entry.coefficient) else null

    val validNote = !entry.isComposite && entry.note.isNotEmpty() && nErr == null
    val noteValue = if (validNote) entry.note.replace(",", ".").toDoubleOrNull() else null
    val dotColor  = noteValue?.let { gradeColor(it) }

    val compositeNote = if (entry.isComposite) computeCompositeNote(entry.subGrades) else null

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

        // ── Nom du cours + toggle composite + supprimer ───────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = entry.matiere,
                onValueChange = onMatiereChange,
                label = { Text("Cours $index") },
                placeholder = { Text("ex: Maths") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onToggleComposite) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = if (entry.isComposite) "Mode simple" else "Mode composite",
                    tint = if (entry.isComposite) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
            IconButton(onClick = onDelete, enabled = canDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = if (canDelete) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        // ── Mode simple : note + coeff ────────────────────────────────────────
        AnimatedVisibility(
            visible = !entry.isComposite,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = entry.note,
                        onValueChange = { if (it.length <= 5) onNoteChange(it) },
                        label = { Text("Note") },
                        suffix = { Text("/20") },
                        isError = nErr != null,
                        supportingText = if (nErr != null) ({ Text(nErr) }) else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = if (dotColor != null) {
                            { Box(Modifier.size(10.dp).background(dotColor, CircleShape)) }
                        } else null,
                        modifier = Modifier.weight(2f)
                    )
                    AnimatedVisibility(
                        visible = showCoeff,
                        enter = expandHorizontally(),
                        exit = shrinkHorizontally(),
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = entry.coefficient,
                            onValueChange = { if (it.length <= 5) onCoeffChange(it) },
                            label = { Text("Coeff.") },
                            isError = cErr != null,
                            supportingText = if (cErr != null) ({ Text(cErr) }) else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Indice note minimale
                if (entry.note.isEmpty() && minGrade != null && targetAvgParsed != null) {
                    val errorColor   = MaterialTheme.colorScheme.error
                    val successColor = Color(0xFF2E7D32)
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val (hintText, hintColor) = when {
                        minGrade > 20 ->
                            "Objectif %.1f/20 impossible dans ce cours (> 20)".format(targetAvgParsed) to errorColor
                        minGrade < 0  ->
                            "Objectif %.1f/20 déjà atteint, même avec 0/20 ✓".format(targetAvgParsed) to successColor
                        else          ->
                            "Note min. pour %.1f/20 : %.2f / 20".format(targetAvgParsed, minGrade) to primaryColor
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = hintColor.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "💡 $hintText",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = hintColor,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ── Mode composite : épreuves ─────────────────────────────────────────
        AnimatedVisibility(
            visible = entry.isComposite,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // En-tête des colonnes
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        Text(
                            "Épreuve",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(2f)
                        )
                        Text(
                            "Note /20",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(2f)
                        )
                        Text(
                            "Pond.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(40.dp))
                    }

                    entry.subGrades.forEachIndexed { si, sg ->
                        if (si > 0) HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        SubGradeRow(
                            sg        = sg,
                            canDelete = entry.subGrades.size > 1,
                            onLabelChange  = { onSubGradeLabelChange(si, it) },
                            onNoteChange   = { onSubGradeNoteChange(si, it) },
                            onWeightChange = { onSubGradeWeightChange(si, it) },
                            onDelete       = { onSubGradeDelete(si) }
                        )
                    }

                    TextButton(
                        onClick = onSubGradeAdd,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ajouter une épreuve", fontSize = 13.sp)
                    }

                    // Note calculée
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Note calculée",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val displayColor = compositeNote?.let { gradeColor(it) }
                            ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (compositeNote != null) {
                                Box(Modifier.size(8.dp).background(displayColor, CircleShape))
                            }
                            Text(
                                text = if (compositeNote != null) "%.2f / 20".format(compositeNote) else "—",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = displayColor
                            )
                        }
                    }
                }
            }
        }

        // ── Coefficient dans le bloc (composite) ──────────────────────────────
        AnimatedVisibility(
            visible = entry.isComposite && showCoeff,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            OutlinedTextField(
                value = entry.coefficient,
                onValueChange = { if (it.length <= 5) onCoeffChange(it) },
                label = { Text("Coeff. dans le bloc") },
                isError = cErr != null,
                supportingText = if (cErr != null) ({ Text(cErr) }) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.width(180.dp)
            )
        }
    }
}

@Composable
private fun SubGradeRow(
    sg: SubGrade,
    canDelete: Boolean,
    onLabelChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val nErr = noteError(sg.note)
    val wErr = coeffError(sg.weight)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        OutlinedTextField(
            value = sg.label,
            onValueChange = onLabelChange,
            placeholder = { Text("ex: CC") },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(2f)
        )
        OutlinedTextField(
            value = sg.note,
            onValueChange = { if (it.length <= 5) onNoteChange(it) },
            suffix = { Text("/20") },
            isError = nErr != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(2f)
        )
        OutlinedTextField(
            value = sg.weight,
            onValueChange = { if (it.length <= 5) onWeightChange(it) },
            isError = wErr != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onDelete,
            enabled = canDelete,
            modifier = Modifier.padding(top = 4.dp).size(40.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Supprimer l'épreuve",
                tint = if (canDelete) MaterialTheme.colorScheme.error
                       else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
