package com.example.semestry.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.semestry.data.GradeEntry
import com.example.semestry.utils.coeffError
import com.example.semestry.utils.noteError

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
    onDelete: () -> Unit
) {
    val nErr = noteError(entry.note)
    val cErr = if (showCoeff) coeffError(entry.coefficient) else null

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Subject name + delete button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            IconButton(onClick = onDelete, enabled = canDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = if (canDelete) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        // Note + coefficient
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

        // Minimum grade hint (only when note field is empty)
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
                    text = "💡 $hintText",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = hintColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
