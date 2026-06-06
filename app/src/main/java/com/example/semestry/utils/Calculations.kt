package com.example.semestry.utils

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

// Returns the minimum note needed in THIS course to reach targetAvg,
// given all other already-filled (note, coeff) pairs.
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
