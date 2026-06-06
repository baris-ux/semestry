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
            put("type", s.type.name)
            put("targetAverage", s.targetAverage)
            put("blocks", JSONArray().also { ba ->
                s.blocks.forEach { b ->
                    ba.put(JSONObject().apply {
                        put("id", b.id)
                        put("name", b.name)
                        put("isExpanded", b.isExpanded)
                        put("grades", JSONArray().also { ga ->
                            b.grades.forEach { g ->
                                ga.put(JSONObject().apply {
                                    put("id", g.id)
                                    put("matiere", g.matiere)
                                    put("note", g.note)
                                    put("coefficient", g.coefficient)
                                })
                            }
                        })
                    })
                }
            })
        })
    }
    context.getSharedPreferences("semestry", Context.MODE_PRIVATE)
        .edit().putString("sessions_v2", arr.toString()).apply()
}

fun loadSessions(context: Context): List<SavedSession> {
    val raw = context.getSharedPreferences("semestry", Context.MODE_PRIVATE)
        .getString("sessions_v2", null) ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val ba  = obj.getJSONArray("blocks")
            SavedSession(
                name          = obj.getString("name"),
                type          = MoyenneType.valueOf(obj.getString("type")),
                targetAverage = obj.optString("targetAverage", "10"),
                blocks = (0 until ba.length()).map { j ->
                    val b  = ba.getJSONObject(j)
                    val ga = b.getJSONArray("grades")
                    CourseBlock(
                        id         = b.optString("id", UUID.randomUUID().toString()),
                        name       = b.getString("name"),
                        isExpanded = b.optBoolean("isExpanded", true),
                        grades = (0 until ga.length()).map { k ->
                            val g = ga.getJSONObject(k)
                            GradeEntry(
                                id          = g.optString("id", UUID.randomUUID().toString()),
                                matiere     = g.getString("matiere"),
                                note        = g.getString("note"),
                                coefficient = g.getString("coefficient")
                            )
                        }
                    )
                }
            )
        }
    } catch (_: Exception) { emptyList() }
}
