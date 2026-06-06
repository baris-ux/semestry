package com.example.semestry.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.semestry.data.CourseBlock
import com.example.semestry.data.GradeEntry
import com.example.semestry.data.MoyenneType
import com.example.semestry.data.SavedSession
import com.example.semestry.data.Stats
import com.example.semestry.data.loadSessions
import com.example.semestry.data.saveSessions
import com.example.semestry.ui.components.BlockCard
import com.example.semestry.ui.components.MoyenneTypeSelector
import com.example.semestry.ui.components.ResultCard
import com.example.semestry.ui.components.SessionsPanel
import com.example.semestry.utils.computeMinGrade
import kotlin.math.pow
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoyenneCalculatorScreen() {
    val context = LocalContext.current

    // ── State ──────────────────────────────────────────────────────────────────
    var moyenneType    by remember { mutableStateOf(MoyenneType.ARITHMETIQUE) }
    var blocks         by remember { mutableStateOf(listOf(CourseBlock(name = "Bloc 1"))) }
    var targetAverage  by remember { mutableStateOf("10") }
    var result         by remember { mutableStateOf<Double?>(null) }
    var stats          by remember { mutableStateOf<Stats?>(null) }
    var errorMessage   by remember { mutableStateOf<String?>(null) }
    var savedSessions  by remember { mutableStateOf(loadSessions(context)) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var sessionName    by remember { mutableStateOf("") }
    var showSessions   by remember { mutableStateOf(false) }

    // ── Derived values ─────────────────────────────────────────────────────────
    val targetAvgParsed: Double? =
        targetAverage.replace(",", ".").toDoubleOrNull()?.takeIf { it in 0.0..20.0 }

    val allFilledParsed: Map<String, Pair<Double, Double>> =
        blocks.flatMap { it.grades }.mapNotNull { g ->
            val n = g.note.replace(",", ".").toDoubleOrNull()?.takeIf { it in 0.0..20.0 }
            val c = g.coefficient.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0 }
            if (n != null && c != null) g.id to (n to c) else null
        }.toMap()

    val minGradeMap: Map<String, Double?> =
        blocks.flatMap { it.grades }.filter { it.note.isEmpty() }.associate { g ->
            val thisCoeff = g.coefficient.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0 }
            val others    = allFilledParsed.filterKeys { it != g.id }.values.toList()
            val min       = if (thisCoeff != null && targetAvgParsed != null)
                                computeMinGrade(targetAvgParsed, thisCoeff, others) else null
            g.id to min
        }

    // ── Block helpers ──────────────────────────────────────────────────────────
    fun updateBlock(bi: Int, updated: CourseBlock) {
        blocks = blocks.toMutableList().also { it[bi] = updated }
    }
    fun updateGrade(bi: Int, gi: Int, updated: GradeEntry) {
        val b = blocks[bi]
        updateBlock(bi, b.copy(grades = b.grades.toMutableList().also { it[gi] = updated }))
    }

    // ── Calculate ──────────────────────────────────────────────────────────────
    fun calculate() {
        errorMessage = null; result = null; stats = null
        val all = blocks.flatMap { it.grades }
        if (all.isEmpty()) { errorMessage = "Ajoutez au moins un cours."; return }
        val parsed = mutableListOf<Pair<Double, Double>>()
        for (g in all) {
            val n = g.note.replace(",", ".").toDoubleOrNull()
            val c = g.coefficient.replace(",", ".").toDoubleOrNull()
            if (n == null || c == null) { errorMessage = "Remplissez toutes les notes et coefficients."; return }
            if (n < 0 || n > 20)       { errorMessage = "Les notes doivent être entre 0 et 20."; return }
            if (c <= 0)                { errorMessage = "Les coefficients doivent être > 0."; return }
            parsed.add(n to c)
        }
        val moyenne = when (moyenneType) {
            MoyenneType.ARITHMETIQUE ->
                parsed.sumOf { (n, c) -> n * c } / parsed.sumOf { (_, c) -> c }
            MoyenneType.GEOMETRIQUE  -> {
                if (parsed.any { (n, _) -> n == 0.0 }) {
                    errorMessage = "Moyenne géométrique indéfinie : une note vaut 0."; return
                }
                parsed.fold(1.0) { acc, (n, _) -> acc * n }.pow(1.0 / parsed.size)
            }
        }
        val notes    = parsed.map { it.first }
        val mean     = notes.average()
        val variance = notes.sumOf { (it - mean).pow(2) } / notes.size
        stats  = Stats(notes.min(), notes.max(), sqrt(variance))
        result = moyenne
    }

    // ── Save dialog ────────────────────────────────────────────────────────────
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false; sessionName = "" },
            title = { Text("Sauvegarder le semestre") },
            text = {
                OutlinedTextField(
                    value = sessionName,
                    onValueChange = { sessionName = it },
                    label = { Text("Nom du semestre") },
                    placeholder = { Text("ex: S1 Informatique 2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (sessionName.isNotBlank()) {
                        val s = SavedSession(sessionName.trim(), moyenneType, blocks, targetAverage)
                        savedSessions = savedSessions.filter { it.name != s.name } + s
                        saveSessions(context, savedSessions)
                        showSaveDialog = false; sessionName = ""
                    }
                }) { Text("Sauvegarder") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false; sessionName = "" }) { Text("Annuler") }
            }
        )
    }

    // ── UI ─────────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Semestry", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            "Calculateur de moyenne", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSessions = !showSessions }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Mes semestres")
                    }
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Default.Save, contentDescription = "Sauvegarder")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Sessions panel
            item {
                AnimatedVisibility(
                    visible = showSessions,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    SessionsPanel(
                        sessions = savedSessions,
                        onLoad = { s ->
                            moyenneType   = s.type
                            blocks        = s.blocks
                            targetAverage = s.targetAverage
                            result = null; stats = null; errorMessage = null
                            showSessions = false
                        },
                        onDelete = { s ->
                            savedSessions = savedSessions - s
                            saveSessions(context, savedSessions)
                        }
                    )
                }
            }

            // Type selector
            item {
                MoyenneTypeSelector(selected = moyenneType, onSelect = {
                    moyenneType = it; result = null; errorMessage = null
                })
            }

            // Description
            item {
                AnimatedContent(targetState = moyenneType, label = "desc") { type ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = when (type) {
                                MoyenneType.ARITHMETIQUE ->
                                    "Moyenne pondérée : somme(note × coeff) ÷ somme(coeff)."
                                MoyenneType.GEOMETRIQUE  ->
                                    "Racine n-ième du produit de toutes les notes (coefficients ignorés)."
                            },
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Target average
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Objectif", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Affiche la note min. requise par cours.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    OutlinedTextField(
                        value = targetAverage,
                        onValueChange = { if (it.length <= 5) targetAverage = it },
                        label = { Text("Cible") },
                        suffix = { Text("/20") },
                        isError = targetAvgParsed == null && targetAverage.isNotEmpty(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.width(130.dp)
                    )
                }
            }

            // Course blocks
            itemsIndexed(blocks) { bi, block ->
                BlockCard(
                    block            = block,
                    showCoeff        = moyenneType == MoyenneType.ARITHMETIQUE,
                    canDeleteBlock   = blocks.size > 1,
                    minGradeMap      = minGradeMap,
                    targetAvgParsed  = targetAvgParsed,
                    onBlockNameChange = { updateBlock(bi, block.copy(name = it)) },
                    onToggleExpand    = { updateBlock(bi, block.copy(isExpanded = !block.isExpanded)) },
                    onDeleteBlock     = {
                        blocks = blocks.toMutableList().also { it.removeAt(bi) }
                        result = null
                    },
                    onAddGrade    = { updateBlock(bi, block.copy(grades = block.grades + GradeEntry())) },
                    onMatiereChange = { gi, v -> updateGrade(bi, gi, block.grades[gi].copy(matiere = v)) },
                    onNoteChange    = { gi, v -> updateGrade(bi, gi, block.grades[gi].copy(note = v)); result = null },
                    onCoeffChange   = { gi, v -> updateGrade(bi, gi, block.grades[gi].copy(coefficient = v)); result = null },
                    onDeleteGrade   = { gi ->
                        updateBlock(bi, block.copy(
                            grades = block.grades.toMutableList().also { it.removeAt(gi) }
                        ))
                        result = null
                    }
                )
            }

            // Add block
            item {
                OutlinedButton(
                    onClick = { blocks = blocks + CourseBlock(name = "Bloc ${blocks.size + 1}") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajouter un bloc")
                }
            }

            // Error
            if (errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            errorMessage!!,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { calculate() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Calculer la moyenne", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (result != null) {
                item { ResultCard(result = result!!, type = moyenneType, stats = stats) }
            }
        }
    }
}
