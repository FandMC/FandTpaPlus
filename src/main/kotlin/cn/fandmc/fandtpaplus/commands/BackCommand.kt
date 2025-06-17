package cn.fandmc.fandtpaplus.commands

import cn.fandmc.fandtpaplus.Main
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class BackCommand(private val plugin: Main) : CommandExecutor {

    override fun onCommand(
            sender: CommandSender,
            command: Command,
            label: String,
            args: Array<String>
    ): Boolean {
        if (sender !is Player) {
            plugin.languageManager.sendMessage(sender, "command.player-only")
            return true
        }

        if (!sender.hasPermission("fandtpaplus.command.back")) {
            plugin.languageManager.sendMessage(sender, "command.no-permission")
            return true
        }

        plugin.backManager.teleportBack(sender)

        return true
    }
}