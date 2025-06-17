package cn.fandmc.fandtpaplus.managers

import cn.fandmc.fandtpaplus.Main
import cn.fandmc.fandtpaplus.models.TpaRequest
import cn.fandmc.fandtpaplus.models.TpaType
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture
import kotlin.collections.HashMap

class TpaManager(private val plugin: Main) {
    private val requests = ConcurrentHashMap<UUID, MutableList<TpaRequest>>()
    private val cooldowns = ConcurrentHashMap<UUID, Long>()
    private val warmupTasks = ConcurrentHashMap<UUID, Any>()

    private var timeout: Int = 60
    private var cooldown: Int = 5
    private var maxPendingRequests: Int = 5
    private var allowCrossWorld: Boolean = true
    private var warmup: Int = 3
    private var cancelOnMove: Boolean = true
    private var cancelOnDamage: Boolean = true

    init {
        loadSettings()
    }

    private fun loadSettings() {
        timeout = plugin.configManager.getInt("tpa.timeout", 60)
        cooldown = plugin.configManager.getInt("tpa.cooldown", 5)
        maxPendingRequests = plugin.configManager.getInt("tpa.max-pending-requests", 5)
        allowCrossWorld = plugin.configManager.getBoolean("tpa.allow-cross-world", true)
        warmup = plugin.configManager.getInt("tpa.warm-up", 3)
        cancelOnMove = plugin.configManager.getBoolean("tpa.cancel-on-move", true)
        cancelOnDamage = plugin.configManager.getBoolean("tpa.cancel-on-damage", true)
    }

    fun createRequest(sender: Player, target: Player, type: TpaType): Boolean {
        // 检查冷却
        if (isOnCooldown(sender)) {
            val remaining = getCooldownRemaining(sender)
            plugin.languageManager.sendMessage(sender, "tpa.cooldown", "time" to remaining)
            return false
        }

        // 检查是否有待处理的请求
        val targetRequests = requests[target.uniqueId] ?: mutableListOf()
        if (targetRequests.any { it.sender == sender.uniqueId && !it.isExpired() }) {
            plugin.languageManager.sendMessage(sender, "tpa.already-pending")
            return false
        }

        // 检查请求数量限制
        if (targetRequests.size >= maxPendingRequests) {
            targetRequests.removeIf { it.isExpired() }
            if (targetRequests.size >= maxPendingRequests) {
                plugin.languageManager.sendMessage(sender, "tpa.already-pending")
                return false
            }
        }

        // 检查跨世界传送
        if (!allowCrossWorld && sender.world != target.world) {
            plugin.languageManager.sendMessage(sender, "tpa.cross-world-disabled")
            return false
        }

        // 创建请求
        val request = TpaRequest(
            sender = sender.uniqueId,
            target = target.uniqueId,
            type = type,
            createTime = System.currentTimeMillis(),
            timeout = timeout * 1000L
        )

        requests.computeIfAbsent(target.uniqueId) { mutableListOf() }.add(request)

        // 设置冷却
        setCooldown(sender)

        // 发送消息
        plugin.languageManager.sendMessage(sender, "tpa.request-sent", "player" to target.name)

        when (type) {
            TpaType.TPA -> plugin.languageManager.sendMessage(target, "tpa.request-received", "player" to sender.name)
            TpaType.TPAHERE -> plugin.languageManager.sendMessage(target, "tpa.request-received-here", "player" to sender.name)
        }

        plugin.languageManager.sendMessage(target, "tpa.request-accept-hint")

        // 设置超时任务
        plugin.scheduler.runTaskLaterAsynchronously({
            if (requests[target.uniqueId]?.remove(request) == true) {
                sender.sendMessage(plugin.languageManager.getComponent("tpa.request-timeout"))
                target.sendMessage(plugin.languageManager.getComponent("tpa.request-timeout"))
            }
        }, timeout * 20L)

        return true
    }

    fun acceptRequest(player: Player): Boolean {
        val playerRequests = requests[player.uniqueId] ?: return false

        // 移除过期请求
        playerRequests.removeIf { it.isExpired() }

        if (playerRequests.isEmpty()) {
            plugin.languageManager.sendMessage(player, "tpa.no-pending-request")
            return false
        }

        // 获取最新的请求
        val request = playerRequests.removeAt(playerRequests.size - 1)
        val sender = plugin.server.getPlayer(request.sender)

        if (sender == null || !sender.isOnline) {
            plugin.languageManager.sendMessage(player, "command.player-not-found", "player" to "请求者")
            return false
        }

        // 发送接受消息
        plugin.languageManager.sendMessage(player, "tpa.request-accepted")
        plugin.languageManager.sendMessage(sender, "tpa.request-accepted")

        // 开始传送
        when (request.type) {
            TpaType.TPA -> startTeleport(sender, player)
            TpaType.TPAHERE -> startTeleport(player, sender)
        }

        return true
    }

