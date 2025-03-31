package ru.snavalstrike.app

import android.content.Context
import android.graphics.*
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlin.random.Random

class GameView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var lastFrameTimeNanos: Long = System.nanoTime()
    private var currentTimeNanos: Long = 0L
    private var deltaTime: Float = 0f

    var currentScene = "start"

    private var screenWidth: Float = 1f
    private var screenHeight: Float = 1f

    private val gameMaps: GameMaps = GameMaps()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w.toFloat()
        screenHeight = h.toFloat()

        gameMaps.init(screenWidth, screenHeight)
    }

    override fun onDraw(canvas: Canvas) {
        currentTimeNanos = System.nanoTime()
        deltaTime = (currentTimeNanos - lastFrameTimeNanos) / 1000000000f
        lastFrameTimeNanos = currentTimeNanos

        when (currentScene) {
            "start" -> {
                canvas.drawColor(Color.BLACK)
                gameMaps.draw(canvas, isLeft = true, isMini = false, isDarkened = false)
                gameMaps.draw(canvas, isLeft = false, isMini = true, isDarkened = true)
            }
            "gameoverB" -> {}
            "exit" -> {}
        }

        postInvalidate()
    }

    inner class GameMaps {
        private val mapBackground: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.map_background)

        private val leftRectF: RectF = RectF(0.0f, 0.0f, 0.0f, 0.0f)
        private val leftMiniRectF: RectF = RectF(0.0f, 0.0f, 0.0f, 0.0f)
        private val leftRemainRectF: RectF = RectF(0.0f, 0.0f, 0.0f, 0.0f)
        private val rightRectF: RectF = RectF(0.0f, 0.0f, 0.0f, 0.0f)
        private val rightMiniRectF: RectF = RectF(0.0f, 0.0f, 0.0f, 0.0f)
        private val rightRemainRectF: RectF = RectF(0.0f, 0.0f, 0.0f, 0.0f)

        private val paintBackground: Paint = Paint()
        private val paintDarkened: Paint = Paint().apply { color = Color.BLACK; alpha = 150 }
        private val paintLines: Paint = Paint().apply { color = Color.WHITE; isAntiAlias = true }

        private val linesFloatArray: FloatArray = FloatArray(4 * 72) { 0.0f }

        fun init(w: Float, h: Float) {
            leftRectF.left = 0.0f
            leftRectF.top = 0.0f
            leftRectF.bottom = h
            leftRectF.right = h

            rightRectF.left = w - h
            rightRectF.top = 0.0f
            rightRectF.bottom = h
            rightRectF.right = w

            if ((w - h) < h) { // вертикальный остаток
                leftMiniRectF.left = (w - h) / 12.0f
                leftMiniRectF.top = h / 2.0f - 5.0f * (w - h) / 12.0f
                leftMiniRectF.right = 11.0f * (w - h) / 12.0f
                leftMiniRectF.bottom = h / 2.0f + 5.0f * (w - h) / 12.0f

                rightMiniRectF.left = h + (w - h) / 12.0f
                rightMiniRectF.top = h / 2.0f - 5.0f * (w - h) / 12.0f
                rightMiniRectF.right = h + 11.0f * (w - h) / 12.0f
                rightMiniRectF.bottom = h / 2.0f + 5.0f * (w - h) / 12.0f
            } else { // горизонтальный остаток
                leftMiniRectF.left = (w - h) / 2.0f - 5.0f * h / 12.0f
                leftMiniRectF.top = h / 12.0f
                leftMiniRectF.right = (w - h) / 2.0f + 5.0f * h / 12.0f
                leftMiniRectF.bottom = 11.0f * h / 12.0f

                rightMiniRectF.left = h + (w - h) / 2.0f - 5.0f * h / 12.0f
                rightMiniRectF.top = h / 12.0f
                rightMiniRectF.right = h + (w - h) / 2.0f + 5.0f * h / 12.0f
                rightMiniRectF.bottom = 11.0f * h / 12.0f
            }

            leftRemainRectF.left = 0.0f
            leftRemainRectF.top = 0.0f
            leftRemainRectF.right = w - h
            leftRemainRectF.bottom = h

            rightRemainRectF.left = h
            rightRemainRectF.top = 0.0f
            rightRemainRectF.right = w
            rightRemainRectF.bottom = h

            paintLines.strokeWidth = h / 200.0f
            for (i in 0 until 9) {
                linesFloatArray[i * 4] = (i + 1) * h / 10.0f
                linesFloatArray[i * 4 + 1] = 0.0f
                linesFloatArray[i * 4 + 2] = (i + 1) * h / 10.0f
                linesFloatArray[i * 4 + 3] = h
                linesFloatArray[(i + 9) * 4] = 0.0f
                linesFloatArray[(i + 9) * 4 + 1] = (i + 1) * h / 10.0f
                linesFloatArray[(i + 9) * 4 + 2] = h
                linesFloatArray[(i + 9) * 4 + 3] = (i + 1) * h / 10.0f

                linesFloatArray[i * 4 + 18]

                linesFloatArray[i * 4 + 27]

                linesFloatArray[i * 4 + 36]

                linesFloatArray[i + 45]

                linesFloatArray[i + 54]

                linesFloatArray[i + 63]
            }
        }

        private fun drawLines(canvas: Canvas, isLeft: Boolean, isMini: Boolean) {
            //
            //
        }

        fun draw(canvas: Canvas, isLeft: Boolean, isMini: Boolean, isDarkened: Boolean) {
            // draw epmty map
            when {
                isLeft && !isMini -> {
                    canvas.drawBitmap(mapBackground, null, leftRectF, paintBackground)
                }
                isLeft && isMini -> {
                    canvas.drawBitmap(mapBackground, null, leftMiniRectF, paintBackground)
                }
                !isLeft && !isMini -> {
                    canvas.drawBitmap(mapBackground, null, rightRectF, paintBackground)
                }
                else -> { // !isLeft && isMini
                    canvas.drawBitmap(mapBackground, null, rightMiniRectF, paintBackground)
                }
            }

            // draw lines
            when {
                isLeft && !isMini -> {
                    canvas.drawLines(linesFloatArray, paintLines)
                }
                isLeft && isMini -> {

                }
                !isLeft && !isMini -> {

                }
                else -> { // !isLeft && isMini

                }
            }

            // draw darkened
            if (isDarkened && isLeft && isMini) canvas.drawRect(leftRemainRectF, paintDarkened)
            if (isDarkened && !isLeft && isMini) canvas.drawRect(rightRemainRectF, paintDarkened)
        }
    }
}