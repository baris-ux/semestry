package io.github.baris_ux.semestry.utils

import io.github.baris_ux.semestry.data.GradeEntry
import io.github.baris_ux.semestry.data.MoyenneType
import io.github.baris_ux.semestry.data.SubGrade
import io.github.baris_ux.semestry.data.UE
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

// Grade needed in one course to reach a target semester average — null if other grades are missing.
fun simulateNeededGrade(ues: List<UE>, targetUeIndex: Int, targetCourseIndex: Int, targetAvg: Double): Double? {
    val targetUE      = ues.getOrNull(targetUeIndex) ?: return null
    val targetCourse  = targetUE.courses.getOrNull(targetCourseIndex) ?: return null
    val targetCoeff   = effectiveCoeff(targetCourse) ?: return null

    val totalCoeff = ues.sumOf { ue -> ue.courses.sumOf { effectiveCoeff(it) ?: 0.0 } }
    if (totalCoeff <= 0) return null

    var otherUEsWeightedSum = 0.0
    for ((i, ue) in ues.withIndex()) {
        if (i == targetUeIndex) continue
        val avg   = computeUEAverage(ue) ?: return null
        val coeff = ue.courses.sumOf { effectiveCoeff(it) ?: 0.0 }
        otherUEsWeightedSum += avg * coeff
    }

    return when (targetUE.moyenneType) {
        MoyenneType.ARITHMETIQUE -> {
            var otherCoursesSum = 0.0
            for ((j, course) in targetUE.courses.withIndex()) {
                if (j == targetCourseIndex) continue
                val note  = computeEffectiveNote(course) ?: return null
                val coeff = effectiveCoeff(course) ?: return null
                otherCoursesSum += note * coeff
            }
            (targetAvg * totalCoeff - otherUEsWeightedSum - otherCoursesSum) / targetCoeff
        }
        MoyenneType.GEOMETRIQUE -> {
            val ueCoeff = targetUE.courses.sumOf { effectiveCoeff(it) ?: 0.0 }
            if (ueCoeff <= 0) return null
            var otherProduct = 1.0
            for ((j, course) in targetUE.courses.withIndex()) {
                if (j == targetCourseIndex) continue
                val note  = computeEffectiveNote(course) ?: return null
                val coeff = effectiveCoeff(course) ?: return null
                if (note <= 0) return null
                otherProduct *= note.pow(coeff)
            }
            val ueAvgTarget = (targetAvg * totalCoeff - otherUEsWeightedSum) / ueCoeff
            if (ueAvgTarget <= 0) return null
            (ueAvgTarget.pow(ueCoeff) / otherProduct).pow(1.0 / targetCoeff)
        }
    }
}

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
