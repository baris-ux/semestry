package com.example.semestry.data

import java.util.UUID

data class SubGrade(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val note: String = ""
)

data class GradeEntry(
    val id: String = UUID.randomUUID().toString(),
    val matiere: String = "",
    val note: String = "",
    val coefficient: String = "",
    val isComposite: Boolean = false,
    val subGrades: List<SubGrade> = listOf(
        SubGrade(label = "CC"),
        SubGrade(label = "Partiel")
    )
)

enum class MoyenneType { ARITHMETIQUE, GEOMETRIQUE }

data class UE(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val moyenneType: MoyenneType = MoyenneType.ARITHMETIQUE,
    val courses: List<GradeEntry> = listOf(GradeEntry())
)

data class SavedSession(
    val name: String,
    val ues: List<UE>
)

data class Stats(val min: Double, val max: Double, val stdDev: Double)
