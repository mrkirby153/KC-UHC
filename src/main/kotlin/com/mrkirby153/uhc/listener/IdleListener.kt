package com.mrkirby153.uhc.listener

import com.mrkirby153.uhc.game.UHCGame
import me.mrkirby153.kcutils.event.UpdateEvent
import me.mrkirby153.kcutils.event.UpdateType
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.weather.WeatherChangeEvent

class IdleListener(private val game: UHCGame) : Listener {

    @EventHandler
    fun onUpdate(event: UpdateEvent) {
        if (event.type == UpdateType.SECOND) {
            Bukkit.getOnlinePlayers().forEach { player ->
                player.allowFlight = true
                player.foodLevel = 20
                player.health = player.getAttribute(Attribute.MAX_HEALTH)!!.value
            }
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onExplode(event: BlockExplodeEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onBlockGrow(event: BlockGrowEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (event.damager.type == EntityType.PLAYER && event.entity.type == EntityType.PLAYER) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onSpawn(event: CreatureSpawnEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onWeatherChange(event: WeatherChangeEvent) {
        event.isCancelled = true
    }

}