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
import android.app.Activity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.semestry.data.GradeEntry
import com.example.semestry.data.MoyenneType
import com.example.semestry.data.SavedSession
import com.example.semestry.data.Stats
import com.example.semestry.data.SubGrade
import com.example.semestry.data.UE
import com.example.semestry.data.loadSessions
import com.example.semestry.data.saveSessions
import com.example.semestry.ads.RewardedAdManager
import com.example.semestry.ui.components.ResultCard
import com.example.semestry.ui.components.SessionsPanel
import com.example.semestry.ui.components.SimulationDialog
import com.example.semestry.ui.components.UECard
import com.example.semestry.utils.computeEffectiveNote
import com.example.semestry.utils.computeUEAverage
import com.example.semestry.utils.effectiveCoeff
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.math.sqrt

// ── Fonctions top-level ───────────────────────────────────────────────────────

private data class CalcState(val result: Double?, val stats: Stats?, val error: String?, val credits: Pair<Int, Int>? = null)

private fun computeCalcState(ues: List<UE>): CalcState {
    if (ues.isEmpty()) return CalcState(null, null, null)
    val parsed = mutableListOf<Pair<Double, Double>>()
    for (ue in ues) {
        if (ue.moyenneType == MoyenneType.GEOMETRIQUE) {
            val allFilled = ue.courses.all { computeEffectiveNote(it) != null }
            if (allFilled && ue.courses.any { computeEffectiveNote(it) == 0.0 }) {
                val nom = ue.name.ifBlank { "sans nom" }
                return CalcState(null, null,
                    "Moyenne géométrique indéfinie dans l'UE \"$nom\" : une note vaut 0.")
            }
        }
        val n = computeUEAverage(ue) ?: return CalcState(null, null, null)
        // UE coefficient = sum of course coefficients
        val c = ue.courses.sumOf { effectiveCoeff(it) ?: 0.0 }
        if (c <= 0.0) return CalcState(null, null, null)
        parsed.add(n to c)
    }
    val moyenne  = parsed.sumOf { (n, c) -> n * c } / parsed.sumOf { (_, c) -> c }
    val allNotes = ues.flatMap { ue -> ue.courses.mapNotNull { computeEffectiveNote(it) } }
    val mean     = allNotes.average()
    val variance = allNotes.sumOf { (it - mean).pow(2) } / allNotes.size

    val uesWithCoeffs = ues.filter { ue -> ue.courses.all { effectiveCoeff(it) != null } }
    val credits = if (uesWithCoeffs.isEmpty()) null else {
        val total    = uesWithCoeffs.sumOf { ue -> ue.courses.sumOf { effectiveCoeff(it) ?: 0.0 } }
        val obtained = uesWithCoeffs.filter { ue ->
            (computeUEAverage(ue) ?: 0.0) >= 10.0
        }.sumOf { ue -> ue.courses.sumOf { effectiveCoeff(it) ?: 0.0 } }
        (obtained.toInt()) to (total.toInt())
    }

    return CalcState(moyenne, Stats(allNotes.min(), allNotes.max(), sqrt(variance)), null, credits)
}

