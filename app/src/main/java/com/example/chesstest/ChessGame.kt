package com.example.chesstest

import android.graphics.Color
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object ChessGame {

    // OBJECT PROPERTIES
    var startedmatch =0 // number of started matches

    var stockfishGameEnded: Boolean = false
    var localGameEnded: Boolean = false
    var resettedGame: Boolean = false
    var firstMove: Boolean = true

    var matchId: Int = 404
    var gameInProgress: String = "" //LOCAL, STOCKFISH, ONLINE
    var isLocalMate= "false"

    var fromSquareHighlight: Square? = null
    var toSquareHighlight: Square? = null

    var piecesBox = mutableSetOf<ChessPiece>()

    val lightColor: Int = Color.parseColor("#F2E6D6") //"#EEEEEE"
    val darkColor: Int = Color.parseColor("#D8B27E")  //"#BBBBBB"
    var lettere= arrayOf("a","b","c","d","e","f","g","h")
    var numeri= arrayOf("8","7","6","5","4","3","2","1")

    ///////////////////////////////////////////////////////////////////////////////////////////////
    init{
        reset(ChessGame.matchId)
    }

    private fun clear() {
        piecesBox.clear()
    }

    fun addPiece(piece: ChessPiece) {
        piecesBox.add(piece)
    }

    //pieceAt returns the piece at the given square by calling pieceAt(square.col, square.row)
    fun pieceAt(square: Square): ChessPiece? {
        return pieceAt(square.col, square.row)
    }
    //overloaded pieceAt checks for the piece at the given column and row inside piecesBox
    //it returns a ChessPiece object or null if no piece is found
    fun pieceAt(col: Int, row: Int): ChessPiece? {
        for (piece in piecesBox) {
            if (col == piece.col && row == piece.row) {
                return piece
            }
        }
        return null
    }

    fun movePiece(from: Square, to: Square) {
        movePiece(from.col, from.row, to.col, to.row)
    }

    fun movePiece(fromCol: Int, fromRow: Int, toCol: Int, toRow: Int) {
        if (fromCol == toCol && fromRow == toRow) return
        val movingPiece = pieceAt(fromCol, fromRow) ?: return

        pieceAt(toCol, toRow)?.let {
            if (it.player == movingPiece.player) {
                return
            }
            piecesBox.remove(it)
        }

        if (movingPiece.chessman==Chessman.PAWN) {
            if (movingPiece.player==Player.WHITE && fromRow==6 && toRow==7) {
                piecesBox.remove(movingPiece)
                addPiece(movingPiece.copy(chessman=Chessman.QUEEN, resID = R.drawable.chess_qlt60, col = toCol, row = toRow))
                return
            }
            else if (movingPiece.player==Player.BLACK && fromRow==1 && toRow==0) {
                piecesBox.remove(movingPiece)
                addPiece(movingPiece.copy(chessman=Chessman.QUEEN, resID = R.drawable.chess_qdt60, col = toCol, row = toRow))
                return
            }
        }

        piecesBox.remove(movingPiece)
        addPiece(movingPiece.copy(col = toCol, row = toRow))
    }

    fun reset(id: Int) {
        resetStockFish(id)
        firstMove = true
        fromSquareHighlight = null
        toSquareHighlight = null
        Log.d("!", "#############\nRESET\n#############")
        clear()

        for (i in 0 until 2) {
            addPiece(ChessPiece(0 + i * 7, 0, Player.WHITE, Chessman.ROOK, R.drawable.chess_rlt60))
            addPiece(ChessPiece(0 + i * 7, 7, Player.BLACK, Chessman.ROOK, R.drawable.chess_rdt60))

            addPiece(ChessPiece(1 + i * 5, 0, Player.WHITE, Chessman.KNIGHT, R.drawable.chess_nlt60))
            addPiece(ChessPiece(1 + i * 5, 7, Player.BLACK, Chessman.KNIGHT, R.drawable.chess_ndt60))

            addPiece(ChessPiece(2 + i * 3, 0, Player.WHITE, Chessman.BISHOP, R.drawable.chess_blt60))
            addPiece(ChessPiece(2 + i * 3, 7, Player.BLACK, Chessman.BISHOP, R.drawable.chess_bdt60))
        }

        for (i in 0 until 8) {
            addPiece(ChessPiece(i, 1, Player.WHITE, Chessman.PAWN, R.drawable.chess_plt60))
            addPiece(ChessPiece(i, 6, Player.BLACK, Chessman.PAWN, R.drawable.chess_pdt60))
        }

        addPiece(ChessPiece(3, 0, Player.WHITE, Chessman.QUEEN, R.drawable.chess_qlt60))
        addPiece(ChessPiece(3, 7, Player.BLACK, Chessman.QUEEN, R.drawable.chess_qdt60))

        addPiece(ChessPiece(4, 0, Player.WHITE, Chessman.KING, R.drawable.chess_klt60))
        addPiece(ChessPiece(4, 7, Player.BLACK, Chessman.KING, R.drawable.chess_kdt60))
    }

    private fun resetStockFish(id: Int){
        resettedGame = true
        stockfishGameEnded = false

        val job = GlobalScope.launch(Dispatchers.IO) {
            run {
                val name = "https://lralli.pythonanywhere.com/reset?index=$id"
                val url = URL(name)
                val conn = url.openConnection() as HttpsURLConnection
                try {
                    conn.run{
                        requestMethod = "GET"
                        val r = JSONObject(InputStreamReader(inputStream).readText())
                        val resetErr= r.get("error") as Boolean
                        val resetID = r.get("reset_id") as Int
                        Log.d("RESET", resetID.toString()+" "+resetErr.toString())
                    }
                } catch (e: Exception) {
                    Log.e("RESET ERROR", e.toString())
                }
            }
        }
        runBlocking {
            job.join()
        }
    }

    fun startMatchId() : Int {
        resettedGame = true
        stockfishGameEnded = false
        localGameEnded = false

        var id_match=404
        val job = GlobalScope.launch(Dispatchers.IO) { run {
            val name = "https://lralli.pythonanywhere.com" + "/startMatch"
            val url = URL(name)
            val conn = url.openConnection() as HttpsURLConnection
            try {
                conn.run{
                    requestMethod = "GET"
                    val r = JSONObject(InputStreamReader(inputStream).readText())
                    id_match = r.get("response") as Int
                    Log.d("MATCH ID", id_match.toString())
                }
            } catch (e: Exception) {
                Log.e("MATCH ERROR", e.toString())
            }
        }
        }
        runBlocking {
            job.join()
        }
        return id_match
    }

    private fun boardRow(row: Int) : String {
        var desc = ""
        for (col in 0 until 8) {
            desc += " "
            desc += pieceAt(col, row)?.let {
                val white = it.player == Player.WHITE
                when (it.chessman) {
                    Chessman.KING -> if (white) "K" else "k"
                    Chessman.QUEEN -> if (white) "Q" else "q"
                    Chessman.BISHOP -> if (white) "B" else "b"
                    Chessman.ROOK -> if (white) "R" else "r"
                    Chessman.KNIGHT -> if (white) "N" else "n"
                    Chessman.PAWN -> if (white) "P" else "p"
                }
            } ?: "."
        }
        return desc
    }

    override fun toString(): String {
        var desc = " \n"
        for (row in 7 downTo 0) {
            desc += "$row"
            desc += boardRow(row)
            desc += "\n"
        }
        desc += "  0 1 2 3 4 5 6 7"

        return desc
    }

    fun promotion(movingPiece:ChessPiece?, fromRow:Int, fromCol:Int, row:Int, col:Int):String {
        if (movingPiece!!.chessman == Chessman.PAWN) {
            if (movingPiece.player == Player.WHITE && fromRow==6 && row==7) {
                piecesBox.remove(movingPiece)

                addPiece(
                    movingPiece.copy(
                        chessman = Chessman.QUEEN,
                        resID = R.drawable.chess_qlt60,
                        col = col,
                        row = row
                    )
                )
                return "Q"
            }
            else if (movingPiece.player == Player.BLACK && fromRow==1 && row==0) {
                piecesBox.remove(movingPiece)

                addPiece(
                    movingPiece.copy(
                        chessman = Chessman.QUEEN,
                        resID = R.drawable.chess_qdt60,
                        col = col,
                        row = row
                    )
                )
                return "q"
            }
        }
        return ""
    }

    fun castle(movingPiece:ChessPiece?, fromRow:Int, fromCol:Int, row:Int, col:Int):String {

        if (movingPiece!!.chessman == Chessman.KING) {
            if (movingPiece.player == Player.WHITE && fromCol==4 && fromRow==0 && col==6 && row==0) {
                return "whiteshort"
            }
            if (movingPiece.player == Player.WHITE && fromCol==4 && fromRow==0 && col==2 && row==0) {
                return "whitelong"
            }
            if (movingPiece.player == Player.BLACK && fromCol==4 && fromRow==7 && col==6 && row==7) {
                return "blackshort"
            }
            if (movingPiece.player == Player.BLACK && (fromCol)==4 && (fromRow)==7 && (col)==2 && (row)==7) {
                return "blacklong"
            }
        }
        return ""
    }

    fun removeEnpassantPawn(movingPiece:ChessPiece?, fromRow:Int, fromCol:Int, row:Int, col:Int) {
        if (movingPiece!!.chessman==Chessman.PAWN) {
            if(fromCol!=col){
                if(pieceAt(col, row)==null){
                    piecesBox.remove(pieceAt(col,fromRow))
                }
            }
        }
    }
}