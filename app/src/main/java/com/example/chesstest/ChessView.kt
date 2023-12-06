package com.example.chesstest

import android.view.View
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.*
import android.media.MediaPlayer
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

class ChessView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private val scaleFactor = 1.0f
    private var originX = 20f
    private var originY = 200f
    private var cellSide = 130f
    private val imgResIDs = setOf(
        R.drawable.chess_bdt60,
        R.drawable.chess_blt60,
        R.drawable.chess_kdt60,
        R.drawable.chess_klt60,
        R.drawable.chess_qdt60,
        R.drawable.chess_qlt60,
        R.drawable.chess_rdt60,
        R.drawable.chess_rlt60,
        R.drawable.chess_ndt60,
        R.drawable.chess_nlt60,
        R.drawable.chess_pdt60,
        R.drawable.chess_plt60,
    )

    private val bitmaps = mutableMapOf<Int, Bitmap>()
    private val paint = Paint()
    private val paintHighlight = Paint()

    private var movingPieceBitmap: Bitmap? = null
    private var movingPiece: ChessPiece? = null
    private var movingPieceX = -1f
    private var movingPieceY = -1f
    private var fromCol: Int = -1
    private var fromRow: Int = -1

    var chessDelegate: ChessDelegate? = null

    /////////////////////////////////////////////////////////////////////////////////////////////
    init {
        loadBitmaps()
    }

    private fun loadBitmaps() =
        imgResIDs.forEach { imgResID ->
            bitmaps[imgResID] = BitmapFactory.decodeResource(resources, imgResID)
        }

    private fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

    private fun convertRowColFromIntToString(move: Int, type: String): String {
        //assert(move>=0 && move<=7)

        var converted = ""
        if (type.equals("column")) {
            when (move) {
                0 -> converted = "a"
                1 -> converted = "b"
                2 -> converted = "c"
                3 -> converted = "d"
                4 -> converted = "e"
                5 -> converted = "f"
                6 -> converted = "g"
                7 -> converted = "h"

            }
        } else if (type.equals("row")) {
            when (move) {
                0 -> converted = "1"
                1 -> converted = "2"
                2 -> converted = "3"
                3 -> converted = "4"
                4 -> converted = "5"
                5 -> converted = "6"
                6 -> converted = "7"
                7 -> converted = "8"
            }
        }
        return converted
    }

    private fun ICBO(value: Int): Int {
        var converted = 9

        when (value) {
            0 -> converted = 7
            1 -> converted = 6
            2 -> converted = 5
            3 -> converted = 4
            4 -> converted = 3
            5 -> converted = 2
            6 -> converted = 1
            7 -> converted = 0
        }
        return converted
    }

    private fun CBO(value: String): String {
        //assert(move>=0 && move<=7)
        var converted = value

        when (value) {
            "1" -> converted = "8"
            "2" -> converted = "7"
            "3" -> converted = "6"
            "4" -> converted = "5"
            "5" -> converted = "4"
            "6" -> converted = "3"
            "7" -> converted = "2"
            "8" -> converted = "1"
            "a" -> converted = "h"
            "b" -> converted = "g"
            "c" -> converted = "f"
            "d" -> converted = "e"
            "e" -> converted = "d"
            "f" -> converted = "c"
            "g" -> converted = "b"
            "h" -> converted = "a"
        }
        return converted
    }

    private fun checkMoveValidity(
        fromCol: Int,
        fromRow: Int,
        toCol: Int,
        toRow: Int,
        prom: String = "",
        id: Int
    ): Boolean? {

        val fromRow2 = fromRow
        val fromCol2 = fromCol
        val toRow2 = toRow
        val toCol2 = toCol


        val usableFromColumn = convertRowColFromIntToString(fromCol2, "column")
        val usableFromRow = convertRowColFromIntToString(fromRow2, "row")
        val usableToCol = convertRowColFromIntToString(toCol2, "column")
        val usableToRow = convertRowColFromIntToString(toRow2, "row")

        var id_string = id.toString()
        var name = "https://lralli.pythonanywhere.com/" + "/?move=" +
                "" + usableFromColumn + usableFromRow + usableToCol + usableToRow + prom +
                "" + "&index=" + id_string
        var url = URL(name)
        var conn = url.openConnection() as HttpsURLConnection

        var checkValidity: Boolean

        try {
            conn.run {
                requestMethod = "POST"
                val r = JSONObject(InputStreamReader(inputStream).readText())
                checkValidity = r.get("valid") as Boolean

                Log.d("Move validity", checkValidity.toString())
                Log.d("Mossa ", usableFromColumn + usableFromRow + usableToCol + usableToRow)
                return checkValidity

            }
        } catch (e: Exception) {
            Log.e("Move error: ", e.toString())
        }
        return null
    }

    private fun getEvaluation(id: Int): Pair<String, Int>? {

        var id_string = id.toString()
        var name = "https://lralli.pythonanywhere.com/" + "/info?index=" + id_string
        var url = URL(name)
        var conn = url.openConnection() as HttpsURLConnection
        var pair: Pair<String, Int>?

        try {
            conn.run {
                requestMethod = "GET"
                val r = JSONObject(InputStreamReader(inputStream).readText())
                val t = r.get("type") as String
                val v = r.get("value") as Int
                pair = Pair(t, v)

                Log.d("Evaluation", pair.toString())
                if (t == "cp") ChessGame.evaluationsArray.add(v)
                return pair

            }
        } catch (e: Exception) {
            Log.e("Move error: ", e.toString())
        }
        return null
    }

    private fun playSound() {
        val mp = MediaPlayer.create(this.context, R.raw.move_loud)
        mp.start()
        mp.setOnCompletionListener(MediaPlayer.OnCompletionListener {
            //mp.release()
        })
    }

    /////////////////////////////////////////////////////////////////////////////////////////////

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val smaller = min(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(smaller, smaller)
    }

    private fun drawSquareAt(canvas: Canvas, col: Float, row: Float, isDark: Boolean) {
        paint.color = if (isDark) ChessGame.darkColor else ChessGame.lightColor
        canvas.drawRect(
            originX + col * cellSide,
            originY + row * cellSide,
            originX + (col + 1) * cellSide,
            originY + (row + 1) * cellSide,
            paint
        )
    }

    private fun drawChessboard(canvas: Canvas) {
        for (row in 0 until 8)
            for (col in 0 until 8)
                drawSquareAt(canvas, col + 0.3f, row + 0f, (col + row) % 2 == 1)
    }

    private fun drawTextAtBis(canvas: Canvas, col: Float, row: Float, msg: String) {
        val paintThin = Paint()
        val padding = 30f
        paintThin.color = Color.parseColor("#999999")
        paintThin.strokeWidth = 4f
        paintThin.textSize = 50f
        canvas.drawText(msg, originX, originY + row * cellSide + cellSide / 2 + 20f, paintThin)
    }

    private fun drawTextAtDoBis(canvas: Canvas) {
        for (row in 0 until 8)
            drawTextAtBis(canvas, 0f, row + 0f, ChessGame.numeri[row])
    }

    private fun drawTextAt(canvas: Canvas, col: Float, row: Float, msg: String) {
        val paintThin = Paint()
        val padding = 30f
        paintThin.color = Color.parseColor("#999999")
        paintThin.strokeWidth = 10f
        paintThin.textSize = 45f
        canvas.drawText(
            msg,
            originX + col * cellSide + cellSide / 2 + 10f,
            originY + row * cellSide - 9f,
            paintThin
        )
    }

    private fun drawTextAtDo(canvas: Canvas) {
        for (col in 0 until 8)
            drawTextAt(canvas, col + 0f, 8.5f, ChessGame.lettere[col])
    }

    private fun highlightSquares(canvas: Canvas, s: Square?) {
        try {
            canvas.drawRect(
                +originX + (s!!.col + 0.3f) * cellSide,
                originY + s!!.row * cellSide,
                originX + (s!!.col + 0.3f + 1) * cellSide,
                originY + (s!!.row + 1) * cellSide,
                paintHighlight
            )
        } catch (e: Exception) {
        }
    }

    private fun drawPieceAt(canvas: Canvas, col: Float, row: Float, resID: Int) =
        canvas.drawBitmap(
            bitmaps[resID]!!,
            null,
            RectF(
                originX + col * cellSide,
                originY + (7 - row) * cellSide,
                originX + (col + 1) * cellSide,
                originY + ((7 - row) + 1) * cellSide
            ),
            paint
        )

    private fun drawPieces(canvas: Canvas) {
        for (row in 0 until 8)
            for (col in 0 until 8)
                chessDelegate?.pieceAt(Square(col, row))?.let { piece ->
                    if (piece != movingPiece) {
                        drawPieceAt(canvas, col + 0.3f, row + 0f, piece.resID)
                    }
                }
        movingPieceBitmap?.let {
            canvas.drawBitmap(
                it,
                null,
                RectF(
                    movingPieceX - cellSide / 2,
                    movingPieceY - cellSide / 2,
                    movingPieceX + cellSide / 2,
                    movingPieceY + cellSide / 2
                ),
                paint
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas ?: return
        val chessBoardSide = min(width, height) * scaleFactor
        cellSide = chessBoardSide / 8.5f
        originX = (width - chessBoardSide) / 2f
        originY = (height - chessBoardSide) / 2f

        drawChessboard(canvas)
        drawTextAtDoBis(canvas)
        drawTextAtDo(canvas)
        paintHighlight.color = Color.parseColor("#A88BC34A")
        highlightSquares(canvas, ChessGame.fromSquareHighlight)
        highlightSquares(canvas, ChessGame.toSquareHighlight)
        drawPieces(canvas)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        event ?: return false // the event is returned if it is null
        if (ChessGame.localGameEnded) return false
        if (ChessGame.stockfishGameEnded) return false
        if (ChessGame.gameInProgress == "ONLINE" && ChessGame.waitingForAdversary) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                fromCol = ((event.x - originX) / cellSide).toInt()
                fromRow = 7 - ((event.y - originY) / cellSide).toInt()

                chessDelegate?.pieceAt(Square(fromCol, fromRow))?.let {
                    movingPiece = it
                    movingPieceBitmap = bitmaps[it.resID]
                }
            }

            MotionEvent.ACTION_MOVE -> {
                var myOnlineColorNum = Player.BLACK
                if (ChessGame.myOnlineColor == "WHITE") myOnlineColorNum = Player.WHITE
                if (
                    ChessGame.gameInProgress == "LOCAL" ||
                    (ChessGame.gameInProgress == "STOCKFISH" && movingPiece?.player?.equals(Player.WHITE) == true) ||
                    (ChessGame.gameInProgress == "ONLINE" && movingPiece?.player?.equals(
                        myOnlineColorNum
                    ) == true)
                ) {
                    //Log.i("I", "onTouchEvent: ${ChessGame.gameInProgress == "ONLINE"}")
                    //Log.i("I", "onTouchEvent: ${movingPiece?.player?.equals(myOnlineColorNum) == true}")
                    movingPieceX = event.x
                    movingPieceY = event.y
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP -> {
                var col = ((event.x - originX) / cellSide).toInt()
                var row = 7 - ((event.y - originY) / cellSide).toInt()

                var moveIsValid = false
                if (movingPiece != null && (fromCol != col || fromRow != row)) {

                    if (ChessGame.gameInProgress == "LOCAL") {


                        val promotionCheck =
                            ChessGame.promotion(movingPiece, fromRow, fromCol, row, col)
                        Log.i("Thread prima di job: ", Thread.currentThread().name)
                        val job = GlobalScope.launch(Dispatchers.IO) {
                            Log.i("Thread job: ", Thread.currentThread().name)
                            val c1 = async {
                                checkMoveValidity(
                                    fromCol,
                                    fromRow,
                                    col,
                                    row,
                                    promotionCheck,
                                    ChessGame.matchId
                                )
                            }
                            moveIsValid = c1.await() == true
                        }

                        runBlocking {
                            job.join()
                            Log.i("Thread runblocking: ", Thread.currentThread().name)
                            if (moveIsValid) {

                                //HIGHLIGHTING
                                ChessGame.fromSquareHighlight = Square(fromCol, 7 - fromRow)
                                ChessGame.toSquareHighlight = Square(col, 7 - row)

                                ChessGame.removeEnpassantPawn(
                                    movingPiece,
                                    fromRow,
                                    fromCol,
                                    row,
                                    col
                                )

                                val castleCheck =
                                    ChessGame.castle(movingPiece, fromRow, fromCol, row, col)
                                when (castleCheck) {
                                    "whiteshort" -> ChessGame.movePiece(7, 0, 5, 0)
                                    "whitelong" -> ChessGame.movePiece(0, 0, 3, 0)
                                    "blackshort" -> ChessGame.movePiece(7, 7, 5, 7)
                                    "blacklong" -> ChessGame.movePiece(0, 7, 3, 7)
                                }

                                if (movingPiece!!.player == Player.WHITE && ChessGame.isLocalMate == "true") {
                                    ChessGame.stockfishGameEnded = true
                                    toast("White won")
                                    println("White won")

                                } else if (movingPiece!!.player == Player.BLACK && ChessGame.isLocalMate == "true") {
                                    ChessGame.stockfishGameEnded = true
                                    toast("Black won")
                                    println("Black won")
                                }

                                ChessGame.piecesBox.remove(movingPiece)
                                if (promotionCheck == "") {
                                    movingPiece?.let {
                                        ChessGame.addPiece(
                                            it.copy(
                                                col = col,
                                                row = row
                                            )
                                        )
                                    }
                                }
                                if (movingPiece != null) {
                                    ChessGame.pieceAt(col, row)?.let {
                                        if (it.player != movingPiece?.player) {
                                            ChessGame.piecesBox.remove(it)
                                        }
                                    }
                                }

                                playSound()
                            }
                        }
                    }

                    else if (ChessGame.gameInProgress == "ONLINE") {

                        var colPrima = 9
                        var rowPrima = 9
                        var fromColPrima = 9
                        var fromRowPrima = 9


                        if (ChessGame.myOnlineColor == "BLACK"){
                            //Log.d("fromRow_prima",fromRow.toString())
                            fromRowPrima=fromRow
                            fromRow=ICBO(fromRow)
                            //Log.d("fromRow_dopo",fromRow.toString())
                            fromColPrima=fromCol
                            fromCol=ICBO(fromCol)
                            rowPrima=row
                            row=ICBO(row)
                            colPrima=col
                            col=ICBO(col)

                        }

                        val promotionCheck = ChessGame.onlinePromotion(movingPiece, fromRow, fromCol, row, col)

                        val job = GlobalScope.launch(Dispatchers.IO) {
                            val c1 = async { checkMoveValidity(fromCol, fromRow, col, row, promotionCheck, ChessGame.matchId) }
                            moveIsValid = c1.await() == true
                        }

                        runBlocking {
                            job.join()

                            if (moveIsValid) {

                                //HIGHLIGHTING

                                if (ChessGame.myOnlineColor == "BLACK") {
                                    ChessGame.fromSquareHighlight = Square(fromColPrima, 7-fromRowPrima)
                                    ChessGame.toSquareHighlight = Square(colPrima, 7-rowPrima)
                                } else {
                                    ChessGame.fromSquareHighlight = Square(fromCol, 7-fromRow)
                                    ChessGame.toSquareHighlight = Square(col, 7-row)
                                }


                                if (ChessGame.myOnlineColor == "BLACK") {
                                    ChessGame.removeEnpassantPawn(movingPiece, ICBO(fromRow), ICBO(fromCol), ICBO(row), ICBO(col))
                                } else {
                                    ChessGame.removeEnpassantPawn(movingPiece, fromRow, fromCol, row, col)
                                }

                                val castleCheck = ChessGame.castle(movingPiece, fromRow, fromCol, row, col)

                                if (ChessGame.myOnlineColor == "BLACK") {
                                    when (castleCheck) {
                                        "whiteshort" -> ChessGame.movePiece(ICBO(7), ICBO(0), ICBO(5), ICBO(0))
                                        "whitelong" -> ChessGame.movePiece(ICBO(0), ICBO(0), ICBO(3), ICBO(0))
                                        "blackshort" -> ChessGame.movePiece(ICBO( 7), ICBO(7), ICBO(5), ICBO(7))
                                        "blacklong" -> ChessGame.movePiece(ICBO( 0), ICBO(7), ICBO(3), ICBO(7))
                                    }
                                } else {
                                    when (castleCheck) {
                                        "whiteshort" -> ChessGame.movePiece(7, 0, 5, 0)
                                        "whitelong" -> ChessGame.movePiece(0, 0, 3, 0)
                                        "blackshort" -> ChessGame.movePiece( 7, 7, 5, 7)
                                        "blacklong" -> ChessGame.movePiece( 0, 7, 3, 7)
                                    }
                                }

                                if (ChessGame.myOnlineColor == "BLACK") {
                                    ChessGame.piecesBox.remove(movingPiece)
                                    if (promotionCheck == "") {
                                        movingPiece?.let {
                                            ChessGame.addPiece(
                                                it.copy(
                                                    col = colPrima,
                                                    row = rowPrima
                                                )
                                            )
                                        }
                                    }
                                    if (movingPiece != null) {
                                        ChessGame.pieceAt(colPrima, rowPrima)?.let {
                                            if (it.player != movingPiece?.player) {
                                                ChessGame.piecesBox.remove(it)
                                            }
                                        }
                                    }

                                } else {
                                    ChessGame.piecesBox.remove(movingPiece)
                                    if (promotionCheck == "") {
                                        movingPiece?.let {
                                            ChessGame.addPiece(
                                                it.copy(
                                                    col = col,
                                                    row = row
                                                )
                                            )
                                        }
                                    }
                                    if (movingPiece != null) {
                                        ChessGame.pieceAt(col, row)?.let {
                                            if (it.player != movingPiece?.player) {
                                                ChessGame.piecesBox.remove(it)
                                            }
                                        }
                                    }
                                }
                                val usableFromCol = convertRowColFromIntToString(fromCol, "column")
                                val usableFromRow = convertRowColFromIntToString(fromRow, "row")
                                val usableToCol = convertRowColFromIntToString(col, "column")
                                val usableToRow = convertRowColFromIntToString(row, "row")

                                if (ChessGame.myOnlineColor == "WHITE") {
                                    val moveB =(usableFromCol) + (usableFromRow) + (usableToCol) + (usableToRow) + promotionCheck
                                    chessDelegate?.updateTurn(movingPiece!!.player, moveB)
                                } else if (ChessGame.myOnlineColor == "BLACK") {
                                    val moveW = usableFromCol + usableFromRow + usableToCol + usableToRow + promotionCheck
                                    chessDelegate?.updateTurn(movingPiece!!.player, moveW)
                                }
                                playSound()
                            }
                        }
                    }

                    else if (ChessGame.gameInProgress == "STOCKFISH") {

                        ChessGame.firstMove = false
                        var response = ""
                        var mate = ""
                        val usableFromColumn = convertRowColFromIntToString(fromCol, "column")
                        val usableFromRow = convertRowColFromIntToString(fromRow, "row")
                        val usableToCol = convertRowColFromIntToString(col, "column")
                        val usableToRow = convertRowColFromIntToString(row, "row")
                        val promotionCheck = ChessGame.promotion(movingPiece, fromRow, fromCol, row, col)

                        Log.i("Thread before job: ",Thread.currentThread().name )

                        val job = GlobalScope.launch(Dispatchers.IO) {
                            Log.i("Thread job: ",Thread.currentThread().name )
                            run {
                                var id_string=ChessGame.matchId.toString()
                                val name = "https://JaR.pythonanywhere.com"+"/stockfish?move=" +
                                        "" + usableFromColumn + usableFromRow + usableToCol + usableToRow + promotionCheck+
                                        "" + "&index=" + id_string
                                val url = URL(name)
                                val conn = url.openConnection() as HttpsURLConnection
                                try {
                                    conn.run {
                                        requestMethod = "POST"
                                        val r = JSONObject(InputStreamReader(inputStream).readText())
                                        Log.d("Stockfish response", r.toString())
                                        moveIsValid = r.get("valid") as Boolean
                                        response = r.get("response") as String
                                        mate = r.get("mate") as String
                                    }
                                } catch (e: Exception) {
                                    Log.e("Move error: ", e.toString())
                                }
                            }
                        }

                        runBlocking {
                            job.join()
                            Log.i("Thread runblocking: ",Thread.currentThread().name )
                            if (moveIsValid) {

                                if (mate!="") chessDelegate?.showEvalChart()

                                // Player move
                                ChessGame.removeEnpassantPawn(movingPiece, fromRow, fromCol, row, col)
                                val castleCheck = ChessGame.castle(movingPiece, fromRow, fromCol, row, col)
                                when (castleCheck) {
                                    "whiteshort" -> ChessGame.movePiece(7, 0, 5, 0)
                                    "whitelong" -> ChessGame.movePiece(0, 0, 3, 0)
                                    "blackshort" -> ChessGame.movePiece(7, 7, 5, 7)
                                    "blacklong" -> ChessGame.movePiece(0, 7, 3, 7)
                                }
                                ChessGame.piecesBox.remove(movingPiece)
                                if (promotionCheck.equals("")) {
                                    movingPiece?.let {
                                        ChessGame.addPiece(
                                            it.copy(
                                                col = col,
                                                row = row
                                            )
                                        )
                                    }
                                }
                                if (movingPiece != null) {
                                    ChessGame.pieceAt(col, row)?.let {
                                        if (it.player != movingPiece?.player) {
                                            ChessGame.piecesBox.remove(it)
                                        }
                                    }
                                }
                                if (mate == "player") ChessGame.stockfishGameEnded = true

                                // Stockfish response
                                else {
                                    val squares = ChessGame.convertMoveStringToSquares(response)
                                    movingPiece = ChessGame.pieceAt(squares[0])

                                    val promotionCheck = ChessGame.promotion(movingPiece, squares[0].row, squares[0].col, squares[1].row, squares[1].col)
                                    ChessGame.removeEnpassantPawn(movingPiece, squares[0].row, squares[0].col, squares[1].row, squares[1].col)
                                    when (ChessGame.castle(movingPiece, squares[0].row, squares[0].col, squares[1].row, squares[1].col)) {
                                        "whiteshort" -> ChessGame.movePiece(7, 0, 5, 0)
                                        "whitelong" -> ChessGame.movePiece(0, 0, 3, 0)
                                        "blackshort" -> ChessGame.movePiece(7, 7, 5, 7)
                                        "blacklong" -> ChessGame.movePiece(0, 7, 3, 7)
                                    }


                                    ChessGame.piecesBox.remove(movingPiece)
                                    if (promotionCheck == "") {
                                        movingPiece?.let {
                                            ChessGame.addPiece(
                                                it.copy(
                                                    col = squares[1].col,
                                                    row = squares[1].row
                                                )
                                            )
                                        }
                                    }

                                    if (movingPiece != null) {
                                        ChessGame.pieceAt(squares[1].col, squares[1].row)?.let {
                                            if (it.player != movingPiece?.player) {
                                                ChessGame.piecesBox.remove(it)
                                            }
                                        }
                                    }
                                    ChessGame.toString()
                                    invalidate()
                                    if (mate == "stockfish") ChessGame.stockfishGameEnded = true
                                }

                                playSound()
                            }
                        }
                    }
                }

                movingPiece = null
                movingPieceBitmap = null
                invalidate()
                ChessGame.resettedGame = false

                // Get position evaluation
                if (ChessGame.gameInProgress == "STOCKFISH" && moveIsValid) {
                    val job = GlobalScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Default) {
                            val (evaluationType, evaluationValue) = checkNotNull(getEvaluation(ChessGame.matchId))
                            chessDelegate?.updateProgressBar(evaluationType, evaluationValue)
                        }
                    }
                    runBlocking {
                        job.join()
                    }
                }
            }
        }
        return true
    }
}




