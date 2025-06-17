package cn.fandmc.fandtpaplus.managers

import cn.fandmc.fandtpaplus.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

class ScoreboardManager(private val plugin: Main) {
    private val playerScoreboards = ConcurrentHashMap<UUID, Scoreboard>()
    private val serializer = LegacyComponentSerializer.legacyAmpersand()

    private var enabled: Boolean = true
    private var updateInterval: Long = 20L
    private var title: String = "&6&lFandTpaPlus"
    private var lines: List<String> = emptyList()
    private var updateTask: Any? = null

    init {
        loadSettings()
        if (enabled) {
            startUpdateTask()
        }
    }

    private fun loadSettings() {
        enabled = plugin.configManager.getBoolean("scoreboard.enabled", true)
        updateInterval = plugin.configManager.getInt("scoreboard.update-interval", 20).toLong()
        title = plugin.configManager.getString("scoreboard.title", "&6&lFandTpaPlus")
        lines = plugin.configManager.getStringList("scoreboard.lines")

        if (lines.isEmpty()) {
            lines = listOf(
                "&7&m----------------",
                "&e玩家: &f%player%",
                "&e世界: &f%world%",
                "&e在线: &f%online%/%max%",
                "&e称号: &f%title%",
                "&7&m----------------"
            )
        }
    }

    private fun startUpdateTask() {
        updateTask = plugin.scheduler.runTaskTimerAsynchronously({
            Bukkit.getOnlinePlayers().forEach { player ->
                updateScoreboard(player)
            }
        }, 0L, updateInterval)
    }

    fun createScoreboard(player: Player) {
        if (!enabled) return

        plugin.scheduler.runAtEntity(player) {
            try {
                // 在Folia中，我们使用玩家现有的scoreboard
                val scoreboard = if (Main.isFolia) {
                    player.scoreboard
                } else {
                    Bukkit.getScoreboardManager().newScoreboard
                }

                // 移除旧的objective
                scoreboard.getObjective("fandtpaplus")?.unregister()

                val objective = scoreboard.registerNewObjective(
                    "fandtpaplus",
                    "dummy",
                    serializer.deserialize(title)
                )
                objective.displaySlot = DisplaySlot.SIDEBAR

                playerScoreboards[player.uniqueId] = scoreboard
                if (!Main.isFolia) {
                    player.scoreboard = scoreboard
                }

                updateScoreboard(player)
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "创建计分板失败: ${e.message}")
            }
        }
    }

    private fun updateScoreboard(player: Player) {
        if (!enabled) return

        val scoreboard = playerScoreboards[player.uniqueId] ?: return
        val objective = scoreboard.getObjective("fandtpaplus") ?: return

        plugin.scheduler.runAtEntity(player) {
            try {
                // 清除旧的行
                scoreboard.entries.forEach { entry ->
                    scoreboard.resetScores(entry)
                }

                // 添加新的行
                var score = lines.size
                lines.forEach { line ->
                    val processedLine = processPlaceholders(line, player)
                    val coloredLine = ChatColor.translateAlternateColorCodes('&', processedLine)

                    // 确保每行都是唯一的
                    var uniqueLine = coloredLine
                    var suffix = 0
                    while (scoreboard.entries.contains(uniqueLine)) {
                        uniqueLine = coloredLine + ChatColor.RESET + ChatColor.values()[suffix % ChatColor.values().size]
                        suffix++
                    }

                    objective.getScore(uniqueLine).score = score--
                }
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "更新计分板失败: ${e.message}")
            }
        }
    }

    private fun processPlaceholders(text: String, player: Player): String {
        var result = text

        // 基础占位符
        result = result.replace("%player%", player.name)
        result = result.replace("%world%", player.world.name)
        result = result.replace("%online%", Bukkit.getOnlinePlayers().size.toString())
        result = result.replace("%max%", Bukkit.getMaxPlayers().toString())

        // 称号占位符
        val title = plugin.titleManager.getTitle(player) ?: "&7无"
        result = result.replace("%title%", title)

        // 家数量占位符
        val homes = plugin.homeManager.getHomes(player).size
        result = result.replace("%homes%", homes.toString())

        // TPS占位符 - Folia兼容
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

        // 坐标占位符
        result = result.replace("%x%", player.location.blockX.toString())
        result = result.replace("%y%", player.location.blockY.toString())
        result = result.replace("%z%", player.location.blockZ.toString())

        return result
    }

    fun removeScoreboard(player: Player) {
        playerScoreboards.remove(player.uniqueId)
        plugin.scheduler.runAtEntity(player) {
            try {
                if (!Main.isFolia) {
                    player.scoreboard = Bukkit.getScoreboardManager().mainScoreboard
                } else {
                    // 在Folia中，只移除objective
                    player.scoreboard.getObjective("fandtpaplus")?.unregister()
                }
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "移除计分板失败: ${e.message}")
            }
        }
    }

    fun toggle(player: Player) {
        if (playerScoreboards.containsKey(player.uniqueId)) {
            removeScoreboard(player)
        } else {
            createScoreboard(player)
        }
    }

    fun reload() {
        // 停止更新任务
        updateTask?.let { plugin.scheduler.cancelTask(it) }

        // 重新加载设置
        loadSettings()

        // 重新创建所有计分板
        playerScoreboards.keys.toList().forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                removeScoreboard(player)
                if (enabled) {
                    createScoreboard(player)
                }
            }
        }

        // 重启更新任务
        if (enabled) {
            startUpdateTask()
        }
    }

    fun cleanupPlayer(player: Player) {
        removeScoreboard(player)
    }
}