    fun denyRequest(player: Player): Boolean {
        val playerRequests = requests[player.uniqueId] ?: return false

        playerRequests.removeIf { it.isExpired() }

        if (playerRequests.isEmpty()) {
            plugin.languageManager.sendMessage(player, "tpa.no-pending-request")
            return false
        }

        val request = playerRequests.removeAt(playerRequests.size - 1)
        val sender = plugin.server.getPlayer(request.sender)

        plugin.languageManager.sendMessage(player, "tpa.request-denied")
        sender?.let {
            plugin.languageManager.sendMessage(it, "tpa.request-denied")
        }

        return true
    }

    fun cancelRequest(player: Player): Boolean {
        var cancelled = false

        // 取消作为发送者的请求
        requests.values.forEach { requestList ->
            requestList.removeIf { request ->
                if (request.sender == player.uniqueId && !request.isExpired()) {
                    val target = plugin.server.getPlayer(request.target)
                    target?.let {
                        plugin.languageManager.sendMessage(it, "tpa.request-cancelled")
                    }
                    cancelled = true
                    true
                } else {
                    false
                }
            }
        }

        if (cancelled) {
            plugin.languageManager.sendMessage(player, "tpa.request-cancelled")
        } else {
            plugin.languageManager.sendMessage(player, "tpa.no-pending-request")
        }

        return cancelled
    }

    private fun startTeleport(player: Player, target: Player) {
        if (warmup <= 0) {
            performTeleport(player, target)
            return
        }

        plugin.languageManager.sendMessage(player, "tpa.warmup", "time" to warmup)

        val startLocation = player.location.clone()
        val startHealth = player.health

        val task = plugin.scheduler.runTaskTimer({
            // 检查玩家是否移动
            if (cancelOnMove && player.location.distance(startLocation) > 0.5) {
                cancelWarmup(player, "tpa.warmup-cancelled")
                return@runTaskTimer
            }

            // 检查玩家是否受伤
            if (cancelOnDamage && player.health < startHealth) {
                cancelWarmup(player, "tpa.warmup-cancelled-damage")
                return@runTaskTimer
            }
        }, 0L, 10L)

        warmupTasks[player.uniqueId] = task

        plugin.scheduler.runTaskLater({
            warmupTasks.remove(player.uniqueId)?.let {
                plugin.scheduler.cancelTask(it)
            }
            performTeleport(player, target)
        }, warmup * 20L)
    }

    private fun performTeleport(player: Player, target: Player) {
        CompletableFuture.runAsync {
            // 保存返回位置
            plugin.backManager.saveLocation(player)

            // 执行传送
            plugin.scheduler.runAtEntity(player) {
                player.teleport(target)
            }
        }
    }

    private fun cancelWarmup(player: Player, messageKey: String) {
        warmupTasks.remove(player.uniqueId)?.let {
            plugin.scheduler.cancelTask(it)
        }
        plugin.languageManager.sendMessage(player, messageKey)
    }

    private fun isOnCooldown(player: Player): Boolean {
        if (player.hasPermission("fandtpaplus.bypass.cooldown")) {
            return false
        }

        val lastUse = cooldowns[player.uniqueId] ?: return false
        return System.currentTimeMillis() - lastUse < cooldown * 1000
    }

    private fun getCooldownRemaining(player: Player): Int {
        val lastUse = cooldowns[player.uniqueId] ?: return 0
        val remaining = cooldown - ((System.currentTimeMillis() - lastUse) / 1000)
        return remaining.toInt().coerceAtLeast(1)
    }

    private fun setCooldown(player: Player) {
        if (!player.hasPermission("fandtpaplus.bypass.cooldown")) {
            cooldowns[player.uniqueId] = System.currentTimeMillis()
        }
    }

    fun cleanupPlayer(player: Player) {
        requests.remove(player.uniqueId)
        cooldowns.remove(player.uniqueId)
        warmupTasks.remove(player.uniqueId)?.let {
            plugin.scheduler.cancelTask(it)
        }

        // 移除该玩家发送的请求
        requests.values.forEach { requestList ->
            requestList.removeIf { it.sender == player.uniqueId }
        }
    }

    fun reload() {
        loadSettings()
    }
}