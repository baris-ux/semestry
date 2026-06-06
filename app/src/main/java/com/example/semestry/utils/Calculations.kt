package com.example.semestry.utils

import com.example.semestry.data.MoyenneType
import com.example.semestry.data.SubGrade
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

// Weighted average of sub-grades — returns null if any sub-grade is missing/invalid or geo zero
fun computeCompositeNote(subGrades: List<SubGrade>, type: MoyenneType = MoyenneType.ARITHMETIQUE): Double? {
    if (subGrades.isEmpty()) return null
    val parsed = subGrades.mapNotNull { sg ->
        val n = sg.note.replace(",", ".").toDoubleOrNull()?.takeIf { it in 0.0..20.0 }
        val w = sg.weight.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0 }
        if (n != null && w != null) n to w else null
    }
    if (parsed.size != subGrades.size) return null
    return when (type) {
        MoyenneType.ARITHMETIQUE ->
            parsed.sumOf { (n, w) -> n * w } / parsed.sumOf { (_, w) -> w }
        MoyenneType.GEOMETRIQUE ->
            if (parsed.any { (n, _) -> n == 0.0 }) null
            else parsed.fold(1.0) { acc, (n, w) -> acc * n.pow(w) }
                .pow(1.0 / parsed.sumOf { (_, w) -> w })
    }
}

// Arithmetic: (∑ n*c + min*thisCoeff) / ∑c = target  →  min = (target*∑c - ∑n*c) / thisCoeff
fun computeMinGrade(
    targetAvg: Double,
    thisCoeff: Double,
    otherFilled: List<Pair<Double, Double>>
): Double {
    val sumOtherProd  = otherFilled.sumOf { (n, c) -> n * c }
    val sumOtherCoeff = otherFilled.sumOf { (_, c) -> c }
    val totalCoeff    = sumOtherCoeff + thisCoeff
    return (targetAvg * totalCoeff - sumOtherProd) / thisCoeff
}

// Geometric: (∏ n_i^c_i * min^thisCoeff)^(1/∑c) = target  →  min = (target^∑c / ∏others)^(1/thisCoeff)
fun computeMinGradeGeometric(
    targetAvg: Double,
    thisCoeff: Double,
    otherFilled: List<Pair<Double, Double>>
): Double {
    val totalCoeff = otherFilled.sumOf { (_, c) -> c } + thisCoeff
    val prodOthers = otherFilled.fold(1.0) { acc, (n, c) -> acc * n.pow(c) }
    if (prodOthers <= 0.0) return 21.0
    return (targetAvg.pow(totalCoeff) / prodOthers).pow(1.0 / thisCoeff)
}
