package com.tetristv

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import com.tetristv.game.GameEngine
import com.tetristv.game.GameState
import com.tetristv.input.InputHandler
import com.tetristv.input.KeyAction
import com.tetristv.render.GameSurfaceView

class MainActivity : Activity() {

    private lateinit var surfaceView: GameSurfaceView
    private lateinit var engine: GameEngine
    private lateinit var inputHandler: InputHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = GameEngine()
        inputHandler = InputHandler()
        surfaceView = GameSurfaceView(this, engine, inputHandler)
        setContentView(surfaceView)
    }

    override fun onPause() {
        super.onPause()
        if (engine.gameState == GameState.PLAYING) {
            engine.gameState = GameState.PAUSED
        }
        surfaceView.pause()
    }

    override fun onResume() {
        super.onResume()
        surfaceView.resumeSurface()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val action = inputHandler.mapKey(keyCode)
        if (action == null) {
            return super.onKeyDown(keyCode, event)
        }

        when (engine.gameState) {
            GameState.IDLE -> {
                engine.start()
            }
            GameState.PLAYING -> {
                when (action) {
                    KeyAction.BACK -> { engine.gameState = GameState.PAUSED }
                    KeyAction.MENU -> { engine.showMenu() }
                    KeyAction.QUICK_RESTART -> { engine.start() }
                    else -> { inputHandler.handleKeyDown(keyCode, event.repeatCount) }
                }
            }
            GameState.PAUSED -> {
                when (action) {
                    KeyAction.CONFIRM, KeyAction.HARD_DROP -> { engine.gameState = GameState.PLAYING }
                    KeyAction.BACK -> { engine.showExitConfirm() }
                    KeyAction.MENU -> { engine.showMenu() }
                    else -> {}
                }
            }
            GameState.GAME_OVER -> {
                when (action) {
                    KeyAction.CONFIRM, KeyAction.HARD_DROP -> { engine.start() }
                    KeyAction.BACK -> { engine.showExitConfirm() }
                    KeyAction.MENU -> { engine.showMenu() }
                    else -> {}
                }
            }
            GameState.MENU -> {
                when (action) {
                    KeyAction.ROTATE_CW, KeyAction.MOVE_LEFT -> { engine.menuUp() }
                    KeyAction.SOFT_DROP, KeyAction.MOVE_RIGHT -> { engine.menuDown() }
                    KeyAction.CONFIRM, KeyAction.HARD_DROP -> {
                        when (engine.menuSelection) {
                            0 -> { engine.start() }
                            1 -> { finish() }
                        }
                    }
                    KeyAction.BACK -> { engine.hideMenu() }
                    else -> {}
                }
            }
            GameState.EXIT_CONFIRM -> {
                when (action) {
                    KeyAction.CONFIRM, KeyAction.HARD_DROP -> { finish() }
                    KeyAction.BACK -> { engine.hideExitConfirm() }
                    else -> {}
                }
            }
            else -> {}
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        inputHandler.handleKeyUp(keyCode)
        return true
    }
}
