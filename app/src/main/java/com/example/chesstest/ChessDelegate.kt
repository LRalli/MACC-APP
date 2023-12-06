package com.example.chesstest

interface ChessDelegate {
    fun pieceAt(square: Square) : ChessPiece?
    fun movePiece(from: Square, to: Square)
    fun updateProgressBar(type: String, value: Int)
    fun updateTurn(player: Player, move: String)
    fun showEvalChart()
}