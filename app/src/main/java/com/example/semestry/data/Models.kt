package com.example.semestry.data

import java.util.UUID

data class SubGrade(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val note: String = "",
    val weight: String = "1"
)

data class GradeEntry(
    val id: String = UUID.randomUUID().toString(),
    val matiere: String = "",
    val note: String = "",
    val coefficient: String = "1",
    val isComposite: Boolean = false,
    val subGrades: List<SubGrade> = listOf(
        SubGrade(label = "CC"),
        SubGrade(label = "Partiel", weight = "2")
    ),
    val moyenneType: MoyenneType = MoyenneType.ARITHMETIQUE
)

enum class MoyenneType { ARITHMETIQUE, GEOMETRIQUE }

data class SavedSession(
    val name: String,
    val grades: List<GradeEntry>,
    val targetAverage: String = "10"
)

data class Stats(val min: Double, val max: Double, val stdDev: Double)
