package com.tetristv.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.tetristv.game.*

class Renderer {
    private val bgPaint = Paint().apply { color = Color.rgb(13, 17, 23) }
    private val boardBgPaint = Paint().apply { color = Color.rgb(10, 14, 20) }
    private val boardBorderPaint = Paint().apply {
        color = Color.rgb(22, 33, 62); style = Paint.Style.STROKE; strokeWidth = 3f
    }
    private val gridPaint = Paint().apply {
        color = Color.argb(8, 255, 255, 255); strokeWidth = 1f
    }
    private val blockPaint = Paint()
    private val ghostPaint = Paint()
    private val panelPaint = Paint().apply { color = Color.rgb(22, 27, 34) }
    private val panelBorderPaint = Paint().apply {
        color = Color.rgb(48, 54, 61); style = Paint.Style.STROKE; strokeWidth = 2f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 28f; typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(136, 135, 128); textSize = 22f
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 36f; typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val overlayPaint = Paint().apply { color = Color.argb(180, 0, 0, 0) }
    private val overlayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 56f; typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val overlaySubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(136, 135, 128); textSize = 28f; textAlign = Paint.Align.CENTER
    }
    private val selPaint = Paint().apply { color = Color.argb(60, 160, 0, 240) }
    private val clearPaint = Paint().apply { color = Color.WHITE }
    private val rectF = RectF()

    private data class Layout(
        val cell: Float, val boardX: Float, val boardY: Float,
        val leftX: Float, val rightX: Float, val panelW: Float
    )

    private fun computeLayout(w: Float, h: Float): Layout {
        val maxBoardH = h - 60f
        val maxBoardW = w * 0.32f
        val cell = minOf(maxBoardH / 20f, maxBoardW / 10f)
        val boardW = cell * 10
        val boardH = cell * 20
        val boardX = (w - boardW) / 2f
        val boardY = (h - boardH) / 2f
        val panelW = minOf(w * 0.14f, 260f)
        val leftX = boardX - panelW - 24f
        val rightX = boardX + boardW + 24f
        return Layout(cell, boardX, boardY, leftX, rightX, panelW)
    }

    fun draw(canvas: Canvas, engine: GameEngine) {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        val layout = computeLayout(w, h)

        canvas.drawRect(0f, 0f, w, h, bgPaint)
        drawBoardBg(canvas, layout)
        drawLockedBlocks(canvas, engine, layout)
        if (engine.gameState == GameState.PLAYING || engine.gameState == GameState.PAUSED ||
            engine.gameState == GameState.LINE_CLEAR || engine.gameState == GameState.MENU) {
            drawGhost(canvas, engine, layout)
            drawCurrentPiece(canvas, engine, layout)
        }
        drawLeftPanel(canvas, engine, layout, h)
        drawRightPanel(canvas, engine, layout)
        drawOverlay(canvas, engine, w, h)
    }

    private fun drawBoardBg(canvas: Canvas, l: Layout) {
        val bw = l.cell * 10
        val bh = l.cell * 20
        canvas.drawRect(l.boardX - 3, l.boardY - 3, l.boardX + bw + 3, l.boardY + bh + 3, boardBgPaint)
        for (r in 0..20) {
            val y = l.boardY + r * l.cell
            canvas.drawLine(l.boardX, y, l.boardX + bw, y, gridPaint)
        }
        for (c in 0..10) {
            val x = l.boardX + c * l.cell
            canvas.drawLine(x, l.boardY, x, l.boardY + bh, gridPaint)
        }
        canvas.drawRect(l.boardX - 3, l.boardY - 3, l.boardX + bw + 3, l.boardY + bh + 3, boardBorderPaint)
    }

