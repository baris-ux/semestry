package io.github.baris_ux.semestry.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

fun saveSessions(context: Context, sessions: List<SavedSession>) {
    val arr = JSONArray()
    sessions.forEach { s ->
        arr.put(JSONObject().apply {
            put("name", s.name)
            put("ues", JSONArray().also { ua ->
                s.ues.forEach { ue ->
                    ua.put(JSONObject().apply {
                        put("id", ue.id)
                        put("name", ue.name)
                        put("moyenneType", ue.moyenneType.name)
                        put("courses", JSONArray().also { ga ->
                            ue.courses.forEach { g ->
                                ga.put(JSONObject().apply {
                                    put("id", g.id)
                                    put("matiere", g.matiere)
                                    put("note", g.note)
                                    put("coefficient", g.coefficient)
                                    put("isComposite", g.isComposite)
                                    put("subGrades", JSONArray().also { sa ->
                                        g.subGrades.forEach { sg ->
                                            sa.put(JSONObject().apply {
                                                put("id", sg.id)
                                                put("label", sg.label)
                                                put("note", sg.note)
                                            })
                                        }
                                    })
                                })
                            }
                        })
                    })
                }
            })
        })
    }
    context.getSharedPreferences("semestry", Context.MODE_PRIVATE)
        .edit().putString("sessions_v7", arr.toString()).apply()
}

private fun parseSubGrade(sg: JSONObject) = SubGrade(
    id    = sg.optString("id", UUID.randomUUID().toString()),
    label = sg.optString("label", ""),
    note  = sg.optString("note", "")
)

private fun parseGradeEntry(g: JSONObject): GradeEntry {
    val sa = g.optJSONArray("subGrades")
    return GradeEntry(
        id          = g.optString("id", UUID.randomUUID().toString()),
        matiere     = g.getString("matiere"),
        note        = g.getString("note"),
        coefficient = g.optString("coefficient", "1"),
        isComposite = g.optBoolean("isComposite", false),
        subGrades   = if (sa != null && sa.length() > 0)
            (0 until sa.length()).map { parseSubGrade(sa.getJSONObject(it)) }
        else listOf(SubGrade(label = "CC"), SubGrade(label = "Partiel"))
    )
}

fun loadSessions(context: Context): List<SavedSession> {
    val prefs = context.getSharedPreferences("semestry", Context.MODE_PRIVATE)

    // v7 — current format (no UE credits field, coef auto-computed)
    val rawV7 = prefs.getString("sessions_v7", null)
    if (rawV7 != null) {
        return try {
            val arr = JSONArray(rawV7)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val ua  = obj.getJSONArray("ues")
                SavedSession(
                    name = obj.getString("name"),
                    ues  = (0 until ua.length()).map { j ->
                        val ue = ua.getJSONObject(j)
                        val ga = ue.getJSONArray("courses")
                        UE(
                            id          = ue.optString("id", UUID.randomUUID().toString()),
                            name        = ue.optString("name", ""),
                            moyenneType = MoyenneType.valueOf(ue.optString("moyenneType", "ARITHMETIQUE")),
                            courses     = (0 until ga.length()).map { k -> parseGradeEntry(ga.getJSONObject(k)) }
                        )
                    }
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    // v6 → v7: had a credits field on UE (now removed)
    val rawV6 = prefs.getString("sessions_v6", null)
    if (rawV6 != null) {
        val migrated = try {
            val arr = JSONArray(rawV6)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val ua  = obj.getJSONArray("ues")
                SavedSession(
                    name = obj.getString("name"),
                    ues  = (0 until ua.length()).map { j ->
                        val ue = ua.getJSONObject(j)
                        val ga = ue.getJSONArray("courses")
                        UE(
                            id          = ue.optString("id", UUID.randomUUID().toString()),
                            name        = ue.optString("name", ""),
                            moyenneType = MoyenneType.valueOf(ue.optString("moyenneType", "ARITHMETIQUE")),
                            courses     = (0 until ga.length()).map { k -> parseGradeEntry(ga.getJSONObject(k)) }
                        )
                    }
                )
            }
        } catch (_: Exception) { emptyList() }
        if (migrated.isNotEmpty()) saveSessions(context, migrated)
        return migrated
    }

    // v5 → v7: UE had a coefficient field (now auto) and GradeEntry had rattrapageNote (now removed)
    val rawV5 = prefs.getString("sessions_v5", null)
    if (rawV5 != null) {
        val migrated = try {
            val arr = JSONArray(rawV5)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val ua  = obj.getJSONArray("ues")
                SavedSession(
                    name = obj.getString("name"),
                    ues  = (0 until ua.length()).map { j ->
                        val ue = ua.getJSONObject(j)
                        val ga = ue.getJSONArray("courses")
                        UE(
                            id          = ue.optString("id", UUID.randomUUID().toString()),
                            name        = ue.optString("name", ""),
                            moyenneType = MoyenneType.valueOf(ue.optString("moyenneType", "ARITHMETIQUE")),
                            courses     = (0 until ga.length()).map { k -> parseGradeEntry(ga.getJSONObject(k)) }
                        )
                    }
                )
            }
        } catch (_: Exception) { emptyList() }
        if (migrated.isNotEmpty()) saveSessions(context, migrated)
        return migrated
    }

    // v4 → v6: moyenneType was per-course; UE had coefficient; GradeEntry had rattrapageNote
    val rawV4 = prefs.getString("sessions_v4", null)
    if (rawV4 != null) {
        val migrated = try {
            val arr = JSONArray(rawV4)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val ua  = obj.getJSONArray("ues")
                SavedSession(
                    name = obj.getString("name"),
                    ues  = (0 until ua.length()).map { j ->
                        val ue = ua.getJSONObject(j)
                        val ga = ue.getJSONArray("courses")
                        val firstCourseMoyenne = if (ga.length() > 0)
                            MoyenneType.valueOf(ga.getJSONObject(0).optString("moyenneType", "ARITHMETIQUE"))
                        else MoyenneType.ARITHMETIQUE
                        UE(
                            id          = ue.optString("id", UUID.randomUUID().toString()),
                            name        = ue.optString("name", ""),
                            moyenneType = firstCourseMoyenne,
                            courses     = (0 until ga.length()).map { k -> parseGradeEntry(ga.getJSONObject(k)) }
                        )
                    }
                )
            }
        } catch (_: Exception) { emptyList() }
        if (migrated.isNotEmpty()) saveSessions(context, migrated)
        return migrated
    }

    // v3 → v6: flat list of GradeEntry, one per UE
    val rawV3 = prefs.getString("sessions_v3", null) ?: return emptyList()
    val migrated = try {
        val arr = JSONArray(rawV3)
        (0 until arr.length()).map { i ->
            val obj    = arr.getJSONObject(i)
            val ga     = obj.getJSONArray("grades")
            val grades = (0 until ga.length()).map { j -> parseGradeEntry(ga.getJSONObject(j)) }
            SavedSession(
                name = obj.getString("name"),
                ues  = grades.map { g ->
                    UE(name = g.matiere, courses = listOf(g.copy(coefficient = "1")))
                }
            )
        }
    } catch (_: Exception) { emptyList() }
    if (migrated.isNotEmpty()) saveSessions(context, migrated)
    return migrated
}
