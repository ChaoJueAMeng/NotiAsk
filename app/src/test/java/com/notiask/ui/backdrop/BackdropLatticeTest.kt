package com.notiask.ui.backdrop

import com.notiask.data.ProviderKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class BackdropLatticeTest {
    @Test
    fun wrapStaysInsidePeriodAndHandlesNegatives() {
        assertEquals(30f, BackdropLattice.wrap(130f, 100f), 0.001f)
        assertEquals(90f, BackdropLattice.wrap(-10f, 100f), 0.001f)
        assertEquals(0f, BackdropLattice.wrap(0f, 100f), 0.001f)
        assertEquals(0f, BackdropLattice.wrap(200f, 100f), 0.001f)
    }

    @Test
    fun diagonalMarksShareTheSameIndex() {
        assertEquals(BackdropLattice.markIndex(2, 0, 6), BackdropLattice.markIndex(1, 1, 6))
        assertEquals(BackdropLattice.markIndex(2, 0, 6), BackdropLattice.markIndex(0, 2, 6))
        assertEquals(0, BackdropLattice.markIndex(-6, 0, 6))
    }

    @Test
    fun patternPeriodCoversFullMarkCycle() {
        assertEquals(720f, BackdropLattice.patternPeriodPx(120f, 6), 0.001f)
    }

    @Test
    fun tileShiftAndPixelOffsetSplitDiagonalMotion() {
        assertEquals(2, BackdropLattice.tileShift(250f, 100f))
        assertEquals(50f, BackdropLattice.pixelOffset(250f, 100f), 0.001f)
        assertEquals(0f, BackdropLattice.pixelOffset(300f, 100f), 0.001f)
    }

    @Test
    fun copiesNeededCoversSpanPlusOneExtraTile() {
        assertEquals(4, BackdropLattice.copiesNeeded(200f, 100f))
    }

    @Test
    fun floatDisplacementIsBoundedAndChangesWithTime() {
        val a = BackdropLattice.floatDisplacement(0f, 12f)
        val b = BackdropLattice.floatDisplacement(1.2f, 12f)
        assertTrue(abs(a.first) <= 12f)
        assertTrue(abs(a.second) <= 12f)
        assertTrue(a != b)
    }

    @Test
    fun everyProviderHasABackdropMark() {
        assertEquals(ProviderKind.entries.size, ModelMark.entries.size)
        ProviderKind.entries.forEach { kind ->
            assertEquals(kind, ModelMark.forProvider(kind).provider)
        }
    }
}
