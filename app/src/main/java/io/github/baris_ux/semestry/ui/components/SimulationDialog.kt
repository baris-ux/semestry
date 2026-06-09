package io.github.baris_ux.semestry.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.baris_ux.semestry.data.UE
import io.github.baris_ux.semestry.utils.simulateNeededGrade

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationDialog(ues: List<UE>, onDismiss: () -> Unit) {
    data class CourseEntry(val label: String, val ueIndex: Int, val courseIndex: Int)

    val courses = remember(ues) {
        ues.flatMapIndexed { ui, ue ->
            ue.courses.mapIndexed { ci, course ->
                val ueName     = ue.name.ifBlank { "UE ${ui + 1}" }
                val courseName = course.matiere.ifBlank { "Cours ${ci + 1}" }
                CourseEntry("$ueName — $courseName", ui, ci)
            }
        }
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    var expanded      by remember { mutableStateOf(false) }
    var targetText    by remember { mutableStateOf("10") }

    val targetAvg = targetText.replace(",", ".").toDoubleOrNull()?.takeIf { it in 0.0..20.0 }
    val entry     = courses.getOrNull(selectedIndex)
    val needed    = if (targetAvg != null && entry != null)
        simulateNeededGrade(ues, entry.ueIndex, entry.courseIndex, targetAvg)
    else null

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Simulation de note cible") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Quelle note te faut-il dans ce cours pour atteindre ta moyenne cible ?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value      = entry?.label ?: "",
                        onValueChange = {},
                        readOnly   = true,
                        label      = { Text("Cours à simuler") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        shape      = RoundedCornerShape(10.dp),
                        modifier   = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        courses.forEachIndexed { idx, c ->
                            DropdownMenuItem(
                                text    = { Text(c.label) },
                                onClick = { selectedIndex = idx; expanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value         = targetText,
                    onValueChange = { targetText = it },
                    label         = { Text("Moyenne cible (/20)") },
                    placeholder   = { Text("ex: 12") },
                    singleLine    = true,
                    shape         = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier      = Modifier.fillMaxWidth()
                )

                if (targetAvg != null) {
                    when {
                        needed == null -> Text(
                            "Remplis toutes les autres notes pour simuler.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        needed > 20.0 -> Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                "Objectif impossible — il faudrait plus de 20/20.",
                                modifier = Modifier.padding(10.dp),
                                color    = MaterialTheme.colorScheme.onErrorContainer,
                                style    = MaterialTheme.typography.bodyMedium
                            )
                        }
                        needed < 0.0 -> Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                "Objectif déjà atteint même avec 0 dans ce cours !",
                                modifier = Modifier.padding(10.dp),
                                color    = MaterialTheme.colorScheme.onTertiaryContainer,
                                style    = MaterialTheme.typography.bodyMedium
                            )
                        }
                        else -> Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "Il te faut %.2f / 20 dans ce cours.".format(needed),
                                modifier = Modifier.padding(10.dp),
                                color    = MaterialTheme.colorScheme.onPrimaryContainer,
                                style    = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}
