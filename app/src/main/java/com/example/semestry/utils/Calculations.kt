package com.example.semestry.utils

import com.example.semestry.data.GradeEntry
import com.example.semestry.data.MoyenneType
import com.example.semestry.data.SubGrade
import com.example.semestry.data.UE
import kotlin.math.pow

fun noteError(note: String): String? {
    if (note.isEmpty()) return null
    val v = note.replace(",", ".").toDoubleOrNull() ?: return "Note invalide"
    return if (v < 0 || v > 20) "Entre 0 et 20" else null
}

fun coeffError(coeff: String): String? {
    if (coeff.isEmpty()) return null
    val v = coeff.replace(",", ".").toDoubleOrNull() ?: return "Invalide"
    return if (v <= 0) "Doit être > 0" else null
}

fun effectiveCoeff(g: GradeEntry): Double? =
    g.coefficient.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0 }

// Simple arithmetic average of sub-grades (equal weight)
fun computeCompositeNote(subGrades: List<SubGrade>): Double? {
    if (subGrades.isEmpty()) return null
    val notes = subGrades.mapNotNull { sg ->
        sg.note.replace(",", ".").toDoubleOrNull()?.takeIf { it in 0.0..20.0 }
    }
    if (notes.size != subGrades.size) return null
    return notes.average()
}

fun computeEffectiveNote(entry: GradeEntry): Double? =
    if (entry.isComposite) computeCompositeNote(entry.subGrades)
    else entry.note.replace(",", ".").toDoubleOrNull()?.takeIf { it in 0.0..20.0 }

// Weighted average of all courses in a UE — null if any course is unfilled or geo-zero.
fun computeUEAverage(ue: UE): Double? {
    if (ue.courses.isEmpty()) return null
    val parsed = mutableListOf<Pair<Double, Double>>()
    for (course in ue.courses) {
        val n = computeEffectiveNote(course) ?: return null
        val c = effectiveCoeff(course) ?: return null
        parsed.add(n to c)
    }
    val totalCoeff = parsed.sumOf { (_, c) -> c }
    if (totalCoeff <= 0.0) return null
    return when (ue.moyenneType) {
        MoyenneType.ARITHMETIQUE ->
            parsed.sumOf { (n, c) -> n * c } / totalCoeff
        MoyenneType.GEOMETRIQUE ->
            if (parsed.any { (n, _) -> n == 0.0 }) null
            else parsed.fold(1.0) { acc, (n, c) -> acc * n.pow(c) }.pow(1.0 / totalCoeff)
    }
}
