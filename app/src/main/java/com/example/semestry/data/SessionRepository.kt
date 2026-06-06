package com.example.semestry.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

fun saveSessions(context: Context, sessions: List<SavedSession>) {
    val arr = JSONArray()
    sessions.forEach { s ->
        arr.put(JSONObject().apply {
            put("name", s.name)
            put("targetAverage", s.targetAverage)
            put("grades", JSONArray().also { ga ->
                s.grades.forEach { g ->
                    ga.put(JSONObject().apply {
                        put("id", g.id)
                        put("matiere", g.matiere)
                        put("note", g.note)
                        put("coefficient", g.coefficient)
                        put("isComposite", g.isComposite)
                        put("moyenneType", g.moyenneType.name)
                        put("subGrades", JSONArray().also { sa ->
                            g.subGrades.forEach { sg ->
                                sa.put(JSONObject().apply {
                                    put("id", sg.id)
                                    put("label", sg.label)
                                    put("note", sg.note)
                                    put("weight", sg.weight)
                                })
                            }
                        })
                    })
                }
            })
        })
    }
    context.getSharedPreferences("semestry", Context.MODE_PRIVATE)
        .edit().putString("sessions_v3", arr.toString()).apply()
}

fun loadSessions(context: Context): List<SavedSession> {
    val raw = context.getSharedPreferences("semestry", Context.MODE_PRIVATE)
        .getString("sessions_v3", null) ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val ga  = obj.getJSONArray("grades")
            SavedSession(
                name          = obj.getString("name"),
                targetAverage = obj.optString("targetAverage", "10"),
                grades = (0 until ga.length()).map { j ->
                    val g  = ga.getJSONObject(j)
                    val sa = g.optJSONArray("subGrades")
                    GradeEntry(
                        id          = g.optString("id", UUID.randomUUID().toString()),
                        matiere     = g.getString("matiere"),
                        note        = g.getString("note"),
                        coefficient = g.getString("coefficient"),
                        isComposite = g.optBoolean("isComposite", false),
                        moyenneType = MoyenneType.valueOf(g.optString("moyenneType", "ARITHMETIQUE")),
                        subGrades   = if (sa != null) {
                            (0 until sa.length()).map { si ->
                                val sg = sa.getJSONObject(si)
                                SubGrade(
                                    id     = sg.optString("id", UUID.randomUUID().toString()),
                                    label  = sg.optString("label", ""),
                                    note   = sg.optString("note", ""),
                                    weight = sg.optString("weight", "1")
                                )
                            }
                        } else listOf(SubGrade(label = "CC"), SubGrade(label = "Partiel", weight = "2"))
                    )
                }
            )
        }
    } catch (_: Exception) { emptyList() }
}
