package com.tetristv.game

import android.graphics.Color

enum class PieceType(val color: Int) {
    I(Color.rgb(0, 240, 240)),
    O(Color.rgb(240, 240, 0)),
    T(Color.rgb(160, 0, 240)),
    S(Color.rgb(0, 240, 0)),
    Z(Color.rgb(240, 0, 0)),
    J(Color.rgb(0, 0, 240)),
    L(Color.rgb(240, 160, 0))
}

class Piece(
    val type: PieceType,
    var rotation: Int = 0,
    var x: Int = 3,
    var y: Int = 0
) {
    fun cells(offsetX: Int = 0, offsetY: Int = 0, rot: Int = rotation): List<Pair<Int, Int>> {
        val shape = SHAPES[type]!![rot]
        return shape.map { (r, c) -> Pair(x + c + offsetX, y + r + offsetY) }
    }

    fun copy(): Piece = Piece(type, rotation, x, y)

    companion object {
        val SHAPES: Map<PieceType, Array<List<Pair<Int, Int>>>> = mapOf(
            PieceType.I to arrayOf(
                listOf(1 to 0, 1 to 1, 1 to 2, 1 to 3),
                listOf(0 to 2, 1 to 2, 2 to 2, 3 to 2),
                listOf(2 to 0, 2 to 1, 2 to 2, 2 to 3),
                listOf(0 to 1, 1 to 1, 2 to 1, 3 to 1)
            ),
            PieceType.O to arrayOf(
                listOf(0 to 1, 0 to 2, 1 to 1, 1 to 2),
                listOf(0 to 1, 0 to 2, 1 to 1, 1 to 2),
                listOf(0 to 1, 0 to 2, 1 to 1, 1 to 2),
                listOf(0 to 1, 0 to 2, 1 to 1, 1 to 2)
            ),
            PieceType.T to arrayOf(
                listOf(0 to 1, 1 to 0, 1 to 1, 1 to 2),
                listOf(0 to 1, 1 to 1, 1 to 2, 2 to 1),
                listOf(1 to 0, 1 to 1, 1 to 2, 2 to 1),
                listOf(0 to 1, 1 to 0, 1 to 1, 2 to 1)
            ),
            PieceType.S to arrayOf(
                listOf(0 to 1, 0 to 2, 1 to 0, 1 to 1),
                listOf(0 to 1, 1 to 1, 1 to 2, 2 to 2),
                listOf(1 to 1, 1 to 2, 2 to 0, 2 to 1),
                listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1)
            ),
            PieceType.Z to arrayOf(
                listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2),
                listOf(0 to 2, 1 to 1, 1 to 2, 2 to 1),
                listOf(1 to 0, 1 to 1, 2 to 1, 2 to 2),
                listOf(0 to 1, 1 to 0, 1 to 1, 2 to 0)
            ),
            PieceType.J to arrayOf(
                listOf(0 to 0, 1 to 0, 1 to 1, 1 to 2),
                listOf(0 to 1, 0 to 2, 1 to 1, 2 to 1),
                listOf(1 to 0, 1 to 1, 1 to 2, 2 to 2),
                listOf(0 to 1, 1 to 1, 2 to 0, 2 to 1)
            ),
            PieceType.L to arrayOf(
                listOf(0 to 2, 1 to 0, 1 to 1, 1 to 2),
                listOf(0 to 1, 1 to 1, 2 to 1, 2 to 2),
                listOf(1 to 0, 1 to 1, 1 to 2, 2 to 0),
                listOf(0 to 0, 0 to 1, 1 to 1, 2 to 1)
            )
        )

        fun spawnX(type: PieceType): Int = when (type) {
            PieceType.O -> 4
            else -> 3
        }
    }
}

object SrsTable {
    // Standard SRS kicks, y-positive=DOWN (game convention, negated from standard y+ = up)
    private val jltszKicks = mapOf(
        Pair(0, 1) to listOf(0 to 0, -1 to 0, -1 to 1, 0 to -2, -1 to -2),
        Pair(1, 0) to listOf(0 to 0, 1 to 0, 1 to -1, 0 to 2, 1 to 2),
        Pair(1, 2) to listOf(0 to 0, 1 to 0, 1 to -1, 0 to 2, 1 to 2),
        Pair(2, 1) to listOf(0 to 0, -1 to 0, -1 to 1, 0 to -2, -1 to -2),
        Pair(2, 3) to listOf(0 to 0, 1 to 0, 1 to 1, 0 to -2, 1 to -2),
        Pair(3, 2) to listOf(0 to 0, -1 to 0, -1 to -1, 0 to 2, -1 to 2),
        Pair(3, 0) to listOf(0 to 0, -1 to 0, -1 to -1, 0 to 2, -1 to 2),
        Pair(0, 3) to listOf(0 to 0, 1 to 0, 1 to 1, 0 to -2, 1 to -2)
    )

    private val iKicks = mapOf(
        Pair(0, 1) to listOf(0 to 0, -2 to 0, 1 to 0, -2 to -1, 1 to 2),
        Pair(1, 0) to listOf(0 to 0, 2 to 0, -1 to 0, 2 to 1, -1 to -2),
        Pair(1, 2) to listOf(0 to 0, -1 to 0, 2 to 0, -1 to 2, 2 to -1),
        Pair(2, 1) to listOf(0 to 0, 1 to 0, -2 to 0, 1 to -2, -2 to 1),
        Pair(2, 3) to listOf(0 to 0, 2 to 0, -1 to 0, 2 to 1, -1 to -2),
        Pair(3, 2) to listOf(0 to 0, -2 to 0, 1 to 0, -2 to -1, 1 to 2),
        Pair(3, 0) to listOf(0 to 0, 1 to 0, -2 to 0, 1 to -2, -2 to 1),
        Pair(0, 3) to listOf(0 to 0, -1 to 0, 2 to 0, -1 to 2, 2 to -1)
    )

    private val oKicks = mapOf(
        Pair(0, 1) to listOf(0 to 0),
        Pair(1, 0) to listOf(0 to 0),
        Pair(1, 2) to listOf(0 to 0),
        Pair(2, 1) to listOf(0 to 0),
        Pair(2, 3) to listOf(0 to 0),
        Pair(3, 2) to listOf(0 to 0),
        Pair(3, 0) to listOf(0 to 0),
        Pair(0, 3) to listOf(0 to 0)
    )

    fun getKicks(type: PieceType, fromRot: Int, toRot: Int): List<Pair<Int, Int>> {
        val table = when (type) {
            PieceType.I -> iKicks
            PieceType.O -> oKicks
            else -> jltszKicks
        }
        return table[Pair(fromRot, toRot)] ?: listOf(0 to 0)
    }
}
