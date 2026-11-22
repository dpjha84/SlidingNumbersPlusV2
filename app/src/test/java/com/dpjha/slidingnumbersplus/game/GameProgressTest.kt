package com.dpjha.slidingnumbersplus.game

import org.junit.Assert.assertEquals
import org.junit.Test

class GameProgressTest {
    @Test
    fun `levels preserve city bands and move limits`() {
        assertEquals(3, GameProgress.gridSizeForLevel(1))
        assertEquals(3, GameProgress.gridSizeForLevel(5))
        assertEquals(4, GameProgress.gridSizeForLevel(6))
        assertEquals(5, GameProgress.gridSizeForLevel(11))
        assertEquals(40, GameProgress.moveLimitForLevel(1))
        assertEquals(72, GameProgress.moveLimitForLevel(5))
        assertEquals(80, GameProgress.moveLimitForLevel(6))
    }

    @Test
    fun `winning always awards at least one star and rewards efficiency`() {
        assertEquals(1, GameProgress.starsForWin(0, 80))
        assertEquals(1, GameProgress.starsForWin(26, 80))
        assertEquals(2, GameProgress.starsForWin(27, 80))
        assertEquals(2, GameProgress.starsForWin(53, 80))
        assertEquals(3, GameProgress.starsForWin(54, 80))
    }

    @Test
    fun `city switches at the intended level boundaries`() {
        assertEquals(City.LONDON, GameProgress.cityForLevel(5))
        assertEquals(City.PARIS, GameProgress.cityForLevel(6))
        assertEquals(City.NEW_YORK, GameProgress.cityForLevel(11))
    }
}
