package com.tetristv.game

enum class GameState {
    IDLE, PLAYING, PAUSED, GAME_OVER, LINE_CLEAR, MENU, EXIT_CONFIRM
}

class GameEngine {
    val board = Board()
    private val randomizer = BagRandomizer()

    var currentPiece: Piece? = null
    var holdPiece: PieceType? = null
    var canHold = true
    var nextQueue = mutableListOf<PieceType>()

    var score = 0
    var level = 1
    var lines = 0
    var highScore = 0
    var combo = -1
    var backToBack = false
    var gameState = GameState.IDLE
    var previousState = GameState.IDLE
    var menuSelection = 0
    val menuItems = arrayOf("Restart", "Exit")

    private var gravityTimer = 0L
    private var lockTimer = -1L
    private var lockResets = 0
    private var lineClearTimer = 0L
    var clearedRows: List<Int> = emptyList()

    companion object {
        private val GRAVITY_TABLE = longArrayOf(
            1000, 793, 618, 473, 355, 262, 190, 135, 94, 64,
            43, 43, 28, 28, 28, 18, 18, 18, 11, 11,
            11, 11, 11, 11, 11, 11, 11, 11, 7
        )
        private const val LOCK_DELAY = 200L
        private const val MAX_LOCK_RESETS = 15
        private const val LINE_CLEAR_DURATION = 300L

        fun gravitySpeed(level: Int): Long {
            val idx = (level - 1).coerceAtMost(GRAVITY_TABLE.size - 1)
            return GRAVITY_TABLE[idx]
        }
    }

    fun start() {
        board.reset()
        randomizer.reset()
        nextQueue.clear()
        fillNextQueue()
        score = 0
        level = 1
        lines = 0
        combo = -1
        backToBack = false
        holdPiece = null
        canHold = true
        gravityTimer = 0
        lockTimer = -1
        lockResets = 0
        lineClearTimer = 0
        clearedRows = emptyList()
        gameState = GameState.PLAYING
        spawnPiece()
    }

    private fun fillNextQueue() {
        while (nextQueue.size < 5) {
            nextQueue.add(randomizer.next())
        }
    }

    private fun spawnPiece() {
        val type = nextQueue.removeAt(0)
        fillNextQueue()
        currentPiece = Piece(type, 0, Piece.spawnX(type), 0)
        canHold = true
        gravityTimer = 0
        lockTimer = -1
        lockResets = 0

        if (board.isGameOver(currentPiece!!)) {
            gameState = GameState.GAME_OVER
            if (score > highScore) highScore = score
        }
    }

    fun moveLeft(): Boolean {
        val piece = currentPiece ?: return false
        if (board.canPlace(piece.cells(-1, 0))) {
            piece.x--
            resetLockDelay()
            return true
        }
        return false
    }

    fun moveRight(): Boolean {
        val piece = currentPiece ?: return false
        if (board.canPlace(piece.cells(1, 0))) {
            piece.x++
            resetLockDelay()
            return true
        }
        return false
    }

    fun rotateCW(): Boolean {
        val piece = currentPiece ?: return false
        if (piece.type == PieceType.O) return false
        val newRot = (piece.rotation + 1) % 4
        val kicks = SrsTable.getKicks(piece.type, piece.rotation, newRot)
        for ((dx, dy) in kicks) {
            if (board.canPlace(piece.cells(dx, dy, newRot))) {
                piece.rotation = newRot
                piece.x += dx
                piece.y += dy
                resetLockDelay()
                return true
            }
        }
        return false
    }

    fun rotateCCW(): Boolean {
        val piece = currentPiece ?: return false
        if (piece.type == PieceType.O) return false
        val newRot = (piece.rotation + 3) % 4
        val kicks = SrsTable.getKicks(piece.type, piece.rotation, newRot)
        for ((dx, dy) in kicks) {
            if (board.canPlace(piece.cells(dx, dy, newRot))) {
                piece.rotation = newRot
                piece.x += dx
                piece.y += dy
                resetLockDelay()
                return true
            }
        }
        return false
    }

    fun softDrop(): Boolean {
        val piece = currentPiece ?: return false
        if (board.canPlace(piece.cells(0, 1))) {
            piece.y++
            score += 1
            gravityTimer = 0
            return true
        }
        return false
    }

