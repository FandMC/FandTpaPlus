package cn.fandmc.fandtpaplus.managers

import cn.fandmc.fandtpaplus.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player

class TabManager(private val plugin: Main) {
    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    private var enabled: Boolean = true
    private var updateInterval: Long = 100L
    private var headerLines: List<String> = emptyList()
    private var footerLines: List<String> = emptyList()
    private var playerFormat: String = "%title% &f%player%"
    private var updateTask: Any? = null

    init {
        loadSettings()
        if (enabled) {
            startUpdateTask()
        }
    }

    private fun loadSettings() {
        enabled = plugin.configManager.getBoolean("tab.enabled", true)
        updateInterval = plugin.configManager.getInt("tab.update-interval", 100).toLong()
        headerLines = plugin.configManager.getStringList("tab.header")
        footerLines = plugin.configManager.getStringList("tab.footer")
        playerFormat = plugin.configManager.getString("tab.player-format", "%title% &f%player%")

        if (headerLines.isEmpty()) {
            headerLines = listOf(
                "&6&lFandTpaPlus",
                "&e欢迎来到服务器！"
            )
        }

        if (footerLines.isEmpty()) {
            footerLines = listOf(
                "&7在线玩家: &f%online%/%max%",
                "&7TPS: &f%tps%"
            )
        }
    }

    private fun startUpdateTask() {
        updateTask = plugin.scheduler.runTaskTimerAsynchronously({
            updateAllPlayers()
        }, 0L, updateInterval)
    }

    private fun updateAllPlayers() {
        if (!enabled) return

        val header = buildHeader()
        val footer = buildFooter()

        plugin.scheduler.runTask {
            Bukkit.getOnlinePlayers().forEach { player ->
                updatePlayer(player, header, footer)
            }
        }
    }

    private fun updatePlayer(player: Player, header: Component, footer: Component) {
        // 更新头部和尾部
        player.sendPlayerListHeaderAndFooter(header, footer)

        // 更新玩家列表名称
        updatePlayerListName(player)
    }

    fun updatePlayerListName(player: Player) {
        if (!enabled) return

        var format = playerFormat

        // 替换占位符
        format = format.replace("%player%", player.name)

        // 替换称号
        val title = plugin.titleManager.getTitle(player)
        if (title != null) {
            format = format.replace("%title%", title)
        } else {
            format = format.replace("%title% ", "") // 移除称号占位符和空格
            format = format.replace("%title%", "")
        }

        // 设置玩家列表名称
        val component = serializer.deserialize(format)
        player.playerListName(component)
    }

    private fun buildHeader(): Component {
        val processedLines = headerLines.map { line ->
            processGlobalPlaceholders(line)
        }

        return serializer.deserialize(processedLines.joinToString("\n"))
    }

    private fun buildFooter(): Component {
        val processedLines = footerLines.map { line ->
            processGlobalPlaceholders(line)
        }

        return serializer.deserialize(processedLines.joinToString("\n"))
    }

    private fun processGlobalPlaceholders(text: String): String {
        var result = text

        // 在线玩家
        result = result.replace("%online%", Bukkit.getOnlinePlayers().size.toString())
        result = result.replace("%max%", Bukkit.getMaxPlayers().toString())

        // TPS - 在Folia中需要特殊处理
        val tpsString = if (Main.isFolia) {
            "20.0" // Folia中默认显示20
        } else {
            try {
                String.format("%.1f", Bukkit.getTPS()[0].coerceAtMost(20.0))
            } catch (e: Exception) {
                "20.0"
            }
        }
        result = result.replace("%tps%", tpsString)

        // 时间
        val time = System.currentTimeMillis()
        val hour = (time / 3600000) % 24
        val minute = (time / 60000) % 60
        result = result.replace("%time%", String.format("%02d:%02d", hour, minute))

        return ChatColor.translateAlternateColorCodes('&', result)
    }

    fun reload() {
        // 停止更新任务
        updateTask?.let { plugin.scheduler.cancelTask(it) }

        // 重新加载设置
        loadSettings()

        // 重启更新任务
        if (enabled) {
            startUpdateTask()
        }
    }

    fun cleanupPlayer(player: Player) {
        // Tab列表会自动清理，无需特殊处理
    }
}