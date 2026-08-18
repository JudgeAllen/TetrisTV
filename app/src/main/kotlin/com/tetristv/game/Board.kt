package com.tetristv.game

class Board(val width: Int = 10, val height: Int = 20) {
    private val grid: Array<IntArray> = Array(height) { IntArray(width) }

    fun getColor(x: Int, y: Int): Int {
        if (x < 0 || x >= width || y < 0 || y >= height) return 0
        return grid[y][x]
    }

    fun canPlace(cells: List<Pair<Int, Int>>): Boolean {
        for ((cx, cy) in cells) {
            if (cx < 0 || cx >= width) return false
            if (cy >= height) return false
            if (cy >= 0 && grid[cy][cx] != 0) return false
        }
        return true
    }

    fun lockPiece(piece: Piece) {
        val color = piece.type.color
        for ((cx, cy) in piece.cells()) {
            if (cy in 0 until height && cx in 0 until width) {
                grid[cy][cx] = color
            }
        }
    }

    fun findFullRows(): List<Int> {
        val result = mutableListOf<Int>()
        for (r in 0 until height) {
            var full = true
            for (c in 0 until width) {
                if (grid[r][c] == 0) {
                    full = false
                    break
                }
            }
            if (full) result.add(r)
        }
        return result
    }

    fun clearRows(rows: List<Int>) {
        val rowSet = rows.toSet()
        var writeRow = height - 1
        for (readRow in height - 1 downTo 0) {
            if (readRow !in rowSet) {
                if (writeRow != readRow) {
                    grid[writeRow] = grid[readRow].copyOf()
                }
                writeRow--
            }
        }
        while (writeRow >= 0) {
            grid[writeRow] = IntArray(width)
            writeRow--
        }
    }

    fun isGameOver(piece: Piece): Boolean = !canPlace(piece.cells())

    fun reset() {
        for (i in grid.indices) {
            grid[i] = IntArray(width)
        }
    }
}

class BagRandomizer {
    private val bag = mutableListOf<PieceType>()
    private val rng = java.util.Random()

    fun next(): PieceType {
        if (bag.isEmpty()) {
            bag.addAll(PieceType.values())
            bag.shuffle(rng)
        }
        return bag.removeAt(0)
    }

    fun reset() {
        bag.clear()
    }
}