    fun hardDrop() {
        val piece = currentPiece ?: return
        var dropDistance = 0
        while (board.canPlace(piece.cells(0, dropDistance + 1))) {
            dropDistance++
        }
        piece.y += dropDistance
        score += dropDistance * 2
        lockTimer = 0
    }

    fun hold() {
        val piece = currentPiece ?: return
        if (!canHold) return
        canHold = false
        if (holdPiece == null) {
            holdPiece = piece.type
            spawnPiece()
        } else {
            val oldHold = holdPiece!!
            holdPiece = piece.type
            currentPiece = Piece(oldHold, 0, Piece.spawnX(oldHold), 0)
            gravityTimer = 0
            lockTimer = -1
            lockResets = 0
        }
    }

    fun getGhostPiece(): Piece? {
        val piece = currentPiece ?: return null
        val ghost = piece.copy()
        var drop = 0
        while (board.canPlace(piece.cells(0, drop + 1))) {
            drop++
        }
        ghost.y += drop
        return ghost
    }

    private fun resetLockDelay() {
        val piece = currentPiece ?: return
        if (!board.canPlace(piece.cells(0, 1))) {
            if (lockResets < MAX_LOCK_RESETS) {
                lockTimer = 0
                lockResets++
            }
        }
    }

    fun update(deltaTimeMs: Long) {
        if (gameState != GameState.PLAYING && gameState != GameState.LINE_CLEAR) return

        if (gameState == GameState.LINE_CLEAR) {
            lineClearTimer += deltaTimeMs
            if (lineClearTimer >= LINE_CLEAR_DURATION) {
                board.clearRows(clearedRows)
                clearedRows = emptyList()
                gameState = GameState.PLAYING
                spawnPiece()
            }
            return
        }

        val piece = currentPiece ?: return
        val onGround = !board.canPlace(piece.cells(0, 1))

        if (onGround) {
            if (lockTimer < 0) lockTimer = 0
            lockTimer += deltaTimeMs
            if (lockTimer >= LOCK_DELAY) {
                board.lockPiece(piece)
                checkLineClears()
            }
        } else {
            lockTimer = -1
            gravityTimer += deltaTimeMs
            val speed = gravitySpeed(level)
            while (gravityTimer >= speed) {
                gravityTimer -= speed
                if (!softDrop()) break
            }
        }
    }

    private fun checkLineClears() {
        val fullRows = board.findFullRows()

        if (fullRows.isNotEmpty()) {
            clearedRows = fullRows
            lineClearTimer = 0
            gameState = GameState.LINE_CLEAR

            val count = fullRows.size
            val isTetris = count == 4
            val baseScore = when (count) {
                1 -> 100
                2 -> 300
                3 -> 500
                4 -> 800
                else -> 0
            }
            var totalScore = baseScore
            if (backToBack && isTetris) totalScore = (totalScore * 1.5).toInt()
            combo++
            if (combo > 0) totalScore += 50 * combo * level
            score += totalScore * level
            lines += count
            backToBack = isTetris

            val newLevel = lines / 10 + 1
            if (newLevel > level) level = newLevel
        } else {
            combo = -1
            spawnPiece()
        }
    }

    fun togglePause() {
        when (gameState) {
            GameState.PLAYING -> gameState = GameState.PAUSED
            GameState.PAUSED -> gameState = GameState.PLAYING
            else -> {}
        }
    }

    fun showMenu() {
        if (gameState != GameState.MENU && gameState != GameState.EXIT_CONFIRM) {
            previousState = gameState
            gameState = GameState.MENU
            menuSelection = 0
        }
    }

    fun hideMenu() {
        gameState = previousState
    }

    fun showExitConfirm() {
        if (gameState != GameState.EXIT_CONFIRM) {
            previousState = gameState
            gameState = GameState.EXIT_CONFIRM
        }
    }

    fun hideExitConfirm() {
        gameState = previousState
    }

    fun menuUp() {
        if (gameState == GameState.MENU) {
            menuSelection = (menuSelection - 1 + menuItems.size) % menuItems.size
        }
    }

    fun menuDown() {
        if (gameState == GameState.MENU) {
            menuSelection = (menuSelection + 1) % menuItems.size
        }
    }

    fun menuConfirm(): Boolean {
        if (gameState != GameState.MENU) return false
        return when (menuSelection) {
            0 -> { start(); true }
            1 -> { true }
            else -> false
        }
    }
}
