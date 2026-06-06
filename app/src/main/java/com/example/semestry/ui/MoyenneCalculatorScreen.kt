package com.example.semestry.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.semestry.data.GradeEntry
import com.example.semestry.data.MoyenneType
import com.example.semestry.data.SavedSession
import com.example.semestry.data.Stats
import com.example.semestry.data.SubGrade
import com.example.semestry.data.loadSessions
import com.example.semestry.data.saveSessions
import com.example.semestry.ui.components.CourseRow
import com.example.semestry.ui.components.ResultCard
import com.example.semestry.ui.components.SessionsPanel
import com.example.semestry.utils.computeCompositeNote
import com.example.semestry.utils.computeMinGrade
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoyenneCalculatorScreen() {
    val context   = LocalContext.current
    val listState = rememberLazyListState()

    // ── State ──────────────────────────────────────────────────────────────────
    var grades        by remember { mutableStateOf(listOf(GradeEntry())) }
    var targetAverage by remember { mutableStateOf("10") }
    var savedSessions by remember { mutableStateOf(loadSessions(context)) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var sessionName    by remember { mutableStateOf("") }
    var showSessions   by remember { mutableStateOf(false) }

    // ── Valeurs dérivées ───────────────────────────────────────────────────────
    val targetAvgParsed: Double? =
        targetAverage.replace(",", ".").toDoubleOrNull()?.takeIf { it in 0.0..20.0 }

    // Coefficient effectif : somme des poids pour un cours composite, coeff manuel sinon
    fun effectiveCoeff(g: GradeEntry): Double? = if (g.isComposite)
        g.subGrades.sumOf { sg -> sg.weight.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0 } ?: 0.0 }
            .takeIf { it > 0 }
    else
        g.coefficient.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0 }

    val allFilledParsed: Map<String, Pair<Double, Double>> = grades.mapNotNull { g ->
        val n = if (g.isComposite) computeCompositeNote(g.subGrades, g.moyenneType)
                else g.note.replace(",", ".").toDoubleOrNull()?.takeIf { it in 0.0..20.0 }
        val c = effectiveCoeff(g)
        if (n != null && c != null) g.id to (n to c) else null
    }.toMap()

    // Note min pour chaque cours vide (mode simple uniquement)
    val minGradeMap: Map<String, Double?> = grades
        .filter { g -> !g.isComposite && g.note.isEmpty() }
        .associate { g ->
            val thisCoeff = effectiveCoeff(g)
            val others    = allFilledParsed.filterKeys { it != g.id }.values.toList()
            val min = if (thisCoeff != null && targetAvgParsed != null)
                computeMinGrade(targetAvgParsed, thisCoeff, others) else null
            g.id to min
        }

    // ── Calcul réactif ─────────────────────────────────────────────────────────
    var result: Double?    = null
    var stats: Stats?      = null
    var liveError: String? = null

    if (grades.isNotEmpty()) {
        val parsed = mutableListOf<Pair<Double, Double>>()
        var complete = true
        for (g in grades) {
            val c = effectiveCoeff(g)
            val n: Double? = if (g.isComposite) {
                // Détecter zéro géométrique avant computeCompositeNote
                if (g.moyenneType == MoyenneType.GEOMETRIQUE &&
                    g.subGrades.all { sg ->
                        sg.note.replace(",", ".").toDoubleOrNull()?.let { it in 0.0..20.0 } != null &&
                        sg.weight.replace(",", ".").toDoubleOrNull()?.let { it > 0 } != null
                    } &&
                    g.subGrades.any { sg -> sg.note.replace(",", ".").toDoubleOrNull() == 0.0 }
                ) {
                    val nom = g.matiere.ifBlank { "sans nom" }
                    liveError = "Moyenne géométrique indéfinie dans \"$nom\" : une note partielle vaut 0."
                    complete = false; break
                }
                computeCompositeNote(g.subGrades, g.moyenneType)
            } else g.note.replace(",", ".").toDoubleOrNull()?.takeIf { it in 0.0..20.0 }
            if (n == null || c == null) { complete = false; break }
            parsed.add(n to c)
        }
        if (complete) {
            val moyenne  = parsed.sumOf { (n, c) -> n * c } / parsed.sumOf { (_, c) -> c }
            val notes    = parsed.map { it.first }
            val mean     = notes.average()
            val variance = notes.sumOf { (it - mean).pow(2) } / notes.size
            result = moyenne
            stats  = Stats(notes.min(), notes.max(), sqrt(variance))
        }
    }

    // ── Auto-scroll vers le résultat (quand il apparaît) ──────────────────────
    val resultVisible = result != null
    LaunchedEffect(resultVisible) {
        if (resultVisible) {
            delay(100L)
            val last = listState.layoutInfo.totalItemsCount - 1
            if (last >= 0) listState.animateScrollToItem(last)
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    fun updateGrade(gi: Int, updated: GradeEntry) {
        grades = grades.toMutableList().also { it[gi] = updated }
    }
    fun updateSubGrade(gi: Int, si: Int, updated: SubGrade) {
        val g = grades[gi]
        updateGrade(gi, g.copy(subGrades = g.subGrades.toMutableList().also { it[si] = updated }))
    }

    // ── Dialogue de sauvegarde ─────────────────────────────────────────────────
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
                        val s = SavedSession(sessionName.trim(), grades, targetAverage)
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
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Panneau des sessions
            item {
                AnimatedVisibility(
                    visible = showSessions,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    SessionsPanel(
                        sessions = savedSessions,
                        onLoad = { s ->
                            grades        = s.grades
                            targetAverage = s.targetAverage
                            showSessions  = false
                        },
                        onDelete = { s ->
                            savedSessions = savedSessions - s
                            saveSessions(context, savedSessions)
                        }
                    )
                }
            }

            // Objectif
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.width(130.dp)
                    )
                }
            }

            // Cours
            itemsIndexed(grades) { gi, grade ->
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        CourseRow(
                            index           = gi + 1,
                            entry           = grade,
                            showCoeff       = grades.size > 1,
                            canDelete       = grades.size > 1,
                            minGrade        = minGradeMap[grade.id],
                            targetAvgParsed = targetAvgParsed,
                            onMatiereChange = { updateGrade(gi, grade.copy(matiere = it)) },
                            onNoteChange    = { updateGrade(gi, grade.copy(note = it)) },
                            onCoeffChange   = { updateGrade(gi, grade.copy(coefficient = it)) },
                            onDelete        = { grades = grades.toMutableList().also { it.removeAt(gi) } },
                            onToggleComposite = { updateGrade(gi, grade.copy(isComposite = !grade.isComposite)) },
                            onMoyenneTypeChange = { updateGrade(gi, grade.copy(moyenneType = it)) },
                            onSubGradeAdd   = { updateGrade(gi, grade.copy(subGrades = grade.subGrades + SubGrade())) },
                            onSubGradeLabelChange  = { si, v -> updateSubGrade(gi, si, grade.subGrades[si].copy(label = v)) },
                            onSubGradeNoteChange   = { si, v -> updateSubGrade(gi, si, grade.subGrades[si].copy(note = v)) },
                            onSubGradeWeightChange = { si, v -> updateSubGrade(gi, si, grade.subGrades[si].copy(weight = v)) },
                            onSubGradeDelete       = { si ->
                                updateGrade(gi, grade.copy(
                                    subGrades = grade.subGrades.toMutableList().also { it.removeAt(si) }
                                ))
                            }
                        )
                    }
                }
            }

            // Bouton ajouter un cours
            item {
                OutlinedButton(
                    onClick = { grades = grades + GradeEntry() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajouter un cours")
                }
            }

            // Erreur (ex: note 0 en géométrique)
            if (liveError != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            liveError,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Résultat
            if (result != null) {
                item { ResultCard(result = result, label = "Moyenne pondérée", stats = stats) }
            }
        }
    }
}
