package com.dpjha.slidingnumbersplus.game

data class GameProgress(
    val level: Int = 1,
    val maxLevel: Int = 1,
    val starCount: Int = 0,
    val movesLeft: Int = -1,
    val sequence: List<Int> = emptyList(),
    val soundOn: Boolean = true,
    val musicOn: Boolean = false,
    val totalScore: Long = 0,
    val levelsCompleted: Int = 0,
    val bestStars: Int = 0,
    val hapticsOn: Boolean = true,
    val reducedMotion: Boolean = false,
) {
    val gridSize: Int get() = gridSizeForLevel(level)
    val moveLimit: Int get() = moveLimitForLevel(level)
    val hasSavedBoard: Boolean
        get() = movesLeft >= 0 && PuzzleEngine.isSolvable(sequence, gridSize)

    fun normalized(): GameProgress {
        val safeLevel = level.coerceAtLeast(1)
        val safeMaxLevel = maxLevel.coerceAtLeast(safeLevel)
        val safeGridSize = gridSizeForLevel(safeLevel)
        val safeSequence = sequence.takeIf { PuzzleEngine.isSolvable(it, safeGridSize) } ?: emptyList()
        return copy(
            level = safeLevel,
            maxLevel = safeMaxLevel,
            starCount = starCount.coerceAtLeast(0),
            movesLeft = movesLeft.coerceIn(-1, 1_000_000),
            sequence = safeSequence,
            totalScore = totalScore.coerceAtLeast(0),
            levelsCompleted = levelsCompleted.coerceAtLeast(0),
            bestStars = bestStars.coerceIn(0, 3),
        )
    }

    companion object {
        fun gridSizeForLevel(level: Int): Int = ((level.coerceAtLeast(1) - 1) / 5 + 3).coerceAtMost(5)

        fun incrementForLevel(level: Int): Int = level.coerceAtLeast(1).let { it % 5 }.let {
            if (it == 0) 5 else it
        }

        fun moveLimitForLevel(level: Int): Int {
            val size = gridSizeForLevel(level)
            return (1 shl size) * (incrementForLevel(level) + 4)
        }

        fun starsForWin(movesLeft: Int, moveLimit: Int): Int = when {
            movesLeft > moveLimit * 2 / 3 -> 3
            movesLeft > moveLimit / 3 -> 2
            else -> 1
        }

        fun cityForLevel(level: Int): City = when (gridSizeForLevel(level)) {
            3 -> City.LONDON
            4 -> City.PARIS
            else -> City.NEW_YORK
        }
    }
}

enum class City(val title: String, val firstLevel: Int, val lastLevel: Int) {
    LONDON("London", 1, 5),
    PARIS("Paris", 6, 10),
    NEW_YORK("New York", 11, Int.MAX_VALUE),
}
