package com.dpjha.slidingnumbersplus.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleEngineTest {
    @Test
    fun `slide across a row moves the whole chain toward the gap`() {
        val engine = PuzzleEngine(3, listOf(1, 2, 0, 5, 4, 6, 7, 8, 3))

        assertEquals(listOf(0, 1, 2), engine.move(0))
        assertEquals(listOf(0, 1, 2, 5, 4, 6, 7, 8, 3), engine.snapshot())
    }

    @Test
    fun `slide down a column moves the whole chain toward the gap`() {
        val engine = PuzzleEngine(3)

        assertEquals(listOf(6, 7, 8), engine.move(6))
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 0, 7, 8), engine.snapshot())
    }

    @Test
    fun `reject blank diagonal and out of range moves`() {
        val engine = PuzzleEngine(3)

        assertTrue(engine.move(8).isEmpty())
        assertTrue(engine.move(0).isEmpty())
        assertTrue(engine.move(-1).isEmpty())
        assertTrue(engine.move(9).isEmpty())
        assertTrue(engine.isSolved())
    }

    @Test
    fun `shuffle remains solvable across supported board sizes`() {
        for (size in 3..5) {
            val engine = PuzzleEngine(size, random = Random(size))
            engine.shuffle(600)

            assertTrue(PuzzleEngine.isSolvable(engine.snapshot(), size))
            assertFalse(engine.isSolved())
        }
    }

    @Test
    fun `invalid starting sequence falls back to solved board`() {
        val engine = PuzzleEngine(3, listOf(1, 2, 3))

        assertTrue(engine.isSolved())
    }

    @Test
    fun `solvability parity rejects swapped tiles`() {
        assertTrue(PuzzleEngine.isSolvable(PuzzleEngine.solvedSequence(4), 4))
        assertFalse(PuzzleEngine.isSolvable(listOf(2, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0), 4))
    }
}
