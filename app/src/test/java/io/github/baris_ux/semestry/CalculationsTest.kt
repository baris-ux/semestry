package io.github.baris_ux.semestry

import io.github.baris_ux.semestry.data.GradeEntry
import io.github.baris_ux.semestry.data.MoyenneType
import io.github.baris_ux.semestry.data.SubGrade
import io.github.baris_ux.semestry.data.UE
import io.github.baris_ux.semestry.utils.coeffError
import io.github.baris_ux.semestry.utils.computeCompositeNote
import io.github.baris_ux.semestry.utils.computeEffectiveNote
import io.github.baris_ux.semestry.utils.computeUEAverage
import io.github.baris_ux.semestry.utils.effectiveCoeff
import io.github.baris_ux.semestry.utils.noteError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

private const val DELTA = 0.001

class CalculationsTest {

    // ── noteError ─────────────────────────────────────────────────────────────

    @Test fun noteError_vide_retourneNull() = assertNull(noteError(""))
    @Test fun noteError_valide_retourneNull() = assertNull(noteError("14"))
    @Test fun noteError_zero_retourneNull() = assertNull(noteError("0"))
    @Test fun noteError_vingt_retourneNull() = assertNull(noteError("20"))
    @Test fun noteError_virgule_valide() = assertNull(noteError("14,5"))
    @Test fun noteError_superieurMax_retourneErreur() = assertNotNull(noteError("21"))
    @Test fun noteError_negatif_retourneErreur() = assertNotNull(noteError("-1"))
    @Test fun noteError_texte_retourneErreur() = assertNotNull(noteError("abc"))

    // ── coeffError ────────────────────────────────────────────────────────────

    @Test fun coeffError_vide_retourneNull() = assertNull(coeffError(""))
    @Test fun coeffError_valide_retourneNull() = assertNull(coeffError("2"))
    @Test fun coeffError_zero_retourneErreur() = assertNotNull(coeffError("0"))
    @Test fun coeffError_negatif_retourneErreur() = assertNotNull(coeffError("-1"))
    @Test fun coeffError_texte_retourneErreur() = assertNotNull(coeffError("x"))

    // ── effectiveCoeff ────────────────────────────────────────────────────────

    @Test fun effectiveCoeff_retourneCoeff() {
        assertEquals(3.0, effectiveCoeff(GradeEntry(coefficient = "3"))!!, DELTA)
    }

    @Test fun effectiveCoeff_composite_retourneCoeffDuCours() {
        val g = GradeEntry(isComposite = true, coefficient = "2",
            subGrades = listOf(SubGrade(note = "10"), SubGrade(note = "14")))
        assertEquals(2.0, effectiveCoeff(g)!!, DELTA)
    }

    @Test fun effectiveCoeff_zero_retourneNull() {
        assertNull(effectiveCoeff(GradeEntry(coefficient = "0")))
    }

    // ── computeCompositeNote ──────────────────────────────────────────────────

    @Test fun computeCompositeNote_vide_retourneNull() {
        assertNull(computeCompositeNote(emptyList()))
    }

    @Test fun computeCompositeNote_epreuveManquante_retourneNull() {
        assertNull(computeCompositeNote(listOf(
            SubGrade(label = "CC", note = "12"),
            SubGrade(label = "Partiel", note = "")
        )))
    }

    @Test fun computeCompositeNote_deuxEpreuves_retourneMoyenne() {
        // (10 + 14) / 2 = 12
        assertEquals(12.0, computeCompositeNote(listOf(
            SubGrade(note = "10"), SubGrade(note = "14")
        ))!!, DELTA)
    }

    @Test fun computeCompositeNote_troisEpreuves_retourneMoyenne() {
        // (9 + 12 + 15) / 3 = 12
        assertEquals(12.0, computeCompositeNote(listOf(
            SubGrade(note = "9"), SubGrade(note = "12"), SubGrade(note = "15")
        ))!!, DELTA)
    }

    @Test fun computeCompositeNote_separateurVirgule() {
        assertEquals(14.0, computeCompositeNote(listOf(
            SubGrade(note = "12,5"), SubGrade(note = "15,5")
        ))!!, DELTA)
    }

    // ── computeEffectiveNote ──────────────────────────────────────────────────

    @Test fun computeEffectiveNote_noteVide_retourneNull() {
        assertNull(computeEffectiveNote(GradeEntry(note = "")))
    }

    @Test fun computeEffectiveNote_noteValide_retourneNote() {
        assertEquals(12.0, computeEffectiveNote(GradeEntry(note = "12"))!!, DELTA)
    }

    @Test fun computeEffectiveNote_composite_retourneMoyenne() {
        val g = GradeEntry(isComposite = true,
            subGrades = listOf(SubGrade(note = "10"), SubGrade(note = "14")))
        assertEquals(12.0, computeEffectiveNote(g)!!, DELTA)
    }

    @Test fun computeEffectiveNote_compositeIncomplet_retourneNull() {
        val g = GradeEntry(isComposite = true,
            subGrades = listOf(SubGrade(note = "10"), SubGrade(note = "")))
        assertNull(computeEffectiveNote(g))
    }

    // ── computeUEAverage ─────────────────────────────────────────────────────

    @Test fun computeUEAverage_deuxCours_arithmetique() {
        val ue = UE(moyenneType = MoyenneType.ARITHMETIQUE, courses = listOf(
            GradeEntry(note = "10", coefficient = "1"),
            GradeEntry(note = "14", coefficient = "1")
        ))
        assertEquals(12.0, computeUEAverage(ue)!!, DELTA)
    }

    @Test fun computeUEAverage_coeffsDifferents() {
        // (10*1 + 16*2) / 3 = 14
        val ue = UE(moyenneType = MoyenneType.ARITHMETIQUE, courses = listOf(
            GradeEntry(note = "10", coefficient = "1"),
            GradeEntry(note = "16", coefficient = "2")
        ))
        assertEquals(14.0, computeUEAverage(ue)!!, DELTA)
    }

    @Test fun computeUEAverage_coursIncomplet_retourneNull() {
        val ue = UE(courses = listOf(
            GradeEntry(note = "10", coefficient = "1"),
            GradeEntry(note = "",   coefficient = "1")
        ))
        assertNull(computeUEAverage(ue))
    }

    @Test fun computeUEAverage_geometrique_coeffsEgaux() {
        // (10^1 * 16^1)^(1/2) = sqrt(160)
        val ue = UE(moyenneType = MoyenneType.GEOMETRIQUE, courses = listOf(
            GradeEntry(note = "10", coefficient = "1"),
            GradeEntry(note = "16", coefficient = "1")
        ))
        assertEquals(kotlin.math.sqrt(160.0), computeUEAverage(ue)!!, DELTA)
    }

    @Test fun computeUEAverage_geometrique_zeroNote_retourneNull() {
        val ue = UE(moyenneType = MoyenneType.GEOMETRIQUE, courses = listOf(
            GradeEntry(note = "0",  coefficient = "1"),
            GradeEntry(note = "14", coefficient = "1")
        ))
        assertNull(computeUEAverage(ue))
    }
}
