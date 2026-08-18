package com.tetristv.render

import android.content.Context
import android.graphics.Canvas
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.tetristv.game.GameEngine
import com.tetristv.input.InputHandler

class GameThread(
    private val holder: SurfaceHolder,
    private val engine: GameEngine,
    private val inputHandler: InputHandler,
    private val renderer: Renderer
) : Thread() {
    @Volatile
    var running = false

    override fun run() {
        running = true
        var lastTime = System.nanoTime()

        while (running) {
            val now = System.nanoTime()
            val dtMs = (now - lastTime) / 1_000_000L
            lastTime = now
            val cappedDt = minOf(dtMs, 100L)

            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    synchronized(holder) {
                        inputHandler.processQueue(engine)
                        engine.update(cappedDt)
                        renderer.draw(canvas, engine)
                    }
                }
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                    }
                }
            }

            val frameTimeMs = (System.nanoTime() - now) / 1_000_000L
            val sleepMs = 16L - frameTimeMs
            if (sleepMs > 0) {
                try {
                    sleep(sleepMs)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }
}

class GameSurfaceView(
    context: Context,
    val engine: GameEngine,
    val inputHandler: InputHandler
) : SurfaceView(context), SurfaceHolder.Callback {

    private val renderer = Renderer()
    private var gameThread: GameThread? = null

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        startThread()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopThread()
    }

    private fun startThread() {
        if (gameThread == null || !gameThread!!.isAlive) {
            gameThread = GameThread(holder, engine, inputHandler, renderer)
            gameThread?.start()
        }
    }

    private fun stopThread() {
        gameThread?.running = false
        try {
            gameThread?.join(1000)
        } catch (e: InterruptedException) {
        }
        gameThread = null
    }

    fun pause() {
        stopThread()
    }

    fun resumeSurface() {
        startThread()
    }
}
