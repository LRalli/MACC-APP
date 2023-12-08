package com.example.chesstest

interface GameEventListener {
    fun onWinDetected(winner: String)
}