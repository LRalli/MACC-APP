package com.example.chesstest

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast

class LocalGame : AppCompatActivity(), ChessDelegate {

    private lateinit var chessView: ChessView
    private lateinit var resetButton: Button
    private lateinit var startButton: Button
    lateinit var StockprogressBar: ProgressBar
    override fun pieceAt(square: Square): ChessPiece? = ChessGame.pieceAt(square)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ChessGame.lettere= arrayOf("a","b","c","d","e","f","g","h")
        ChessGame.numeri= arrayOf("8","7","6","5","4","3","2","1")

        setContentView(R.layout.activity_local_game)

        chessView = findViewById(R.id.chess_view)
        resetButton = findViewById(R.id.reset_button)
        startButton = findViewById(R.id.start_button)
        StockprogressBar = findViewById(R.id.progress_bar_local)

        if(ChessGame.startedmatch==0){
            resetButton.isEnabled = false
            startButton.isEnabled = true
        }else{
            resetButton.isEnabled = true
            startButton.isEnabled = false
        }

        ChessGame.startedmatch=ChessGame.startedmatch+1
        chessView.chessDelegate = this

        resetButton.setOnClickListener {
            ChessGame.reset(ChessGame.matchId)
            ChessGame.matchId=404
            ChessGame.resettedGame = true
            resetButton.isEnabled = false
            startButton.isEnabled = true
            chessView.invalidate()
        }

        startButton.setOnClickListener {
            StockprogressBar.visibility = View.VISIBLE
            ChessGame.matchId=ChessGame.startMatchId()
            println("Started match"+ChessGame.matchId)
            if(ChessGame.matchId!=404) {
                ChessGame.gameInProgress = "LOCAL"
                Toast.makeText(applicationContext, "Game started, Good luck!", Toast.LENGTH_LONG).show()

                resetButton.isEnabled = true
                startButton.isEnabled = false
                StockprogressBar.visibility = View.INVISIBLE

            }else{
                Toast.makeText(applicationContext, "Server full, please try again later", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun drawTextAt(canvas: Canvas, col: Int, row: Int) {
        val paintThin = Paint()
        val padding = 30f
        paintThin.color = Color.parseColor("#999999")
        paintThin.strokeWidth = 3f
        paintThin.textSize = 60f
        canvas.drawText("s", +50f, 50f, paintThin)
    }

    override fun movePiece(from: Square, to: Square) {}
    override fun updateProgressBar(type: String, value: Int) {}
    override fun showEvalChart() {}

    override fun updateTurn(player: Player, move: String) {
        Log.d("player", player.toString())
        ChessGame.firstMove=false
    }
}