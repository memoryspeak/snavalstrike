package ru.snavalstrike.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlin.random.Random

class GameView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var lastFrameTimeNanos: Long = System.nanoTime()
    private var currentTimeNanos: Long = 0L
    private var deltaTime: Float = 0f

    var currentScene = "start"
    var gameTime: Float = 180.0f

    private var screenWidth: Float = 1f
    private var screenHeight: Float = 1f

    private val gameMaps: GameMaps = GameMaps()
    private val gamePopups: GamePopup = GamePopup()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w.toFloat()
        screenHeight = h.toFloat()

        gameMaps.init(screenWidth, screenHeight)
        gamePopups.init(screenWidth, screenHeight)
    }

    override fun onDraw(canvas: Canvas) {
        currentTimeNanos = System.nanoTime()
        deltaTime = (currentTimeNanos - lastFrameTimeNanos) / 1000000000f
        lastFrameTimeNanos = currentTimeNanos

        gameTime -= deltaTime

        when (currentScene) {
            "start" -> {
                canvas.drawColor(Color.BLACK)
                gameMaps.draw(canvas, isLeft = true, isMini = false, isDarkened = false)
                gameMaps.draw(canvas, isLeft = false, isMini = true, isDarkened = true)
                gamePopups.draw(canvas, name = "start")
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

        private val linesLeftFloatArray: FloatArray = FloatArray(4 * 18) { 0.0f }
        private val linesRightFloatArray: FloatArray = FloatArray(4 * 18) { 0.0f }
        private val linesLeftMiniFloatArray: FloatArray = FloatArray(4 * 18) { 0.0f }
        private val linesRightMiniFloatArray: FloatArray = FloatArray(4 * 18) { 0.0f }

        private val paintBackground: Paint = Paint()
        private val paintDarkened: Paint = Paint().apply { color = Color.BLACK; alpha = 150 }
        private val paintLines: Paint = Paint().apply { color = Color.WHITE; isAntiAlias = true }

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
            // startX, startY, endX, endY
            for (i in 0 until 9) {
                // вертикальные линии левого большого квадрата
                linesLeftFloatArray[i * 4] = leftRectF.left + (i + 1) * (leftRectF.right - leftRectF.left) / 10.0f
                linesLeftFloatArray[i * 4 + 1] = leftRectF.top
                linesLeftFloatArray[i * 4 + 2] = leftRectF.left + (i + 1) * (leftRectF.right - leftRectF.left) / 10.0f
                linesLeftFloatArray[i * 4 + 3] = leftRectF.bottom

                // горизонтальные линии левого большого квардрата
                linesLeftFloatArray[(i + 9) * 4] = leftRectF.left
                linesLeftFloatArray[(i + 9) * 4 + 1] = leftRectF.top + (i + 1) * (leftRectF.bottom - leftRectF.top) / 10.0f
                linesLeftFloatArray[(i + 9) * 4 + 2] = leftRectF.right
                linesLeftFloatArray[(i + 9) * 4 + 3] = leftRectF.top + (i + 1) * (leftRectF.bottom - leftRectF.top) / 10.0f

                // вертикальные линии правого большого квадрата
                linesRightFloatArray[i * 4] = rightRectF.left + (i + 1) * (rightRectF.right - rightRectF.left) / 10.0f
                linesRightFloatArray[i * 4 + 1] = rightRectF.top
                linesRightFloatArray[i * 4 + 2] = rightRectF.left + (i + 1) * (rightRectF.right - rightRectF.left) / 10.0f
                linesRightFloatArray[i * 4 + 3] = rightRectF.bottom

                // горизонтальные линии правого большого квардрата
                linesRightFloatArray[(i + 9) * 4] = rightRectF.left
                linesRightFloatArray[(i + 9) * 4 + 1] = rightRectF.top + (i + 1) * (rightRectF.bottom - rightRectF.top) / 10.0f
                linesRightFloatArray[(i + 9) * 4 + 2] = rightRectF.right
                linesRightFloatArray[(i + 9) * 4 + 3] = rightRectF.top + (i + 1) * (rightRectF.bottom - rightRectF.top) / 10.0f

                // вертикальные линии левого малого квадрата
                linesLeftMiniFloatArray[i * 4] = leftMiniRectF.left + (i + 1) * (leftMiniRectF.right - leftMiniRectF.left) / 10.0f
                linesLeftMiniFloatArray[i * 4 + 1] = leftMiniRectF.top
                linesLeftMiniFloatArray[i * 4 + 2] = leftMiniRectF.left + (i + 1) * (leftMiniRectF.right - leftMiniRectF.left) / 10.0f
                linesLeftMiniFloatArray[i * 4 + 3] = leftMiniRectF.bottom

                // горизонтальные линии левого малого квадрата
                linesLeftMiniFloatArray[(i + 9) * 4] = leftMiniRectF.left
                linesLeftMiniFloatArray[(i + 9) * 4 + 1] = leftMiniRectF.top + (i + 1) * (leftMiniRectF.bottom - leftMiniRectF.top) / 10.0f
                linesLeftMiniFloatArray[(i + 9) * 4 + 2] = leftMiniRectF.right
                linesLeftMiniFloatArray[(i + 9) * 4 + 3] = leftMiniRectF.top + (i + 1) * (leftMiniRectF.bottom - leftMiniRectF.top) / 10.0f

                // вертикальные линии правого малого квадрата
                linesRightMiniFloatArray[i * 4] = rightMiniRectF.left + (i + 1) * (rightMiniRectF.right - rightMiniRectF.left) / 10.0f
                linesRightMiniFloatArray[i * 4 + 1] = rightMiniRectF.top
                linesRightMiniFloatArray[i * 4 + 2] = rightMiniRectF.left + (i + 1) * (rightMiniRectF.right - rightMiniRectF.left) / 10.0f
                linesRightMiniFloatArray[i * 4 + 3] = rightMiniRectF.bottom

                // горизонтальные линии правого малого квадрата
                linesRightMiniFloatArray[(i + 9) * 4] = rightMiniRectF.left
                linesRightMiniFloatArray[(i + 9) * 4 + 1] = rightMiniRectF.top + (i + 1) * (rightMiniRectF.bottom - rightMiniRectF.top) / 10.0f
                linesRightMiniFloatArray[(i + 9) * 4 + 2] = rightMiniRectF.right
                linesRightMiniFloatArray[(i + 9) * 4 + 3] = rightMiniRectF.top + (i + 1) * (rightMiniRectF.bottom - rightMiniRectF.top) / 10.0f
            }
        }

        fun draw(canvas: Canvas, isLeft: Boolean, isMini: Boolean, isDarkened: Boolean) {
            // draw epmty map + lines
            when {
                isLeft && !isMini -> {
                    canvas.drawBitmap(mapBackground, null, leftRectF, paintBackground)
                    canvas.drawLines(linesLeftFloatArray, paintLines)
                }
                isLeft && isMini -> {
                    canvas.drawBitmap(mapBackground, null, leftMiniRectF, paintBackground)
                    canvas.drawLines(linesLeftMiniFloatArray, paintLines)
                }
                !isLeft && !isMini -> {
                    canvas.drawBitmap(mapBackground, null, rightRectF, paintBackground)
                    canvas.drawLines(linesRightFloatArray, paintLines)
                }
                else -> { // !isLeft && isMini
                    canvas.drawBitmap(mapBackground, null, rightMiniRectF, paintBackground)
                    canvas.drawLines(linesRightMiniFloatArray, paintLines)
                }
            }

            // draw darkened
            if (isDarkened && isLeft && isMini) canvas.drawRect(leftRemainRectF, paintDarkened)
            if (isDarkened && !isLeft && isMini) canvas.drawRect(rightRemainRectF, paintDarkened)
        }
    }

    inner class GamePopup() {

        private val backgroundPaint = Paint().apply { color = Color.BLACK; alpha = 150 }

        private val titlePaint = Paint().apply { color = Color.argb(200, 255, 255, 255); textAlign = Paint.Align.CENTER; textSize = 0.0f; isAntiAlias = true }
        private var titleStartXPosition: Float = 0.0f
        private var titleStartYPosition: Float = 0.0f

        private val subtitleRoundRectF: RectF = RectF(0.0f, 0.0f, 0.0f, 0.0f)
        private var subtitleRoundRectFCornerRadius: Float = 0.0f
        private val subtitleRoundRectFPaint = Paint().apply { color = Color.BLACK; alpha = 100; style = Paint.Style.FILL; isAntiAlias = true }
        private var subtitleText = ""
        private val subtitleTextPaint: Paint = Paint().apply { color = Color.RED; isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 0.0f }
        private var subtitleTextStartXPosition: Float = 0.0f
        private var subtitleTextStartYPosition: Float = 0.0f

        private val leftButtonRoundRectF: RectF = RectF(0.0f, 0.0f, 0.0f, 0.0f)
        private val centralButtonRoundRectF: RectF = RectF(0.0f, 0.0f, 0.0f, 0.0f)
        private val rightButtonRoundRectF: RectF = RectF(0.0f, 0.0f, 0.0f, 0.0f)

        private var buttonCornerRadius: Float = 0.0f

        private val buttonPaint = Paint().apply { color = Color.argb(100, 255, 255, 255); style = Paint.Style.FILL; isAntiAlias = true }

        private val rightButtonTextPaint: Paint = Paint().apply { color = Color.BLACK; isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 0.0f; }
        private var rightButtonTextStartXPosition: Float = 0.0f
        private var rightButtonTextStartYPosition: Float = 0.0f

        private val centralButtonTextPaint: Paint = Paint().apply { color = Color.BLACK; isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 0.0f; }
        private var centralButtonTextStartXPosition: Float = 0.0f
        private var centralButtonTextStartYPosition: Float = 0.0f

        private val leftButtonTextPaint: Paint = Paint().apply { color = Color.BLACK; isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 0.0f; }
        private var leftButtonTextStartXPosition: Float = 0.0f
        private var leftButtonTextStartYPosition: Float = 0.0f

        fun init(w: Float, h: Float) {
            titlePaint.textSize = h / 12.0f
            titleStartXPosition = w / 2.0f
            titleStartYPosition = 2.0f * h / 10.0f

            leftButtonRoundRectF.left = 1.5f * h / 10.0f
            leftButtonRoundRectF.top = 6.5f * h / 10.0f
            leftButtonRoundRectF.right = 7.5f * h / 10.0f
            leftButtonRoundRectF.bottom = 8.5f * h / 10.0f

            centralButtonRoundRectF.left = w / 2.0f - 3.0f * h / 10.0f
            centralButtonRoundRectF.top = 6.5f * h / 10.0f
            centralButtonRoundRectF.right = w / 2.0f + 3.0f * h / 10.0f
            centralButtonRoundRectF.bottom = 8.5f * h / 10.0f

            rightButtonRoundRectF.left = w - 7.5f * h / 10.0f
            rightButtonRoundRectF.top = 6.5f * h / 10.0f
            rightButtonRoundRectF.right = w - 1.5f * h / 10.0f
            rightButtonRoundRectF.bottom = 8.5f * h / 10.0f

            buttonCornerRadius = h / 40.0f

            rightButtonTextPaint.textSize = h / 15.0f
            rightButtonTextStartXPosition = (rightButtonRoundRectF.right + rightButtonRoundRectF.left) / 2.0f
            rightButtonTextStartYPosition = (rightButtonRoundRectF.bottom + rightButtonRoundRectF.top) / 2.0f + h / 40.0f

            centralButtonTextPaint.textSize = h / 15.0f
            centralButtonTextStartXPosition = (centralButtonRoundRectF.right + centralButtonRoundRectF.left) / 2.0f
            centralButtonTextStartYPosition = (centralButtonRoundRectF.bottom + centralButtonRoundRectF.top) / 2.0f + h / 40.0f

            leftButtonTextPaint.textSize = h / 15.0f
            leftButtonTextStartXPosition = (leftButtonRoundRectF.right + leftButtonRoundRectF.left) / 2.0f
            leftButtonTextStartYPosition = (leftButtonRoundRectF.bottom + leftButtonRoundRectF.top) / 2.0f + h / 40.0f

            subtitleTextPaint.textSize = h / 20.0f
            subtitleRoundRectF.left = w / 2.0f - 3.0f * h / 10.0f
            subtitleRoundRectF.top = 4.75f * h / 10.0f
            subtitleRoundRectF.right = w / 2.0f + 3.0f * h / 10.0f
            subtitleRoundRectF.bottom = 5.75f * h / 10.0f
            subtitleRoundRectFCornerRadius = h / 40.0f
            subtitleTextStartXPosition = (subtitleRoundRectF.right + subtitleRoundRectF.left) / 2.0f
            subtitleTextStartYPosition = (subtitleRoundRectF.bottom + subtitleRoundRectF.top) / 2.0f + h / 80.0f
        }

        fun draw(canvas: Canvas, name: String) {
            when (name) {
                "start" -> {
                    canvas.drawPaint(backgroundPaint)
                    drawTitle(canvas, "Place the snake\non the left side of the screen\nby clicking on the cells")
                    drawCentralButton(canvas, "OK")
                    val minutes: Int = (gameTime / 60.0f).toInt()
                    val seconds: Int = (gameTime % 60.0f).toInt()
                    val milliseconds: Int = ((gameTime % 60.0f - seconds.toFloat()) * 1000).toInt()
                    subtitleText = if (milliseconds < 500) {
                        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}}"
                    } else {
                        "${minutes.toString().padStart(2, '0')} ${seconds.toString().padStart(2, '0')}}"
                    }
                    drawSubtitle(canvas)
                }
            }
        }

        private fun drawTitle(canvas: Canvas, text: String) {
            var currentTextYPosition = titleStartYPosition
            val lines = text.split("\n")
            lines.forEach { line ->
                canvas.drawText(line, titleStartXPosition, currentTextYPosition, titlePaint)
                currentTextYPosition += titlePaint.descent() - titlePaint.ascent()
            }
        }

        private fun drawSubtitle(canvas: Canvas) {
            canvas.drawRoundRect(subtitleRoundRectF, subtitleRoundRectFCornerRadius, subtitleRoundRectFCornerRadius, subtitleRoundRectFPaint)
            canvas.drawText(subtitleText, subtitleTextStartXPosition, subtitleTextStartYPosition, subtitleTextPaint)
        }

        private fun drawRightButton(canvas: Canvas, text: String = "OK", color: Int = Color.BLACK) {
            canvas.drawRoundRect(rightButtonRoundRectF, buttonCornerRadius, buttonCornerRadius, buttonPaint)
            rightButtonTextPaint.color = color
            canvas.drawText(text, rightButtonTextStartXPosition, rightButtonTextStartYPosition, rightButtonTextPaint)
        }

        private fun drawCentralButton(canvas: Canvas, text: String = "OK", color: Int = Color.BLACK) {
            canvas.drawRoundRect(centralButtonRoundRectF, buttonCornerRadius, buttonCornerRadius, buttonPaint)
            centralButtonTextPaint.color = color
            canvas.drawText(text, centralButtonTextStartXPosition, centralButtonTextStartYPosition, centralButtonTextPaint)
        }

        private fun drawLeftButton(canvas: Canvas, text: String = "CANCEL", color: Int = Color.BLACK) {
            canvas.drawRoundRect(leftButtonRoundRectF, buttonCornerRadius, buttonCornerRadius, buttonPaint)
            leftButtonTextPaint.color = color
            canvas.drawText(text, leftButtonTextStartXPosition, leftButtonTextStartYPosition, leftButtonTextPaint)
        }
    }
}