    private fun drawBlock(canvas: Canvas, x: Float, y: Float, size: Float, color: Int, alpha: Int = 255) {
        blockPaint.color = if (alpha < 255) Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)) else color
        rectF.set(x + 1, y + 1, x + size - 1, y + size - 1)
        canvas.drawRoundRect(rectF, size * 0.12f, size * 0.12f, blockPaint)
        blockPaint.color = lighten(color, 0.3f)
        blockPaint.style = Paint.Style.STROKE
        blockPaint.strokeWidth = 2f
        canvas.drawRoundRect(rectF, size * 0.12f, size * 0.12f, blockPaint)
        blockPaint.style = Paint.Style.FILL
    }

    private fun drawLockedBlocks(canvas: Canvas, engine: GameEngine, l: Layout) {
        for (r in 0 until engine.board.height) {
            for (c in 0 until engine.board.width) {
                val color = engine.board.getColor(c, r)
                if (color != 0) {
                    val isClearing = engine.clearedRows.contains(r)
                    if (isClearing) {
                        drawBlock(canvas, l.boardX + c * l.cell, l.boardY + r * l.cell, l.cell, Color.WHITE)
                    } else {
                        drawBlock(canvas, l.boardX + c * l.cell, l.boardY + r * l.cell, l.cell, color)
                    }
                }
            }
        }
    }

    private fun drawGhost(canvas: Canvas, engine: GameEngine, l: Layout) {
        val ghost = engine.getGhostPiece() ?: return
        ghostPaint.color = Color.argb(40, 255, 255, 255)
        ghostPaint.style = Paint.Style.STROKE
        ghostPaint.strokeWidth = 2f
        for ((cx, cy) in ghost.cells()) {
            if (cy < 0) continue
            rectF.set(
                l.boardX + cx * l.cell + 2, l.boardY + cy * l.cell + 2,
                l.boardX + (cx + 1) * l.cell - 2, l.boardY + (cy + 1) * l.cell - 2
            )
            canvas.drawRoundRect(rectF, l.cell * 0.12f, l.cell * 0.12f, ghostPaint)
        }
        ghostPaint.style = Paint.Style.FILL
    }

    private fun drawCurrentPiece(canvas: Canvas, engine: GameEngine, l: Layout) {
        val piece = engine.currentPiece ?: return
        for ((cx, cy) in piece.cells()) {
            if (cy < 0) continue
            drawBlock(canvas, l.boardX + cx * l.cell, l.boardY + cy * l.cell, l.cell, piece.type.color)
        }
    }

    private fun drawLeftPanel(canvas: Canvas, engine: GameEngine, l: Layout, h: Float) {
        val panelH = l.cell * 20
        canvas.drawRect(l.leftX, l.boardY, l.leftX + l.panelW, l.boardY + panelH, panelPaint)
        canvas.drawRect(l.leftX, l.boardY, l.leftX + l.panelW, l.boardY + panelH, panelBorderPaint)

        val items = listOf(
            "SCORE" to engine.score.toString(),
            "LEVEL" to engine.level.toString(),
            "LINES" to engine.lines.toString(),
            "HIGH" to engine.highScore.toString()
        )
        val itemH = panelH / items.size
        items.forEachIndexed { i, (label, value) ->
            val cy = l.boardY + i * itemH
            canvas.drawText(label, l.leftX + 20f, cy + 35f, labelPaint)
            canvas.drawText(value, l.leftX + 20f, cy + 70f, valuePaint)
        }
    }

    private fun drawRightPanel(canvas: Canvas, engine: GameEngine, l: Layout) {
        val panelH = l.cell * 20
        canvas.drawRect(l.rightX, l.boardY, l.rightX + l.panelW, l.boardY + panelH, panelPaint)
        canvas.drawRect(l.rightX, l.boardY, l.rightX + l.panelW, l.boardY + panelH, panelBorderPaint)

        var y = l.boardY + 15f
        canvas.drawText("NEXT", l.rightX + 20f, y + 22f, labelPaint)
        y += 40f
        val previewCount = minOf(3, engine.nextQueue.size)
        for (i in 0 until previewCount) {
            drawMiniPiece(canvas, engine.nextQueue[i], l.rightX + 20f, y, l.cell * 0.55f)
            y += l.cell * 0.55f * 3 + 15f
        }
        y += 10f
        canvas.drawText("HOLD", l.rightX + 20f, y + 22f, labelPaint)
        y += 40f
        engine.holdPiece?.let {
            drawMiniPiece(canvas, it, l.rightX + 20f, y, l.cell * 0.55f)
        }
    }

    private fun drawMiniPiece(canvas: Canvas, type: PieceType, x: Float, y: Float, cellSize: Float) {
        val shape = Piece.SHAPES[type]!![0]
        for ((r, c) in shape) {
            drawBlock(canvas, x + c * cellSize, y + r * cellSize, cellSize, type.color)
        }
    }

    private fun drawOverlay(canvas: Canvas, engine: GameEngine, w: Float, h: Float) {
        val cx = w / 2f
        val cy = h / 2f
        when (engine.gameState) {
            GameState.PAUSED -> {
                canvas.drawRect(0f, 0f, w, h, overlayPaint)
                canvas.drawText("PAUSED", cx, cy - 20f, overlayTextPaint)
                canvas.drawText("OK to resume", cx, cy + 40f, overlaySubPaint)
            }
            GameState.GAME_OVER -> {
                canvas.drawRect(0f, 0f, w, h, overlayPaint)
                canvas.drawText("GAME OVER", cx, cy - 40f, overlayTextPaint)
                canvas.drawText("Score: ${engine.score}", cx, cy + 30f, overlaySubPaint)
                canvas.drawText("OK to restart", cx, cy + 70f, overlaySubPaint)
            }
            GameState.MENU -> {
                canvas.drawRect(0f, 0f, w, h, overlayPaint)
                canvas.drawText("MENU", cx, cy - 80f, overlayTextPaint)
                val startY = cy - 20f
                for (i in engine.menuItems.indices) {
                    val my = startY + i * 60f
                    if (i == engine.menuSelection) {
                        canvas.drawRect(cx - 120f, my - 35f, cx + 120f, my + 10f, selPaint)
                        overlaySubPaint.color = Color.WHITE
                    } else {
                        overlaySubPaint.color = Color.rgb(136, 135, 128)
                    }
                    canvas.drawText(engine.menuItems[i], cx, my, overlaySubPaint)
                }
                overlaySubPaint.color = Color.rgb(136, 135, 128)
                canvas.drawText("OK confirm  Back cancel", cx, cy + 120f, overlaySubPaint)
            }
            GameState.EXIT_CONFIRM -> {
                canvas.drawRect(0f, 0f, w, h, overlayPaint)
                canvas.drawText("Exit game?", cx, cy - 20f, overlayTextPaint)
                canvas.drawText("OK to exit  Back to cancel", cx, cy + 40f, overlaySubPaint)
            }
            GameState.IDLE -> {
                canvas.drawRect(0f, 0f, w, h, overlayPaint)
                canvas.drawText("TETRIS TV", cx, cy - 30f, overlayTextPaint)
                canvas.drawText("Press any key to start", cx, cy + 40f, overlaySubPaint)
            }
            else -> {}
        }
    }

    private fun lighten(color: Int, amount: Float): Int {
        val r = (Color.red(color) + (255 - Color.red(color)) * amount).toInt()
        val g = (Color.green(color) + (255 - Color.green(color)) * amount).toInt()
        val b = (Color.blue(color) + (255 - Color.blue(color)) * amount).toInt()
        return Color.rgb(r, g, b)
    }
}
