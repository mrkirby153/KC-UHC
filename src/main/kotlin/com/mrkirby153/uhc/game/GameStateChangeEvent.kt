package com.mrkirby153.uhc.game

import com.mrkirby153.uhc.CancellableBukkitEvent

data class GameStateChangeEvent(val from: GameState, val to: GameState) : CancellableBukkitEvent()