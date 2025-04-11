package ru.snavalstrike.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.floor

class GameView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var lastFrameTimeNanos: Long = System.nanoTime()
    private var currentTimeNanos: Long = 0L
    private var deltaTime: Float = 0f

    var currentScene = "start"
    var gameTime: Float = 120.0f

    private var screenWidth: Float = 1f
    private var screenHeight: Float = 1f

    private val gameMaps: GameMaps = GameMaps()
    private val gamePopups: GamePopup = GamePopup()
    private val gameArrows: GameArrows = GameArrows()
    private val gameSnake: GameSnake = GameSnake()

    private var hoverRectF: RectF? = null

    var state: String = "AA" // AA, AB, BA, BB
    // AA - активна левая большая карта, пользователь работает со змеёй
    // AB - активна правая большая карта, пользователь кидает в соперника
    // BA - активна правая большая карта, пользователь ждет, когда соперник передвинет змею
    // AA - активна левая большая карта, пользователь ждет, когда соперник в него кинет

    //private val snakeList: MutableList<MutableList<Cell>> = mutableListOf(mutableListOf())

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w.toFloat()
        screenHeight = h.toFloat()

        gameMaps.init(screenWidth, screenHeight)
        gamePopups.init(screenWidth, screenHeight)
        gameArrows.init()
        gameSnake.init()
    }

    override fun onDraw(canvas: Canvas) {
        currentTimeNanos = System.nanoTime()
        deltaTime = (currentTimeNanos - lastFrameTimeNanos) / 1000000000f
        lastFrameTimeNanos = currentTimeNanos

        if (currentScene != "game_over") {
            gameTime -= deltaTime
            if (gameTime <= 0.0f) {
                gameTime = 0.0f
                currentScene = "game_over"
            }
        }

        when (currentScene) {
            "start" -> {
                canvas.drawColor(Color.BLACK)
                gameMaps.draw(canvas)
                gamePopups.draw(canvas)
                gameArrows.draw(canvas)
                gameSnake.draw(canvas)
            }
            "preparing" -> {
                canvas.drawColor(Color.BLACK)
                gameMaps.draw(canvas)
                gamePopups.draw(canvas)
                gameArrows.draw(canvas)
                gameSnake.draw(canvas)
            }
            "game_over" -> {}
            "exit" -> {}
        }

        postInvalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        val x: Float = event.x
        val y: Float = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                when (currentScene) {
                    "start" -> {
                        if (pointInRectF(x, y, gamePopups.centralButtonRoundRectF)) {
                            hoverRectF = gamePopups.centralButtonRoundRectF
                        } else {
                            hoverRectF = null
                        }
                    }
                    "preparing" -> {
                        // проверим, что мы в правой области
                        if (pointInRectF(x, y, gamePopups.rightMiniButtonRoundRectF)) {
                            hoverRectF = gamePopups.rightMiniButtonRoundRectF
                        // или в левой области
                        } else if (pointInRectF(x, y, gameMaps.leftRectF)) {
                            // найдем клетку, в которую ткнул пользователь
                            val col: Int = floor(x / (screenHeight / 10.0f)).toInt()
                            val row: Int = floor(y / (screenHeight / 10.0f)).toInt()
                            // найдем список голов подзмеи
                            val snakeHeaders: List<Cell> = gameSnake.getHeaders(col, row)
                            if (snakeHeaders.isEmpty()) {
                                // если список голов подзмеи пустой, это значит:
                                when {
                                    // 1. или нет ни одной подзмеи - значит ставим первый кусок змеи
                                    gameSnake.isEmpty() -> gameSnake.addFirstCell(col, row)
                                    // 2. или ткнули мимо змеи, но попали в стрелочку - значит ставим на место стрелочки кусок змеи
                                    gameArrows.hasCell(col, row) -> gameSnake.addCell(col, row)
                                    // 3. или ткнули мимо змеи и мимо стрелочек - значит убираем крест на голове змеи
                                    else -> gameSnake.removeCrosses()
                                }
                                // в любом случае - убираем стрелочки
                                gameArrows.list.clear()
                            } else {
                                // если ткнули в подзмею
                                if (gameArrows.list.isEmpty()) {
                                    // если список стрелочек пуст - рисуем стрелочки + рисуем крест у головы змеи
                                    for (cell in snakeHeaders) {
                                        if ((cell.col - 1) >= 0 && !gameSnake.hasCell(cell.col - 1, cell.row)) gameArrows.list.add(Cell(cell.col - 1, cell.row, direction = "left"))
                                        if ((cell.col + 1) <= 9 && !gameSnake.hasCell(cell.col + 1, cell.row)) gameArrows.list.add(Cell(cell.col + 1, cell.row, direction = "right"))
                                        if ((cell.row - 1) >= 0 && !gameSnake.hasCell(cell.col, cell.row - 1)) gameArrows.list.add(Cell(cell.col, cell.row - 1, direction = "top"))
                                        if ((cell.row + 1) <= 9 && !gameSnake.hasCell(cell.col, cell.row + 1)) gameArrows.list.add(Cell(cell.col, cell.row + 1, direction = "bottom"))
                                        gameSnake.setCross(cell.col, cell.row)
                                    }
                                } else {
                                    // если в момент касания отображались стрелочки - удаляем их и удаляем кресты на головах
                                    gameSnake.removeCrosses()
                                    gameArrows.list.clear()
                                }
                                for (cell in snakeHeaders) {
                                    if (cell.col == col && cell.row == row) {
                                        // если нажали на голову - удаляем голову
                                        if (cell.isDelete) {
                                            gameSnake.removeCell(cell.col, cell.row)
                                            break
                                        }
                                    }
                                }
                            }
                        } else {
                            hoverRectF = null
                        }
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                when (currentScene) {
                    "start" -> {
                        if (pointInRectF(x, y, gamePopups.centralButtonRoundRectF)) {
                            hoverRectF = gamePopups.centralButtonRoundRectF
                        } else {
                            hoverRectF = null
                        }
                    }
                    "preparing" -> {

                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                when (currentScene) {
                    "start" -> {
                        if (pointInRectF(x, y, gamePopups.centralButtonRoundRectF)) {
                            currentScene = "preparing"
                        }
                        hoverRectF = null
                    }
                    "preparing" -> {
                        if (pointInRectF(x, y, gamePopups.rightMiniButtonRoundRectF)) {
                            currentScene = "TODO"
                        } else if (pointInRectF(x, y, gameMaps.leftRectF)) {
                            //
                        }
                        hoverRectF = null
                    }
                }
            }
        }
        return true
    }

    private fun pointInRectF(x: Float, y: Float, rectF: RectF) : Boolean {
        if (x > rectF.left && x < rectF.right && y > rectF.top && y < rectF.bottom) return true
        return false
    }

    private fun isRectFsEqual(rectFA: RectF?, rectFB: RectF?) : Boolean {
        if (rectFA == null) return false
        if (rectFB == null) return false
        if (rectFA.left == rectFB.left && rectFA.top == rectFB.top && rectFA.right == rectFB.right && rectFA.bottom == rectFB.bottom) return true
        return false
    }

    private fun loadBitmap(resId: Int, context: Context, targetWidth: Int, targetHeight: Int): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, options)
        options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565
        return BitmapFactory.decodeResource(context.resources, resId, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        if (reqWidth <= 0 || reqHeight <= 0) return 1
        val (originalHeight: Int, originalWidth: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (originalHeight > reqHeight || originalWidth > reqWidth) {
            val halfHeight = originalHeight / 2
            val halfWidth = originalWidth / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) inSampleSize *= 2
        }
        return inSampleSize
    }

    data class Cell (val col: Int, val row: Int, var direction: String? = null, var isDelete: Boolean = false)

    inner class GameSnake() {
        private val list: MutableList<MutableList<Cell>> = mutableListOf(mutableListOf())

        private var delete: Bitmap? = null
        private var circleGreen: Bitmap? = null
        private var circleLeftGreen: Bitmap? = null
        private var circleTopGreen: Bitmap? = null
        private var circleRightGreen: Bitmap? = null
        private var circleBottomGreen: Bitmap? = null
        private var leftTopGreen: Bitmap? = null
        private var topRightGreen: Bitmap? = null
        private var rightBottomGreen: Bitmap? = null
        private var bottomLeftGreen: Bitmap? = null
        private var verticalGreen: Bitmap? = null
        private var horizontalGreen: Bitmap? = null

        fun init() {
            val cellSize: Int = (screenHeight / 10.0f).toInt()
            delete = loadBitmap(R.drawable.delete, context, cellSize, cellSize)
            circleGreen = loadBitmap(R.drawable.snake_circle_green, context, cellSize, cellSize)
            circleLeftGreen = loadBitmap(R.drawable.snake_circle_left_green, context, cellSize, cellSize)
            circleTopGreen = loadBitmap(R.drawable.snake_circle_top_green, context, cellSize, cellSize)
            circleRightGreen = loadBitmap(R.drawable.snake_circle_right_green, context, cellSize, cellSize)
            circleBottomGreen = loadBitmap(R.drawable.snake_circle_bottom_green, context, cellSize, cellSize)
            leftTopGreen = loadBitmap(R.drawable.snake_left_top_green, context, cellSize, cellSize)
            topRightGreen = loadBitmap(R.drawable.snake_top_right_green, context, cellSize, cellSize)
            rightBottomGreen = loadBitmap(R.drawable.snake_right_bottom_green, context, cellSize, cellSize)
            bottomLeftGreen = loadBitmap(R.drawable.snake_bottom_left_green, context, cellSize, cellSize)
            verticalGreen = loadBitmap(R.drawable.snake_vertical_green, context, cellSize, cellSize)
            horizontalGreen = loadBitmap(R.drawable.snake_horizontal_green, context, cellSize, cellSize)
        }

        fun getHeaders(col: Int, row: Int): List<Cell> {
            val sublist = list.find { sublist ->
                sublist.any { it.col == col && it.row == row }
            } ?: return emptyList()

            return when (sublist.size) {
                0 -> emptyList() // На случай если sublist пуст (хотя по логике не должен)
                1 -> listOf(sublist.first().copy()) // Один элемент
                else -> listOf(
                    sublist.first().copy(),
                    sublist.last().copy()
                )
            }
        }

        fun hasCell(col: Int, row: Int) : Boolean {
            for (snakeElement in list) {
                for (cell in snakeElement) {
                    if (cell.col == col && cell.row == row) {
                        return true
                    }
                }
            }
            return false
        }

        fun isEmpty() : Boolean {
            for (sublist in list) {
                for (cell in sublist) {
                    return false
                }
            }
            return true
        }

        fun setCross(col: Int, row: Int) {
            for (element in list) {
                for (cell in element) {
                    if (cell.col == col && cell.row == row) {
                        cell.isDelete = true
                        return
                    }
                }
            }
        }

        fun addFirstCell(col: Int, row: Int) {
            list.clear()
            list.add(mutableListOf(Cell(col, row, direction = "head_tail")))
        }

        fun addCell(col: Int, row: Int) {
            for (arrowCell in gameArrows.list) {
                if (arrowCell.col == col && arrowCell.row == row) {
                    when (arrowCell.direction) {
                        "left" -> loop@ for (sublist in list) {
                            for (snakeCell in sublist) {
                                if (snakeCell.col == (col  + 1) && snakeCell.row == row) {
                                    when (snakeCell.direction) {
                                        "head_left" -> snakeCell.direction = "body_horizontal"
                                        "head_top" -> snakeCell.direction = "body_bottom_left"
                                        "head_right" -> snakeCell.direction = "head_right"
                                        "head_bottom" -> snakeCell.direction = "body_left_top"
                                        "head_tail" -> snakeCell.direction = "head_right"
                                    }
                                    when (sublist.indexOf(snakeCell)) {
                                        0 -> sublist.add(0, Cell(col, row, "head_left"))
                                        else -> sublist.add(sublist.size, Cell(col, row, "head_left"))
                                    }
                                    //sublist.add(0, Cell(col, row, "head_left"))
                                    break@loop
                                }
                            }
                        }
                        "top" -> loop@ for (sublist in list) {
                            for (snakeCell in sublist) {
                                if (snakeCell.col == col && snakeCell.row == (row + 1)) {
                                    when (snakeCell.direction) {
                                        "head_left" -> snakeCell.direction = "body_top_right"
                                        "head_top" -> snakeCell.direction = "body_vertical"
                                        "head_right" -> snakeCell.direction = "body_left_top"
                                        "head_bottom" -> snakeCell.direction = "head_bottom"
                                        "head_tail" -> snakeCell.direction = "head_bottom"
                                    }
                                    when (sublist.indexOf(snakeCell)) {
                                        0 -> sublist.add(0, Cell(col, row, "head_top"))
                                        else -> sublist.add(sublist.size, Cell(col, row, "head_top"))
                                    }
                                    //sublist.add(0, Cell(col, row, "head_top"))
                                    break@loop
                                }
                            }
                        }
                        "right" -> loop@ for (sublist in list) {
                            for (snakeCell in sublist) {
                                if (snakeCell.col == (col  - 1) && snakeCell.row == row) {
                                    when (snakeCell.direction) {
                                        "head_left" -> snakeCell.direction = "head_left"
                                        "head_top" -> snakeCell.direction = "body_right_bottom"
                                        "head_right" -> snakeCell.direction = "body_horizontal"
                                        "head_bottom" -> snakeCell.direction = "body_top_right"
                                        "head_tail" -> snakeCell.direction = "head_left"
                                    }
                                    when (sublist.indexOf(snakeCell)) {
                                        0 -> sublist.add(0, Cell(col, row, "head_right"))
                                        else -> sublist.add(sublist.size, Cell(col, row, "head_right"))
                                    }
                                    //sublist.add(0, Cell(col, row, "head_right"))
                                    break@loop
                                }
                            }
                        }
                        "bottom" -> loop@ for (sublist in list) {
                            for (snakeCell in sublist) {
                                if (snakeCell.col == col && snakeCell.row == (row - 1)) {
                                    when (snakeCell.direction) {
                                        "head_left" -> snakeCell.direction = "body_right_bottom"
                                        "head_top" -> snakeCell.direction = "head_top"
                                        "head_right" -> snakeCell.direction = "body_bottom_left"
                                        "head_bottom" -> snakeCell.direction = "body_vertical"
                                        "head_tail" -> snakeCell.direction = "head_top"
                                    }
                                    when (sublist.indexOf(snakeCell)) {
                                        0 -> sublist.add(0, Cell(col, row, "head_bottom"))
                                        else -> sublist.add(sublist.size, Cell(col, row, "head_bottom"))
                                    }
                                    //sublist.add(0, Cell(col, row, "head_bottom"))
                                    break@loop
                                }
                            }
                        }
                    }
                    break
                }
            }
            removeCrosses()
        }

        fun removeCell(col: Int, row: Int) {
            for (sublist in list) {
                for (cell in sublist) {
                    if (cell.col == col && cell.row == row) {
                        val cellIndex = sublist.indexOf(cell)
                        val sublistIndex = list.indexOf(sublist)
                        fun renameDirection(prevIndex: Int) {
                            when (sublist[prevIndex].direction) {
                                "head_tail" -> sublist[prevIndex].direction = "head_tail"
                                "head_left" -> sublist[prevIndex].direction = "head_tail"
                                "head_top" -> sublist[prevIndex].direction = "head_tail"
                                "head_right" -> sublist[prevIndex].direction = "head_tail"
                                "head_bottom" -> sublist[prevIndex].direction = "head_tail"
                                "body_left_top" -> sublist[prevIndex].direction = if (sublist[prevIndex].col == col) "head_right" else "head_bottom"
                                "body_top_right" -> sublist[prevIndex].direction = if (sublist[prevIndex].col == col) "head_left" else "head_bottom"
                                "body_right_bottom" -> sublist[prevIndex].direction = if (sublist[prevIndex].col == col) "head_left" else "head_top"
                                "body_bottom_left" -> sublist[prevIndex].direction = if (sublist[prevIndex].col == col) "head_right" else "head_top"
                                "body_vertical" -> sublist[prevIndex].direction = if (sublist[prevIndex].row < row) "head_bottom" else "head_top"
                                "body_horizontal" -> sublist[prevIndex].direction = if (sublist[prevIndex].col < col) "head_right" else "head_left"
                            }
                        }
                        if ((cellIndex - 1) >= 0) renameDirection(cellIndex - 1)
                        if ((cellIndex + 1) < sublist.size) renameDirection(cellIndex + 1)

                        // делим подсписок на два списка или удаляем элемент
                        when (cellIndex) {
                            0, sublist.lastIndex -> {
                                sublist.removeAt(cellIndex)
                                if (sublist.isEmpty()) list.removeAt(sublistIndex)
                            }
                            else -> {
                                val firstPart = sublist.subList(0, cellIndex).toMutableList()
                                val secondPart = sublist.subList(cellIndex + 1, sublist.size).toMutableList()
                                list.removeAt(sublistIndex)
                                list.add(sublistIndex, firstPart)
                                list.add(sublistIndex + 1, secondPart)
                            }
                        }
                        return
                    }
                }
            }
        }

        fun removeCrosses() {
            for (snakeElement in list) {
                for (cell in snakeElement) {
                    cell.isDelete = false
                }
            }
        }

        fun draw(canvas: Canvas) {
            for (snakeElement in list) {
                for (cell in snakeElement) {
                    val cellSize: Float = screenHeight / 10.0f
                    val dstRectF = RectF(
                        cell.col * cellSize,
                        cell.row * cellSize,
                        (cell.col + 1) * cellSize,
                        (cell.row + 1) * cellSize
                    )
                    val paint = Paint()
                    when (cell.direction) {
                        "head_tail" -> circleGreen?.let { canvas.drawBitmap(it, null, dstRectF, paint) }
                        "head_left" -> circleLeftGreen?.let { canvas.drawBitmap(it, null, dstRectF, paint) }
                        "head_top" -> circleTopGreen?.let { canvas.drawBitmap(it, null, dstRectF, paint) }
                        "head_right" -> circleRightGreen?.let { canvas.drawBitmap(it, null, dstRectF, paint) }
                        "head_bottom" -> circleBottomGreen?.let { canvas.drawBitmap(it, null, dstRectF, paint) }
                        "body_left_top" -> leftTopGreen?.let { canvas.drawBitmap(it, null, dstRectF, paint) }
                        "body_top_right" -> topRightGreen?.let { canvas.drawBitmap(it, null, dstRectF, paint) }
                        "body_right_bottom" -> rightBottomGreen?.let { canvas.drawBitmap(it, null, dstRectF, paint) }
                        "body_bottom_left" -> bottomLeftGreen?.let { canvas.drawBitmap(it, null, dstRectF, paint) }
                        "body_vertical" -> verticalGreen?.let { canvas.drawBitmap(it, null, dstRectF, paint) }
                        "body_horizontal" -> horizontalGreen?.let { canvas.drawBitmap(it, null, dstRectF, paint) }
                    }
                    if (cell.isDelete) delete?.let {
                        canvas.drawBitmap(it, null,
                            RectF(cell.col * cellSize + 0.2f * cellSize,
                                cell.row * cellSize + 0.2f * cellSize,
                                (cell.col + 1) * cellSize - 0.2f * cellSize,
                                (cell.row + 1) * cellSize - 0.2f * cellSize),
                            paint
                        )
                    }
                }
            }
        }
    }

    inner class GameArrows() {
        val list: MutableList<Cell> = mutableListOf()
        private var leftArrow: Bitmap? = null
        private var topArrow: Bitmap? = null
        private var rightArrow: Bitmap? = null
        private var bottomArrow: Bitmap? = null

        fun init() {
            val cellSize: Int = (screenHeight / 10.0f).toInt()
            leftArrow = loadBitmap(R.drawable.triangle_white_left, context, cellSize, cellSize)
            topArrow = loadBitmap(R.drawable.triangle_white_up, context, cellSize, cellSize)
            rightArrow = loadBitmap(R.drawable.triangle_white_right, context, cellSize, cellSize)
            bottomArrow = loadBitmap(R.drawable.triangle_white_down, context, cellSize, cellSize)
        }

        fun hasCell(col: Int, row: Int) : Boolean {
            for (cell in list) {
                if (cell.col == col && cell.row == row) {
                    return true
                }
            }
            return false
        }

        fun draw(canvas: Canvas) {
            for (cell in list) {
                val cellSize: Float = screenHeight / 10.0f
                val dstRectF: RectF = RectF(
                    cell.col * cellSize + 0.2f * cellSize,
                    cell.row * cellSize + 0.2f * cellSize,
                    (cell.col + 1) * cellSize - 0.2f * cellSize,
                    (cell.row + 1) * cellSize - 0.2f * cellSize
                )
                val paint: Paint = Paint()
                when (cell.direction) {
                    "left" -> leftArrow?.let { canvas.drawBitmap(it, null, dstRectF, paint) }
                    "top" -> topArrow?.let { canvas.drawBitmap(it, null, dstRectF, paint) }
                    "right" -> rightArrow?.let { canvas.drawBitmap(it, null, dstRectF, paint) }
                    "bottom" -> bottomArrow?.let { canvas.drawBitmap(it, null, dstRectF, paint) }
                }
            }
        }
    }

    inner class GameMaps {
        private val mapBackground: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.map_background)

        val leftRectF: RectF = RectF(0.0f, 0.0f, 0.0f, 0.0f)
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

        fun draw(canvas: Canvas) {
            when (state) {
                "AA" -> {
                    canvas.drawBitmap(mapBackground, null, leftRectF, paintBackground)
                    canvas.drawLines(linesLeftFloatArray, paintLines)
                    canvas.drawBitmap(mapBackground, null, rightMiniRectF, paintBackground)
                    canvas.drawLines(linesRightMiniFloatArray, paintLines)
                    drawDarkened(canvas)
                }
            }
        }

        private fun drawDarkened(canvas: Canvas) {
            when (currentScene) {
                "start" -> {
                    canvas.drawRect(leftRectF, paintDarkened)
                    canvas.drawRect(rightRemainRectF, paintDarkened)
                }
                "preparing" -> {
                    canvas.drawRect(rightRemainRectF, paintDarkened)
                }
            }
        }
    }

    inner class GamePopup() {

        private val backgroundPaint = Paint().apply { color = Color.BLACK; alpha = 150 }

        private val titlePaint = Paint().apply { color = Color.WHITE; alpha = 200; textAlign = Paint.Align.CENTER; textSize = 0.0f; isAntiAlias = true }
        private var titleStartXPosition: Float = 0.0f
        private var titleStartYPosition: Float = 0.0f

        private val titleRightMiniPaint = Paint().apply { color = Color.WHITE; alpha = 200; textAlign = Paint.Align.CENTER; textSize = 0.0f; isAntiAlias = true }
        private var titleRightMiniStartXPosition: Float = 0.0f
        private var titleRightMiniStartYPosition: Float = 0.0f

        private val subtitleRoundRectF: RectF = RectF(0.0f, 0.0f, 0.0f, 0.0f)
        private var subtitleRoundRectFCornerRadius: Float = 0.0f
        private val subtitleRoundRectFPaint = Paint().apply { color = Color.BLACK; alpha = 100; style = Paint.Style.FILL; isAntiAlias = true }
        private var subtitleText = ""
        private val subtitleTextPaint: Paint = Paint().apply { color = Color.RED; isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 0.0f }
        private var subtitleTextStartXPosition: Float = 0.0f
        private var subtitleTextStartYPosition: Float = 0.0f

        private val subtitleRightMiniRoundRectF: RectF = RectF(0.0f, 0.0f, 0.0f, 0.0f)
        private var subtitleRightMiniRoundRectFCornerRadius: Float = 0.0f
        private val subtitleRightMiniRoundRectFPaint = Paint().apply { color = Color.BLACK; alpha = 100; style = Paint.Style.FILL; isAntiAlias = true }
        private var subtitleRightMiniText = ""
        private val subtitleRightMiniTextPaint: Paint = Paint().apply { color = Color.RED; isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 0.0f }
        private var subtitleRightMiniTextStartXPosition: Float = 0.0f
        private var subtitleRightMiniTextStartYPosition: Float = 0.0f

        private val leftButtonRoundRectF: RectF = RectF(0.0f, 0.0f, 0.0f, 0.0f)
        val centralButtonRoundRectF: RectF = RectF(0.0f, 0.0f, 0.0f, 0.0f)
        private val rightButtonRoundRectF: RectF = RectF(0.0f, 0.0f, 0.0f, 0.0f)
        val rightMiniButtonRoundRectF: RectF = RectF(0.0f, 0.0f, 0.0f, 0.0f)

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

        private val rightMiniButtonTextPaint: Paint = Paint().apply { color = Color.BLACK; isAntiAlias = true; textAlign = Paint.Align.CENTER; textSize = 0.0f; }
        private var rightMiniButtonTextStartXPosition: Float = 0.0f
        private var rightMiniButtonTextStartYPosition: Float = 0.0f

        fun init(w: Float, h: Float) {
            titlePaint.textSize = h / 12.0f
            titleStartXPosition = w / 2.0f
            titleStartYPosition = 2.0f * h / 10.0f

            titleRightMiniPaint.textSize = h / 24.0f
            titleRightMiniStartXPosition = (w + h) / 2.0f
            titleRightMiniStartYPosition = h / 2.0f - 4.0f * (w - h) / 12.0f

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

            rightMiniButtonRoundRectF.left = h + 3.0f * (w - h) / 12.0f
            rightMiniButtonRoundRectF.top = h / 2.0f + 2.0f * (w - h) / 12.0f
            rightMiniButtonRoundRectF.right = h + 9.0f * (w - h) / 12.0f
            rightMiniButtonRoundRectF.bottom = h / 2.0f + 4.0f * (w - h) / 12.0f

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

            rightMiniButtonTextPaint.textSize = h / 15.0f
            rightMiniButtonTextStartXPosition = (rightMiniButtonRoundRectF.right + rightMiniButtonRoundRectF.left) / 2.0f
            rightMiniButtonTextStartYPosition = (rightMiniButtonRoundRectF.bottom + rightMiniButtonRoundRectF.top) / 2.0f + h / 40.0f

            subtitleTextPaint.textSize = h / 20.0f
            subtitleRoundRectF.left = centralButtonRoundRectF.left
            subtitleRoundRectF.top =  4.75f * h / 10.0f
            subtitleRoundRectF.right = centralButtonRoundRectF.right
            subtitleRoundRectF.bottom = 5.75f * h / 10.0f
            subtitleRoundRectFCornerRadius = h / 40.0f
            subtitleTextStartXPosition = (subtitleRoundRectF.right + subtitleRoundRectF.left) / 2.0f
            subtitleTextStartYPosition = (subtitleRoundRectF.bottom + subtitleRoundRectF.top) / 2.0f + h / 80.0f

            subtitleRightMiniTextPaint.textSize = h / 25.0f
            subtitleRightMiniRoundRectF.left = rightMiniButtonRoundRectF.left
            subtitleRightMiniRoundRectF.top = h / 2.0f - (w - h) / 12.0f
            subtitleRightMiniRoundRectF.right = rightMiniButtonRoundRectF.right
            subtitleRightMiniRoundRectF.bottom = h / 2.0f + (w - h) / 12.0f
            subtitleRightMiniRoundRectFCornerRadius = h / 50.0f
            subtitleRightMiniTextStartXPosition = (subtitleRightMiniRoundRectF.right + subtitleRightMiniRoundRectF.left) / 2.0f
            subtitleRightMiniTextStartYPosition = (subtitleRightMiniRoundRectF.bottom + subtitleRightMiniRoundRectF.top) / 2.0f + h / 80.0f
        }

        fun draw(canvas: Canvas) {
            when (currentScene) {
                "start" -> {
                    canvas.drawPaint(backgroundPaint)
                    drawTitle(canvas, "Place the snake\non the left side of the screen\nby clicking on the cells", titleStartXPosition, titleStartYPosition, titlePaint)
                    drawCentralButton(canvas, "OK")
                    val minutes: Int = (gameTime / 60.0f).toInt()
                    val seconds: Int = (gameTime % 60.0f).toInt()
                    val milliseconds: Int = ((gameTime % 60.0f - seconds.toFloat()) * 1000).toInt()
                    subtitleText = if (milliseconds < 500) {
                        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
                    } else {
                        "${minutes.toString().padStart(2, '0')} ${seconds.toString().padStart(2, '0')}"
                    }
                    drawSubtitle(canvas)
                }
                "preparing" -> {
                    drawTitle(canvas, "Place the snake\non the left side of the screen\nby clicking on the cells", titleRightMiniStartXPosition, titleRightMiniStartYPosition, titleRightMiniPaint)
                    drawRightMiniButton(canvas, "Ready")
                    val minutes: Int = (gameTime / 60.0f).toInt()
                    val seconds: Int = (gameTime % 60.0f).toInt()
                    val milliseconds: Int = ((gameTime % 60.0f - seconds.toFloat()) * 1000).toInt()
                    subtitleRightMiniText = if (milliseconds < 500) {
                        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
                    } else {
                        "${minutes.toString().padStart(2, '0')} ${seconds.toString().padStart(2, '0')}"
                    }
                    drawSubtitle(canvas)
                }
            }
        }

        private fun drawTitle(canvas: Canvas, text: String, startXPosition: Float, startYPosition: Float, paint: Paint) {
            var currentTextYPosition = startYPosition
            val lines = text.split("\n")
            lines.forEach { line ->
                canvas.drawText(line, startXPosition, currentTextYPosition, paint)
                currentTextYPosition += paint.descent() - paint.ascent()
            }
        }

        private fun drawSubtitle(canvas: Canvas) {
            when (currentScene) {
                "start" -> {
                    canvas.drawRoundRect(subtitleRoundRectF, subtitleRoundRectFCornerRadius, subtitleRoundRectFCornerRadius, subtitleRoundRectFPaint)
                    canvas.drawText(subtitleText, subtitleTextStartXPosition, subtitleTextStartYPosition, subtitleTextPaint)
                }
                "preparing" -> {
                    canvas.drawRoundRect(subtitleRightMiniRoundRectF, subtitleRightMiniRoundRectFCornerRadius, subtitleRightMiniRoundRectFCornerRadius, subtitleRightMiniRoundRectFPaint)
                    canvas.drawText(subtitleRightMiniText, subtitleRightMiniTextStartXPosition, subtitleRightMiniTextStartYPosition, subtitleRightMiniTextPaint)
                }
            }
        }

        private fun drawRightButton(canvas: Canvas, text: String = "OK", color: Int = Color.BLACK) {
            canvas.drawRoundRect(rightButtonRoundRectF, buttonCornerRadius, buttonCornerRadius, buttonPaint)
            rightButtonTextPaint.color = color
            canvas.drawText(text, rightButtonTextStartXPosition, rightButtonTextStartYPosition, rightButtonTextPaint)
        }

        private fun drawCentralButton(canvas: Canvas, text: String = "OK", buttonColor: Int = Color.argb(100, 255, 255, 255), textColor: Int = Color.BLACK) {
            if (!isRectFsEqual(hoverRectF, centralButtonRoundRectF)) {
                canvas.drawRoundRect(centralButtonRoundRectF, buttonCornerRadius, buttonCornerRadius, buttonPaint.apply { color = buttonColor })
                canvas.drawText(text, centralButtonTextStartXPosition, centralButtonTextStartYPosition, centralButtonTextPaint.apply { color = textColor })
            } else {
                val hoverRectF = RectF().apply {
                    left = centralButtonRoundRectF.left - 0.10f * (centralButtonRoundRectF.right - centralButtonRoundRectF.left)
                    top = centralButtonRoundRectF.top - 0.10f * (centralButtonRoundRectF.bottom - centralButtonRoundRectF.top)
                    right = centralButtonRoundRectF.right + 0.10f * (centralButtonRoundRectF.right - centralButtonRoundRectF.left)
                    bottom = centralButtonRoundRectF.bottom + 0.10f * (centralButtonRoundRectF.bottom - centralButtonRoundRectF.top)
                }
                canvas.drawRoundRect(hoverRectF, buttonCornerRadius, buttonCornerRadius, buttonPaint.apply { color = Color.BLACK; alpha = 100 })
                canvas.drawText(text, centralButtonTextStartXPosition, centralButtonTextStartYPosition, centralButtonTextPaint.apply { color = Color.WHITE; alpha = 200 })
            }
        }

        private fun drawLeftButton(canvas: Canvas, text: String = "CANCEL", color: Int = Color.BLACK) {
            canvas.drawRoundRect(leftButtonRoundRectF, buttonCornerRadius, buttonCornerRadius, buttonPaint)
            leftButtonTextPaint.color = color
            canvas.drawText(text, leftButtonTextStartXPosition, leftButtonTextStartYPosition, leftButtonTextPaint)
        }

        private fun drawRightMiniButton(canvas: Canvas, text: String = "Ready", buttonColor: Int = Color.argb(100, 255, 255, 255), textColor: Int = Color.BLACK) {
            if (!isRectFsEqual(hoverRectF, rightMiniButtonRoundRectF)) {
                canvas.drawRoundRect(rightMiniButtonRoundRectF, buttonCornerRadius, buttonCornerRadius, buttonPaint.apply { color = buttonColor })
                canvas.drawText(text, rightMiniButtonTextStartXPosition, rightMiniButtonTextStartYPosition, rightMiniButtonTextPaint.apply { color = textColor })
            } else {
                val hoverRectF = RectF().apply {
                    left = rightMiniButtonRoundRectF.left - 0.10f * (rightMiniButtonRoundRectF.right - rightMiniButtonRoundRectF.left)
                    top = rightMiniButtonRoundRectF.top - 0.10f * (rightMiniButtonRoundRectF.bottom - rightMiniButtonRoundRectF.top)
                    right = rightMiniButtonRoundRectF.right + 0.10f * (rightMiniButtonRoundRectF.right - rightMiniButtonRoundRectF.left)
                    bottom = rightMiniButtonRoundRectF.bottom + 0.10f * (rightMiniButtonRoundRectF.bottom - rightMiniButtonRoundRectF.top)
                }
                canvas.drawRoundRect(hoverRectF, buttonCornerRadius, buttonCornerRadius, buttonPaint.apply { color = Color.BLACK; alpha = 100 })
                canvas.drawText(text, rightMiniButtonTextStartXPosition, rightMiniButtonTextStartYPosition, rightMiniButtonTextPaint.apply { color = Color.WHITE; alpha = 200 })
            }
        }
    }
}