package com.tetristv.input

import android.view.KeyEvent
import com.tetristv.game.GameEngine
import com.tetristv.game.GameState
import java.util.concurrent.ConcurrentLinkedQueue

enum class KeyAction {
    MOVE_LEFT, MOVE_RIGHT, ROTATE_CW, ROTATE_CCW,
    SOFT_DROP, HARD_DROP, HOLD,
    BACK, MENU, CONFIRM,
    QUICK_RESTART
}

class InputHandler {
    private val actionQueue = ConcurrentLinkedQueue<KeyAction>()

    private var lastLeftTime = 0L
    private var lastRightTime = 0L
    private var lastRotateTime = 0L

    private var softDropHeld = false
    private var softDropPressTime = 0L
    private var softDropLastRepeat = 0L

    fun mapKey(keyCode: Int): KeyAction? = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> KeyAction.MOVE_LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> KeyAction.MOVE_RIGHT
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> KeyAction.ROTATE_CW
        KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> KeyAction.SOFT_DROP
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_SPACE -> KeyAction.HARD_DROP
        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_DEL -> KeyAction.BACK
        KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_M -> KeyAction.MENU
        KeyEvent.KEYCODE_ENTER -> KeyAction.CONFIRM
        KeyEvent.KEYCODE_P -> KeyAction.BACK
        KeyEvent.KEYCODE_R -> KeyAction.QUICK_RESTART
        KeyEvent.KEYCODE_C -> KeyAction.HOLD
        KeyEvent.KEYCODE_Z -> KeyAction.ROTATE_CCW
        else -> null
    }

    fun handleKeyDown(keyCode: Int, repeatCount: Int): Boolean {
        val action = mapKey(keyCode) ?: return false
        val now = System.currentTimeMillis()

        when (action) {
            KeyAction.SOFT_DROP -> {
                if (repeatCount == 0) {
                    softDropHeld = true
                    softDropPressTime = now
                    softDropLastRepeat = now
                    actionQueue.add(KeyAction.SOFT_DROP)
                }
            }
            KeyAction.MOVE_LEFT -> {
                if (now - lastLeftTime >= 50) {
                    actionQueue.add(KeyAction.MOVE_LEFT)
                    lastLeftTime = now
                }
            }
            KeyAction.MOVE_RIGHT -> {
                if (now - lastRightTime >= 50) {
                    actionQueue.add(KeyAction.MOVE_RIGHT)
                    lastRightTime = now
                }
            }
            KeyAction.ROTATE_CW, KeyAction.ROTATE_CCW -> {
                if (now - lastRotateTime >= 100) {
                    actionQueue.add(action)
                    lastRotateTime = now
                }
            }
            KeyAction.HARD_DROP, KeyAction.HOLD -> {
                if (repeatCount == 0) actionQueue.add(action)
            }
            else -> {}
        }
        return true
    }

    fun handleKeyUp(keyCode: Int): Boolean {
        val action = mapKey(keyCode) ?: return false
        if (action == KeyAction.SOFT_DROP) softDropHeld = false
        return true
    }

    fun processQueue(engine: GameEngine) {
        if (engine.gameState != GameState.PLAYING) {
            actionQueue.clear()
            return
        }

        while (true) {
            val action = actionQueue.poll() ?: break
            when (action) {
                KeyAction.MOVE_LEFT -> engine.moveLeft()
                KeyAction.MOVE_RIGHT -> engine.moveRight()
                KeyAction.ROTATE_CW -> engine.rotateCW()
                KeyAction.ROTATE_CCW -> engine.rotateCCW()
                KeyAction.SOFT_DROP -> engine.softDrop()
                KeyAction.HARD_DROP -> engine.hardDrop()
                KeyAction.HOLD -> engine.hold()
                else -> {}
            }
        }

        if (softDropHeld) {
            val now = System.currentTimeMillis()
            if (now - softDropPressTime >= 300 && now - softDropLastRepeat >= 50) {
                engine.softDrop()
                softDropLastRepeat = now
            }
        }
    }

    fun reset() {
        actionQueue.clear()
        softDropHeld = false
        lastLeftTime = 0
        lastRightTime = 0
        lastRotateTime = 0
    }
}
