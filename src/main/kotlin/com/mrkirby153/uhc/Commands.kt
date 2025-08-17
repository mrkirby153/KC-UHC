package com.mrkirby153.uhc

class Commands(private val plugin: UHCPlugin) {


    fun register() {
        plugin.logger.info { "Registering commands" }
        this.registerCommandCompletions()

        // TODO: Register commands here
        plugin.logger.info { "Commands registered" }
    }

    private fun registerCommandCompletions() {

    }
}