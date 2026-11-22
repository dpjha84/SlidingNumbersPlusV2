package com.dpjha.slidingnumbersplus.game

import kotlin.random.Random

class PuzzleEngine(
    val size: Int,
    sequence: List<Int> = solvedSequence(size),
    private val random: Random = Random.Default,
) {
    private val cells: MutableList<Int>

    init {
        require(size in MIN_SIZE..MAX_SIZE) { "Grid size must be between $MIN_SIZE and $MAX_SIZE" }
        cells = sequence.takeIf { isSolvable(it, size) }?.toMutableList()
            ?: solvedSequence(size).toMutableList()
    }

    fun snapshot(): List<Int> = cells.toList()

    fun isSolved(): Boolean = cells == solvedSequence(size)

    fun move(clickedIndex: Int): List<Int> {
        if (clickedIndex !in cells.indices || cells[clickedIndex] == EMPTY) return emptyList()
        val emptyIndex = cells.indexOf(EMPTY)
        val clickedRow = clickedIndex / size
        val clickedColumn = clickedIndex % size
        val emptyRow = emptyIndex / size
        val emptyColumn = emptyIndex % size

        val rowDelta: Int
        val columnDelta: Int
        if (clickedRow == emptyRow && clickedColumn != emptyColumn) {
            rowDelta = 0
            columnDelta = if (emptyColumn > clickedColumn) 1 else -1
        } else if (clickedColumn == emptyColumn && clickedRow != emptyRow) {
            rowDelta = if (emptyRow > clickedRow) 1 else -1
            columnDelta = 0
        } else {
            return emptyList()
        }

        val chain = mutableListOf<Int>()
        var row = clickedRow
        var column = clickedColumn
        while (row != emptyRow || column != emptyColumn) {
            chain += row * size + column
            row += rowDelta
            column += columnDelta
        }

        for (index in chain.lastIndex downTo 0) {
            val destination = if (index == chain.lastIndex) emptyIndex else chain[index + 1]
            cells[destination] = cells[chain[index]]
        }
        cells[clickedIndex] = EMPTY
        return chain + emptyIndex
    }

    fun shuffle(minimumDisplacedTiles: Int = 2) {
        require(minimumDisplacedTiles >= 0)
        val startingBoard = cells.toList()
        val requiredDisplacedTiles = minimumDisplacedTiles.coerceIn(1, size * size - 1)
        do {
            cells.clear()
            cells.addAll(startingBoard.shuffled(random))
        } while (
            !isSolvable(cells, size) ||
            cells == startingBoard ||
            cells.indices.count { cells[it] != startingBoard[it] } < requiredDisplacedTiles
        )
    }

    private fun adjacentIndices(index: Int): List<Int> {
        val row = index / size
        val column = index % size
        return buildList(4) {
            if (row > 0) add(index - size)
            if (row < size - 1) add(index + size)
            if (column > 0) add(index - 1)
            if (column < size - 1) add(index + 1)
        }
    }

    companion object {
        const val EMPTY = 0
        const val MIN_SIZE = 3
        const val MAX_SIZE = 5

        fun solvedSequence(size: Int): List<Int> =
            (1 until size * size).toList() + EMPTY

        fun isValidSequence(sequence: List<Int>, size: Int): Boolean =
            size in MIN_SIZE..MAX_SIZE &&
                sequence.size == size * size &&
                sequence.sorted() == (0 until size * size).toList()

        fun isSolvable(sequence: List<Int>, size: Int): Boolean {
            if (!isValidSequence(sequence, size)) return false
            val numbered = sequence.filter { it != EMPTY }
            val inversions = numbered.indices.sumOf { left ->
                (left + 1 until numbered.size).count { right -> numbered[left] > numbered[right] }
            }
            return if (size % 2 == 1) {
                inversions % 2 == 0
            } else {
                val blankRowFromBottom = size - sequence.indexOf(EMPTY) / size
                (inversions + blankRowFromBottom) % 2 == 1
            }
        }
    }
}
