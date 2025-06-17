package cn.fandmc.fandtpaplus.commands

import cn.fandmc.fandtpaplus.Main
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class HomeCommand(private val plugin: Main) : CommandExecutor, TabCompleter {

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

        if (!sender.hasPermission("fandtpaplus.command.home")) {
            plugin.languageManager.sendMessage(sender, "command.no-permission")
            return true
        }

        val homes = plugin.homeManager.getHomes(sender)

        if (homes.isEmpty()) {
            plugin.languageManager.sendMessage(sender, "home.list-empty")
            return true
        }

        if (args.isEmpty()) {
            // 如果只有一个家，直接传送
            if (homes.size == 1) {
                plugin.homeManager.teleportHome(sender, homes.first())
            } else {
                // 显示家列表
                plugin.languageManager.sendMessage(sender, "home.list", "homes" to homes.joinToString(", "))
            }
            return true
        }

        val homeName = args[0]
        plugin.homeManager.teleportHome(sender, homeName)

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String> {
        if (args.size == 1 && sender is Player) {
            return plugin.homeManager.getHomes(sender)
                .filter { it.lowercase().startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}

class SetHomeCommand(private val plugin: Main) : CommandExecutor {

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

        if (!sender.hasPermission("fandtpaplus.command.sethome")) {
            plugin.languageManager.sendMessage(sender, "command.no-permission")
            return true
        }

        if (args.isEmpty()) {
            plugin.languageManager.sendMessage(sender, "command.invalid-usage", "usage" to "/sethome <家名>")
            return true
        }

        val homeName = args[0]
        plugin.homeManager.setHome(sender, homeName)

        return true
    }
}

class DelHomeCommand(private val plugin: Main) : CommandExecutor, TabCompleter {

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

        if (!sender.hasPermission("fandtpaplus.command.delhome")) {
            plugin.languageManager.sendMessage(sender, "command.no-permission")
            return true
        }

        if (args.isEmpty()) {
            plugin.languageManager.sendMessage(sender, "command.invalid-usage", "usage" to "/delhome <家名>")
            return true
        }

        val homeName = args[0]
        plugin.homeManager.deleteHome(sender, homeName)

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String> {
        if (args.size == 1 && sender is Player) {
            return plugin.homeManager.getHomes(sender)
                .filter { it.lowercase().startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}