// ── Composable ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoyenneCalculatorScreen() {
    val context   = LocalContext.current
    val listState = rememberLazyListState()

    // ── State ──────────────────────────────────────────────────────────────────
    var ues              by remember { mutableStateOf(listOf(UE())) }
    var savedSessions    by remember { mutableStateOf(loadSessions(context)) }
    var showSaveDialog   by remember { mutableStateOf(false) }
    var sessionName      by remember { mutableStateOf("") }
    var showSessions     by remember { mutableStateOf(false) }
    var showSimulation   by remember { mutableStateOf(false) }
    var adNotReady       by remember { mutableStateOf(false) }

    val calcState = remember(ues) { computeCalcState(ues) }
    val result    = calcState.result
    val stats     = calcState.stats
    val liveError = calcState.error

    // ── Auto-scroll vers le résultat ───────────────────────────────────────────
    LaunchedEffect(result != null) {
        if (result != null) {
            delay(100L)
            val last = listState.layoutInfo.totalItemsCount - 1
            if (last >= 0) listState.animateScrollToItem(last)
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    fun updateUE(ui: Int, updated: UE) {
        ues = ues.toMutableList().also { it[ui] = updated }
    }
    fun updateCourse(ui: Int, ci: Int, updated: GradeEntry) {
        val ue = ues[ui]
        updateUE(ui, ue.copy(courses = ue.courses.toMutableList().also { it[ci] = updated }))
    }
    fun updateSubGrade(ui: Int, ci: Int, si: Int, updated: SubGrade) {
        val g = ues[ui].courses[ci]
        updateCourse(ui, ci, g.copy(subGrades = g.subGrades.toMutableList().also { it[si] = updated }))
    }

    // ── Dialogue simulation ────────────────────────────────────────────────────
    if (showSimulation) {
        SimulationDialog(ues = ues, onDismiss = { showSimulation = false })
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
                        val s = SavedSession(sessionName.trim(), ues)
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                AnimatedVisibility(
                    visible = showSessions,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    SessionsPanel(
                        sessions = savedSessions,
                        onLoad = { s ->
                            ues          = s.ues
                            showSessions = false
                        },
                        onDelete = { s ->
                            savedSessions = savedSessions - s
                            saveSessions(context, savedSessions)
                        }
                    )
                }
            }

            itemsIndexed(ues, key = { _, ue -> ue.id }) { ui, ue ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "UE ${ui + 1}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }
                UECard(
                    ue                   = ue,
                    canDelete            = ues.size > 1,
                    onNameChange         = { updateUE(ui, ue.copy(name = it)) },
                    onMoyenneTypeChange  = { updateUE(ui, ue.copy(moyenneType = it)) },
                    onDeleteUE           = { ues = ues.toMutableList().also { it.removeAt(ui) } },
                    onAddCourse          = { updateUE(ui, ue.copy(courses = ue.courses + GradeEntry())) },
                    onCourseUpdate       = { ci, updated -> updateCourse(ui, ci, updated) },
                    onDeleteCourse       = { ci ->
                        updateUE(ui, ue.copy(courses = ue.courses.toMutableList().also { it.removeAt(ci) }))
                    },
                    onSubGradeAdd        = { ci ->
                        updateCourse(ui, ci, ue.courses[ci].copy(
                            subGrades = ue.courses[ci].subGrades + SubGrade()
                        ))
                    },
                    onSubGradeLabelChange = { ci, si, v ->
                        updateSubGrade(ui, ci, si, ue.courses[ci].subGrades[si].copy(label = v))
                    },
                    onSubGradeNoteChange  = { ci, si, v ->
                        updateSubGrade(ui, ci, si, ue.courses[ci].subGrades[si].copy(note = v))
                    },
                    onSubGradeDelete      = { ci, si ->
                        updateCourse(ui, ci, ue.courses[ci].copy(
                            subGrades = ue.courses[ci].subGrades.toMutableList().also { it.removeAt(si) }
                        ))
                    }
                )
                } // end Column
            }

            item {
                OutlinedButton(
                    onClick = { ues = ues + UE() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajouter une UE")
                }
            }

            item {
                val activity = context as? Activity
                OutlinedButton(
                    onClick = {
                        adNotReady = false
                        if (!RewardedAdManager.isReady()) {
                            adNotReady = true
                            RewardedAdManager.load(context)
                            return@OutlinedButton
                        }
                        activity?.let {
                            RewardedAdManager.show(
                                activity   = it,
                                onRewarded = { showSimulation = true },
                                onDismissed = {}
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simuler ma note cible")
                    Spacer(modifier = Modifier.width(6.dp))
                    androidx.compose.material3.Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            "PUB",
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                if (adNotReady) {
                    Text(
                        "Pub en cours de chargement, réessayez dans quelques secondes.",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            }

            if (liveError != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
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

            if (result != null) {
                item { ResultCard(result = result, label = "Moyenne pondérée", stats = stats, credits = calcState.credits) }
            }
        }
    }
}
