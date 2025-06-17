package cn.fandmc.fandtpaplus.commands

import cn.fandmc.fandtpaplus.Main
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TitleCommand(private val plugin: Main) : CommandExecutor, TabCompleter {

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

        if (!sender.hasPermission("fandtpaplus.command.title")) {
            plugin.languageManager.sendMessage(sender, "command.no-permission")
            return true
        }

        if (args.isEmpty()) {
            // 显示当前称号
            val currentTitle = plugin.titleManager.getTitle(sender)
            if (currentTitle != null) {
                plugin.languageManager.sendMessage(sender, "title.current", "title" to currentTitle)
            } else {
                plugin.languageManager.sendMessage(sender, "title.current", "title" to plugin.languageManager.getMessage("title.none"))
            }
            return true
        }

        when (args[0].lowercase()) {
            "remove", "off", "clear" -> {
                plugin.titleManager.removeTitle(sender)
            }

            "list" -> {
                val titles = plugin.titleManager.listAvailableTitles(sender)
                if (titles.isEmpty()) {
                    plugin.languageManager.sendMessage(sender, "title.list", "titles" to plugin.languageManager.getMessage("title.none"))
                } else {
                    plugin.languageManager.sendMessage(sender, "title.list", "titles" to titles.joinToString(", "))
                }
            }

            else -> {
                // 设置称号
                val title = args.joinToString(" ")
                plugin.titleManager.setTitle(sender, title)
            }
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String> {
        if (args.size == 1 && sender is Player) {
            val suggestions = mutableListOf("remove", "list")
            suggestions.addAll(plugin.titleManager.listAvailableTitles(sender))
            return suggestions.filter { it.lowercase().startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}