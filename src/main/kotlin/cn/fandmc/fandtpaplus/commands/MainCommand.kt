package cn.fandmc.fandtpaplus.commands

import cn.fandmc.fandtpaplus.Main
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class MainCommand(private val plugin: Main) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> {
                if (!sender.hasPermission("fandtpaplus.admin")) {
                    plugin.languageManager.sendMessage(sender, "command.no-permission")
                    return true
                }

                plugin.reload()
                plugin.languageManager.sendMessage(sender, "plugin.reload")
            }

            "version" -> {
                plugin.languageManager.sendMessage(sender, "plugin.version",
                    "version" to plugin.description.version)
            }

            "update" -> {
                if (!sender.hasPermission("fandtpaplus.admin")) {
                    plugin.languageManager.sendMessage(sender, "command.no-permission")
                    return true
                }

                plugin.languageManager.sendMessage(sender, "update.checking")
                plugin.updateChecker.checkForUpdate { hasUpdate, latestVersion ->
                    if (hasUpdate) {
                        plugin.languageManager.sendMessage(sender, "update.available",
                            "version" to latestVersion,
                            "current" to plugin.description.version)

                        if (sender.hasPermission("fandtpaplus.admin")) {
                            plugin.updateChecker.downloadAndUpdate(latestVersion)
                        }
                    } else {
                        plugin.languageManager.sendMessage(sender, "update.up-to-date")
                    }
                }
            }

            "help" -> {
                showHelp(sender)
            }

            else -> {
                showHelp(sender)
            }
        }

        return true
    }

    private fun showHelp(sender: CommandSender) {
        plugin.languageManager.sendMessage(sender, "plugin.help.header")

        if (sender.hasPermission("fandtpaplus.command.tpa")) {
            plugin.languageManager.sendMessage(sender, "plugin.help.tpa")
            plugin.languageManager.sendMessage(sender, "plugin.help.tpahere")
            plugin.languageManager.sendMessage(sender, "plugin.help.tpaccept")
            plugin.languageManager.sendMessage(sender, "plugin.help.tpdeny")
            plugin.languageManager.sendMessage(sender, "plugin.help.tpcancel")
        }

        if (sender.hasPermission("fandtpaplus.command.home")) {
            plugin.languageManager.sendMessage(sender, "plugin.help.home")
            plugin.languageManager.sendMessage(sender, "plugin.help.sethome")
            plugin.languageManager.sendMessage(sender, "plugin.help.delhome")
        }

        if (sender.hasPermission("fandtpaplus.command.back")) {
            plugin.languageManager.sendMessage(sender, "plugin.help.back")
        }

        if (sender.hasPermission("fandtpaplus.command.title")) {
            plugin.languageManager.sendMessage(sender, "plugin.help.title")
        }

        if (sender.hasPermission("fandtpaplus.admin")) {
            plugin.languageManager.sendMessage(sender, "plugin.help.reload")
        }

        plugin.languageManager.sendMessage(sender, "plugin.help.version")
        plugin.languageManager.sendMessage(sender, "plugin.help.footer")
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String> {
        if (args.size == 1) {
            val suggestions = mutableListOf("help", "version")

            if (sender.hasPermission("fandtpaplus.admin")) {
                suggestions.add("reload")
                suggestions.add("update")
            }

            return suggestions.filter { it.lowercase().startsWith(args[0].lowercase()) }
        }

        return emptyList()
    }
}