package cn.fandmc.fandtpaplus.managers

import cn.fandmc.fandtpaplus.Main
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class BackManager(private val plugin: Main) {
    private val backLocations = ConcurrentHashMap<UUID, Location>()
    private val cooldowns = ConcurrentHashMap<UUID, Long>()

    private var enabled: Boolean = true
    private var cooldown: Int = 10
    private var saveOnDeath: Boolean = true
    private var saveOnTeleport: Boolean = true

    init {
        loadSettings()
    }

    private fun loadSettings() {
        enabled = plugin.configManager.getBoolean("back.enabled", true)
        cooldown = plugin.configManager.getInt("back.cooldown", 10)
        saveOnDeath = plugin.configManager.getBoolean("back.save-on-death", true)
        saveOnTeleport = plugin.configManager.getBoolean("back.save-on-teleport", true)
    }

    fun saveLocation(player: Player, location: Location? = null) {
        if (!enabled) return

                val loc = location ?: player.location
        backLocations[player.uniqueId] = loc.clone()
    }

    fun teleportBack(player: Player): Boolean {
        if (!enabled) {
            plugin.languageManager.sendMessage(player, "back.disabled")
            return false
        }

        val location = backLocations[player.uniqueId]
        if (location == null) {
            plugin.languageManager.sendMessage(player, "back.no-location")
            return false
        }

        // 检查冷却
        if (!player.hasPermission("fandtpaplus.bypass.cooldown")) {
            val lastUse = cooldowns[player.uniqueId] ?: 0
            val timePassed = (System.currentTimeMillis() - lastUse) / 1000

            if (timePassed < cooldown) {
                val remaining = cooldown - timePassed
                plugin.languageManager.sendMessage(player, "back.cooldown", "time" to remaining)
                return false
            }
        }

        // 检查世界是否存在
        if (location.world == null) {
            plugin.languageManager.sendMessage(player, "back.no-location")
            backLocations.remove(player.uniqueId)
            return false
        }

        // 保存当前位置作为新的返回点
        saveLocation(player)

        // 发送消息
        plugin.languageManager.sendMessage(player, "back.returning")

        // 执行传送
        plugin.scheduler.runAtEntity(player) {
            player.teleport(location)
        }

        // 设置冷却
        if (!player.hasPermission("fandtpaplus.bypass.cooldown")) {
            cooldowns[player.uniqueId] = System.currentTimeMillis()
        }

        return true
    }

    fun hasBackLocation(player: Player): Boolean {
        return backLocations.containsKey(player.uniqueId)
    }

    fun getBackLocation(player: Player): Location? {
        return backLocations[player.uniqueId]?.clone()
    }

    fun cleanupPlayer(player: Player) {
        backLocations.remove(player.uniqueId)
        cooldowns.remove(player.uniqueId)
    }

    fun reload() {
        loadSettings()
    }

    fun isEnabled(): Boolean = enabled
    fun shouldSaveOnDeath(): Boolean = saveOnDeath && enabled
    fun shouldSaveOnTeleport(): Boolean = saveOnTeleport && enabled
}