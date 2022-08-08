package com.mrkirby153.kcuhc.game

/**
 * The current state of the game
 */
enum class GameState {
    UNINITIALIZED,
    INITIALIZING,
    WAITING,
    COUNTDOWN,
    RUNNING,
    ENDING,
    ENDED,
    CANCELED
}