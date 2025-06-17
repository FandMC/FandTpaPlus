package cn.fandmc.fandtpaplus.commands

import cn.fandmc.fandtpaplus.Main
import cn.fandmc.fandtpaplus.models.TpaType
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TpaCommand(private val plugin: Main) : CommandExecutor, TabCompleter {

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

        if (!sender.hasPermission("fandtpaplus.command.tpa")) {
            plugin.languageManager.sendMessage(sender, "command.no-permission")
            return true
        }

        if (args.isEmpty()) {
            plugin.languageManager.sendMessage(sender, "command.invalid-usage", "usage" to "/tpa <玩家>")
            return true
        }

        val targetName = args[0]
        val target = plugin.server.getPlayer(targetName)

        if (target == null || !target.isOnline) {
            plugin.languageManager.sendMessage(sender, "command.player-not-found", "player" to targetName)
            return true
        }

        if (target == sender) {
            plugin.languageManager.sendMessage(sender, "tpa.cannot-tpa-self")
            return true
        }

        plugin.tpaManager.createRequest(sender, target, TpaType.TPA)

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String> {
        if (args.size == 1 && sender is Player) {
            return plugin.server.onlinePlayers
                .filter { it != sender && it.name.lowercase().startsWith(args[0].lowercase()) }
                .map { it.name }
        }
        return emptyList()
    }
}

class TpaHereCommand(private val plugin: Main) : CommandExecutor, TabCompleter {

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

        if (!sender.hasPermission("fandtpaplus.command.tpahere")) {
            plugin.languageManager.sendMessage(sender, "command.no-permission")
            return true
        }

        if (args.isEmpty()) {
            plugin.languageManager.sendMessage(sender, "command.invalid-usage", "usage" to "/tpahere <玩家>")
            return true
        }

        val targetName = args[0]
        val target = plugin.server.getPlayer(targetName)

        if (target == null || !target.isOnline) {
            plugin.languageManager.sendMessage(sender, "command.player-not-found", "player" to targetName)
            return true
        }

        if (target == sender) {
            plugin.languageManager.sendMessage(sender, "tpa.cannot-tpa-self")
            return true
        }

        plugin.tpaManager.createRequest(sender, target, TpaType.TPAHERE)

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String> {
        if (args.size == 1 && sender is Player) {
            return plugin.server.onlinePlayers
                .filter { it != sender && it.name.lowercase().startsWith(args[0].lowercase()) }
                .map { it.name }
        }
        return emptyList()
    }
}

class TpAcceptCommand(private val plugin: Main) : CommandExecutor {

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

        if (!sender.hasPermission("fandtpaplus.command.tpaccept")) {
            plugin.languageManager.sendMessage(sender, "command.no-permission")
            return true
        }

        plugin.tpaManager.acceptRequest(sender)

        return true
    }
}

class TpDenyCommand(private val plugin: Main) : CommandExecutor {

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

        if (!sender.hasPermission("fandtpaplus.command.tpdeny")) {
            plugin.languageManager.sendMessage(sender, "command.no-permission")
            return true
        }

        plugin.tpaManager.denyRequest(sender)

        return true
    }
}

class TpCancelCommand(private val plugin: Main) : CommandExecutor {

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

        if (!sender.hasPermission("fandtpaplus.command.tpcancel")) {
            plugin.languageManager.sendMessage(sender, "command.no-permission")
            return true
        }

        plugin.tpaManager.cancelRequest(sender)

        return true
    }
}