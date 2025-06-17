package cn.fandmc.fandtpaplus.listeners

import cn.fandmc.fandtpaplus.Main
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent

class PlayerListener(private val plugin: Main) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // 创建计分板
        plugin.scoreboardManager.createScoreboard(player)

        // 更新Tab列表
        plugin.tabManager.updatePlayerListName(player)

        // 检查更新（仅限OP）
        if (player.isOp && plugin.configManager.getBoolean("update-checker.notify-ops", true)) {
            plugin.scheduler.runTaskLaterAsynchronously({
                plugin.updateChecker.checkForUpdate { hasUpdate, latestVersion ->
                    if (hasUpdate) {
                        plugin.languageManager.sendMessage(player, "update.notify",
                            "version" to latestVersion)
                    }
                }
            }, 60L) // 延迟3秒
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player

        // 清理数据
        plugin.tpaManager.cleanupPlayer(player)
        plugin.homeManager.cleanupPlayer(player)
        plugin.backManager.cleanupPlayer(player)
        plugin.scoreboardManager.cleanupPlayer(player)
        plugin.tabManager.cleanupPlayer(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity

        // 保存死亡位置
        if (plugin.backManager.shouldSaveOnDeath()) {
            plugin.backManager.saveLocation(player)
            plugin.languageManager.sendMessage(player, "back.saved")
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        val player = event.player

        // 排除插件自己的传送
        if (event.cause == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            return
        }

        // 保存传送前的位置
        if (plugin.backManager.shouldSaveOnTeleport()) {
            plugin.backManager.saveLocation(player, event.from)
        }
    }
}