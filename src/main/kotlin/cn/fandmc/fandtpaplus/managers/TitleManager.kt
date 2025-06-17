package cn.fandmc.fandtpaplus.managers

import cn.fandmc.fandtpaplus.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

class TitleManager(private val plugin: Main) {
    private val playerTitles = ConcurrentHashMap<UUID, String>()
    private val availableTitles = ConcurrentHashMap<String, TitleData>()

    private var enabled: Boolean = true
    private var maxLength: Int = 16
    private var allowColorCodes: Boolean = true
    private var blockedWords: List<String> = emptyList()

    data class TitleData(
        val name: String,
        val displayName: String,
        val permission: String? = null
    )

    init {
        loadSettings()
        loadTitles()
    }

    private fun loadSettings() {
        enabled = plugin.configManager.getBoolean("title.enabled", true)
        maxLength = plugin.configManager.getInt("title.max-length", 16)
        allowColorCodes = plugin.configManager.getBoolean("title.allow-color-codes", true)
        blockedWords = plugin.configManager.getStringList("title.blocked-words")
    }

    private fun loadTitles() {
        CompletableFuture.runAsync {
            try {
                // 加载玩家称号
                val titlesFile = File(plugin.dataFolder, "data/titles.yml")
                if (titlesFile.exists()) {
                    val config = YamlConfiguration.loadConfiguration(titlesFile)

                    config.getKeys(false).forEach { uuidString ->
                        try {
                            val uuid = UUID.fromString(uuidString)
                            val title = config.getString(uuidString)
                            if (!title.isNullOrEmpty()) {
                                playerTitles[uuid] = title
                            }
                        } catch (e: Exception) {
                            plugin.logger.log(Level.WARNING, "加载玩家 $uuidString 的称号失败", e)
                        }
                    }
                }

                // 加载预设称号
                loadPresetTitles()

                plugin.logger.info("成功加载 ${playerTitles.size} 个玩家称号")
            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, "加载称号数据失败", e)
            }
        }
    }

    private fun loadPresetTitles() {
        val presetsFile = File(plugin.dataFolder, "presets/titles.yml")
        if (!presetsFile.exists()) {
            saveDefaultPresets()
        }

        val config = YamlConfiguration.loadConfiguration(presetsFile)
        config.getKeys(false).forEach { key ->
            val section = config.getConfigurationSection(key)
            if (section != null) {
                val titleData = TitleData(
                    name = key,
                    displayName = section.getString("display", key) ?: key,
                    permission = section.getString("permission")
                )
                availableTitles[key.lowercase()] = titleData
            }
        }
    }

    private fun saveDefaultPresets() {
        val presetsDir = File(plugin.dataFolder, "presets")
        if (!presetsDir.exists()) {
            presetsDir.mkdirs()
        }

        val presetsFile = File(presetsDir, "titles.yml")
        val config = YamlConfiguration()

        // 添加默认称号
        config.set("vip.display", "&6[VIP]")
        config.set("vip.permission", "fandtpaplus.title.vip")

        config.set("mvp.display", "&b[MVP]")
        config.set("mvp.permission", "fandtpaplus.title.mvp")

        config.set("hero.display", "&c[英雄]")
        config.set("hero.permission", "fandtpaplus.title.hero")

        config.set("legend.display", "&5[传奇]")
        config.set("legend.permission", "fandtpaplus.title.legend")

        try {
            config.save(presetsFile)
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "保存默认称号预设失败", e)
        }
    }

    fun saveData() {
        try {
            val dataDir = File(plugin.dataFolder, "data")
            if (!dataDir.exists()) {
                dataDir.mkdirs()
            }

            val titlesFile = File(dataDir, "titles.yml")
            val config = YamlConfiguration()

            playerTitles.forEach { (uuid, title) ->
                config.set(uuid.toString(), title)
            }

            config.save(titlesFile)
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "保存称号数据失败", e)
        }
    }

    fun setTitle(player: Player, title: String): Boolean {
        if (!enabled) {
            return false
        }

        // 检查是否是预设称号
        val presetTitle = availableTitles[title.lowercase()]
        if (presetTitle != null) {
            if (presetTitle.permission != null && !player.hasPermission(presetTitle.permission)) {
                plugin.languageManager.sendMessage(player, "title.no-permission-title")
                return false
            }

            playerTitles[player.uniqueId] = presetTitle.displayName
            plugin.languageManager.sendMessage(player, "title.set", "title" to formatTitle(presetTitle.displayName))

            CompletableFuture.runAsync { saveData() }
            return true
        }

        // 自定义称号检查
        val strippedTitle = ChatColor.stripColor(title) ?: title

        // 检查长度
        if (strippedTitle.length > maxLength) {
            plugin.languageManager.sendMessage(player, "title.invalid-length", "max" to maxLength)
            return false
        }

        // 检查屏蔽词
        if (blockedWords.any { strippedTitle.contains(it, ignoreCase = true) }) {
            plugin.languageManager.sendMessage(player, "title.blocked-word")
            return false
        }

        // 检查颜色代码权限
        if (title.contains('&') && !allowColorCodes) {
            plugin.languageManager.sendMessage(player, "title.no-permission-color")
            return false
        }

        if (title.contains('&') && !player.hasPermission("fandtpaplus.title.color")) {
            plugin.languageManager.sendMessage(player, "title.no-permission-color")
            return false
        }

        playerTitles[player.uniqueId] = title
        plugin.languageManager.sendMessage(player, "title.set", "title" to formatTitle(title))

        CompletableFuture.runAsync { saveData() }
        return true
    }

    fun removeTitle(player: Player): Boolean {
        if (playerTitles.remove(player.uniqueId) != null) {
            plugin.languageManager.sendMessage(player, "title.removed")
            CompletableFuture.runAsync { saveData() }
            return true
        }
        return false
    }

    fun getTitle(player: Player): String? {
        return playerTitles[player.uniqueId]
    }

    fun getFormattedTitle(player: Player): Component? {
        val title = getTitle(player) ?: return null
        return LegacyComponentSerializer.legacyAmpersand().deserialize(title)
    }

    fun listAvailableTitles(player: Player): List<String> {
        return availableTitles.values
            .filter { it.permission == null || player.hasPermission(it.permission) }
            .map { it.name }
    }

    private fun formatTitle(title: String): String {
        return ChatColor.translateAlternateColorCodes('&', title)
    }

    fun cleanupPlayer(player: Player) {
        // 不移除称号，保持玩家的称号
    }

    fun reload() {
        loadSettings()
        availableTitles.clear()
        loadPresetTitles()
    }
}