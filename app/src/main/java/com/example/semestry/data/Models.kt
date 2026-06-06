package com.example.semestry.data

import java.util.UUID

data class GradeEntry(
    val id: String = UUID.randomUUID().toString(),
    val matiere: String = "",
    val note: String = "",
    val coefficient: String = "1"
)

data class CourseBlock(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val grades: List<GradeEntry> = listOf(GradeEntry()),
    val isExpanded: Boolean = true
)

enum class MoyenneType { ARITHMETIQUE, GEOMETRIQUE }

data class SavedSession(
    val name: String,
    val type: MoyenneType,
    val blocks: List<CourseBlock>,
    val targetAverage: String = "10"
)

data class Stats(val min: Double, val max: Double, val stdDev: Double)
