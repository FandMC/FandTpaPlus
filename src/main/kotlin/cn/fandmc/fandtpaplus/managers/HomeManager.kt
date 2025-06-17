package cn.fandmc.fandtpaplus.managers

import cn.fandmc.fandtpaplus.Main
import cn.fandmc.fandtpaplus.models.Home
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

class HomeManager(private val plugin: Main) {
    private val homes = ConcurrentHashMap<UUID, MutableMap<String, Home>>()
    private val teleportTasks = ConcurrentHashMap<UUID, Any>()

    private var defaultMaxHomes: Int = 3
    private var teleportDelay: Int = 3
    private var cancelOnMove: Boolean = true
    private var cancelOnDamage: Boolean = true

    init {
        loadSettings()
        loadHomes()
    }

    private fun loadSettings() {
        defaultMaxHomes = plugin.configManager.getInt("home.max-homes.default", 3)
        teleportDelay = plugin.configManager.getInt("home.teleport-delay", 3)
        cancelOnMove = plugin.configManager.getBoolean("home.cancel-on-move", true)
        cancelOnDamage = plugin.configManager.getBoolean("home.cancel-on-damage", true)
    }

    private fun loadHomes() {
        CompletableFuture.runAsync {
            try {
                val homesFile = File(plugin.dataFolder, "data/homes.yml")
                if (!homesFile.exists()) return@runAsync

                val config = YamlConfiguration.loadConfiguration(homesFile)

                config.getKeys(false).forEach { uuidString ->
                    try {
                        val uuid = UUID.fromString(uuidString)
                        val playerHomes = mutableMapOf<String, Home>()

                        config.getConfigurationSection(uuidString)?.getKeys(false)?.forEach { homeName ->
                            val homeSection = config.getConfigurationSection("$uuidString.$homeName")
                            if (homeSection != null) {
                                val world = plugin.server.getWorld(homeSection.getString("world") ?: return@forEach)
                                if (world != null) {
                                    val home = Home(
                                        name = homeName,
                                        world = world.name,
                                        x = homeSection.getDouble("x"),
                                        y = homeSection.getDouble("y"),
                                        z = homeSection.getDouble("z"),
                                        yaw = homeSection.getDouble("yaw").toFloat(),
                                        pitch = homeSection.getDouble("pitch").toFloat()
                                    )
                                    playerHomes[homeName.lowercase()] = home
                                }
                            }
                        }

                        if (playerHomes.isNotEmpty()) {
                            homes[uuid] = playerHomes
                        }
                    } catch (e: Exception) {
                        plugin.logger.log(Level.WARNING, "加载玩家 $uuidString 的家失败", e)
                    }
                }

                plugin.logger.info("成功加载 ${homes.size} 个玩家的家")
            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, "加载家数据失败", e)
            }
        }
    }

    fun saveData() {
        try {
            val dataDir = File(plugin.dataFolder, "data")
            if (!dataDir.exists()) {
                dataDir.mkdirs()
            }

            val homesFile = File(dataDir, "homes.yml")
            val config = YamlConfiguration()

            homes.forEach { (uuid, playerHomes) ->
                playerHomes.forEach { (_, home) ->
                    val path = "$uuid.${home.name}"
                    config.set("$path.world", home.world)
                    config.set("$path.x", home.x)
                    config.set("$path.y", home.y)
                    config.set("$path.z", home.z)
                    config.set("$path.yaw", home.yaw)
                    config.set("$path.pitch", home.pitch)
                }
            }

            config.save(homesFile)
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "保存家数据失败", e)
        }
    }

    fun setHome(player: Player, name: String): Boolean {
        if (!isValidHomeName(name)) {
            plugin.languageManager.sendMessage(player, "home.invalid-name")
            return false
        }

        val playerHomes = homes.computeIfAbsent(player.uniqueId) { mutableMapOf() }
        val maxHomes = getMaxHomes(player)

        if (!playerHomes.containsKey(name.lowercase()) && playerHomes.size >= maxHomes) {
            plugin.languageManager.sendMessage(player, "home.limit-reached", "limit" to maxHomes)
            return false
        }

        val home = Home(
            name = name,
            world = player.world.name,
            x = player.location.x,
            y = player.location.y,
            z = player.location.z,
            yaw = player.location.yaw,
            pitch = player.location.pitch
        )

        playerHomes[name.lowercase()] = home

        plugin.languageManager.sendMessage(player, "home.set", "home" to name)

        // 异步保存
        CompletableFuture.runAsync { saveData() }

        return true
    }

    fun deleteHome(player: Player, name: String): Boolean {
        val playerHomes = homes[player.uniqueId] ?: return false

        if (!playerHomes.containsKey(name.lowercase())) {
            plugin.languageManager.sendMessage(player, "home.not-found", "home" to name)
            return false
        }

        playerHomes.remove(name.lowercase())

        if (playerHomes.isEmpty()) {
            homes.remove(player.uniqueId)
        }

        plugin.languageManager.sendMessage(player, "home.deleted", "home" to name)

        // 异步保存
        CompletableFuture.runAsync { saveData() }

        return true
    }

    fun teleportHome(player: Player, name: String): Boolean {
        val playerHomes = homes[player.uniqueId]

        if (playerHomes == null || playerHomes.isEmpty()) {
            plugin.languageManager.sendMessage(player, "home.list-empty")
            return false
        }

        val home = playerHomes[name.lowercase()]

        if (home == null) {
            plugin.languageManager.sendMessage(player, "home.not-found", "home" to name)
            return false
        }

        val world = plugin.server.getWorld(home.world)

        if (world == null) {
            plugin.languageManager.sendMessage(player, "home.not-found", "home" to name)
            return false
        }

        val location = Location(world, home.x, home.y, home.z, home.yaw, home.pitch)

        if (teleportDelay <= 0 || player.hasPermission("fandtpaplus.bypass.delay")) {
            performTeleport(player, location, name)
        } else {
            startTeleportDelay(player, location, name)
        }

        return true
    }

    private fun startTeleportDelay(player: Player, location: Location, homeName: String) {
        plugin.languageManager.sendMessage(player, "home.teleporting", "home" to homeName)

        val startLocation = player.location.clone()
        val startHealth = player.health

        // 取消之前的传送任务
        cancelTeleport(player)

        val task = plugin.scheduler.runTaskTimer({
            if (cancelOnMove && player.location.distance(startLocation) > 0.5) {
                cancelTeleport(player)
                plugin.languageManager.sendMessage(player, "home.teleport-cancelled")
                return@runTaskTimer
            }

            if (cancelOnDamage && player.health < startHealth) {
                cancelTeleport(player)
                plugin.languageManager.sendMessage(player, "home.teleport-cancelled")
                return@runTaskTimer
            }
        }, 0L, 10L)

        teleportTasks[player.uniqueId] = task

        plugin.scheduler.runTaskLater({
            cancelTeleport(player)
            performTeleport(player, location, homeName)
        }, teleportDelay * 20L)
    }

    private fun performTeleport(player: Player, location: Location, homeName: String) {
        // 保存返回位置
        plugin.backManager.saveLocation(player)

        // 执行传送
        plugin.scheduler.runAtEntity(player) {
            player.teleport(location)
        }
    }

    private fun cancelTeleport(player: Player) {
        teleportTasks.remove(player.uniqueId)?.let {
            plugin.scheduler.cancelTask(it)
        }
    }

    fun getHomes(player: Player): List<String> {
        return homes[player.uniqueId]?.keys?.toList() ?: emptyList()
    }

    fun getHome(player: Player, name: String): Home? {
        return homes[player.uniqueId]?.get(name.lowercase())
    }

    fun hasHome(player: Player, name: String): Boolean {
        return homes[player.uniqueId]?.containsKey(name.lowercase()) ?: false
    }

    private fun getMaxHomes(player: Player): Int {
        return when {
            player.hasPermission("fandtpaplus.home.unlimited") -> Int.MAX_VALUE
            player.hasPermission("fandtpaplus.home.admin") -> plugin.configManager.getInt("home.max-homes.admin", 10)
            player.hasPermission("fandtpaplus.home.vip") -> plugin.configManager.getInt("home.max-homes.vip", 5)
            else -> defaultMaxHomes
        }
    }

    private fun isValidHomeName(name: String): Boolean {
        return name.matches(Regex("^[a-zA-Z0-9_]+$")) && name.length <= 16
    }

    fun cleanupPlayer(player: Player) {
        cancelTeleport(player)
    }

    fun reload() {
        loadSettings()
        // 不重新加载家数据，避免数据丢失
    }
}