package com.mrkirby153.kcuhc.events

import com.mrkirby153.kcuhc.game.GameState


class GameInitializingEvent : GenericBukkitEvent()

class GameStateChangeEvent(val from: GameState, val to: GameState) : GenericBukkitEvent()