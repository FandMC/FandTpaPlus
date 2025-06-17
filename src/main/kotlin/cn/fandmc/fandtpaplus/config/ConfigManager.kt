package cn.fandmc.fandtpaplus.config

import cn.fandmc.fandtpaplus.Main
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.logging.Level

class ConfigManager(private val plugin: Main) {
    lateinit var config: FileConfiguration
    private set

    init {
        loadConfig()
    }

    private fun loadConfig() {
        try {
            val configFile = File(plugin.dataFolder, "config.yml")

            if (!configFile.exists()) {
                plugin.saveResource("config.yml", false)
            }

            config = YamlConfiguration.loadConfiguration(configFile)

            // 检查配置版本并更新
            checkAndUpdateConfig()
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "加载配置文件失败", e)
            config = YamlConfiguration()
            loadDefaultConfig()
        }
    }

    private fun checkAndUpdateConfig() {
        val currentVersion = config.getInt("config-version", 0)
        val latestVersion = getLatestConfigVersion()

        if (currentVersion < latestVersion) {
            plugin.logger.info("检测到配置文件需要更新...")
            updateConfig(currentVersion, latestVersion)
        }
    }

    private fun getLatestConfigVersion(): Int {
        return try {
            plugin.getResource("config.yml")?.use { stream ->
                    val defaultConfig = YamlConfiguration.loadConfiguration(
                    InputStreamReader(stream, StandardCharsets.UTF_8)
            )
                defaultConfig.getInt("config-version", 1)
            } ?: 1
        } catch (e: Exception) {
            1
        }
    }

    private fun updateConfig(currentVersion: Int, latestVersion: Int) {
        // 备份旧配置
        val backupFile = File(plugin.dataFolder, "config.yml.backup.$currentVersion")
        File(plugin.dataFolder, "config.yml").copyTo(backupFile, true)

        // 加载默认配置
        val defaultConfig = plugin.getResource("config.yml")?.use { stream ->
                YamlConfiguration.loadConfiguration(
                        InputStreamReader(stream, StandardCharsets.UTF_8)
                )
        } ?: return

                // 合并配置
                mergeConfigs(config, defaultConfig)

        // 保存更新后的配置
        config.set("config-version", latestVersion)
        saveConfig()

        plugin.logger.info("配置文件已更新到版本 $latestVersion")
    }

    private fun mergeConfigs(current: FileConfiguration, default: FileConfiguration) {
        default.getKeys(true).forEach { key ->
            if (!current.contains(key)) {
                current.set(key, default.get(key))
            }
        }
    }

    private fun loadDefaultConfig() {
        config.set("config-version", 1)

        // TPA设置
        config.set("tpa.timeout", 60)
        config.set("tpa.cooldown", 5)
        config.set("tpa.max-pending-requests", 5)
        config.set("tpa.allow-cross-world", true)
        config.set("tpa.warm-up", 3)
        config.set("tpa.cancel-on-move", true)
        config.set("tpa.cancel-on-damage", true)

        // Home设置
        config.set("home.max-homes.default", 3)
        config.set("home.max-homes.vip", 5)
        config.set("home.max-homes.admin", 10)
        config.set("home.teleport-delay", 3)
        config.set("home.cancel-on-move", true)
        config.set("home.cancel-on-damage", true)

        // Back设置
        config.set("back.enabled", true)
        config.set("back.cooldown", 10)
        config.set("back.save-on-death", true)
        config.set("back.save-on-teleport", true)

        // 称号设置
        config.set("title.enabled", true)
        config.set("title.max-length", 16)
        config.set("title.allow-color-codes", true)
        config.set("title.blocked-words", listOf("admin", "owner", "op"))

        // 计分板设置
        config.set("scoreboard.enabled", true)
        config.set("scoreboard.update-interval", 20)
        config.set("scoreboard.title", "&6&lFandTpaPlus")
        config.set("scoreboard.lines", listOf(
                "&7&m----------------",
                "&e玩家: &f%player%",
                "&e世界: &f%world%",
                "&e在线: &f%online%/%max%",
                "&e称号: &f%title%",
                "&7&m----------------"
        ))

        // Tab列表设置
        config.set("tab.enabled", true)
        config.set("tab.update-interval", 100)
        config.set("tab.header", listOf(
                "&6&lFandTpaPlus",
                "&e欢迎来到服务器！"
        ))
        config.set("tab.footer", listOf(
                "&7在线玩家: &f%online%/%max%",
                "&7TPS: &f%tps%"
        ))
        config.set("tab.player-format", "%title% &f%player%")

        // 更新检查器设置
        config.set("update-checker.enabled", true)
        config.set("update-checker.auto-update", false)
        config.set("update-checker.check-interval", 3600)
        config.set("update-checker.notify-ops", true)

        // 性能设置
        config.set("performance.async-teleport", true)
        config.set("performance.cache-duration", 300)
        config.set("performance.max-concurrent-operations", 10)
    }

    fun saveConfig() {
        CompletableFuture.runAsync {
            try {
                config.save(File(plugin.dataFolder, "config.yml"))
            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, "保存配置文件失败", e)
            }
        }
    }

    fun reload() {
        loadConfig()
    }

    fun getString(path: String, default: String = ""): String {
        return config.getString(path, default) ?: default
    }

    fun getInt(path: String, default: Int = 0): Int {
        return config.getInt(path, default)
    }

    fun getBoolean(path: String, default: Boolean = false): Boolean {
        return config.getBoolean(path, default)
    }

    fun getDouble(path: String, default: Double = 0.0): Double {
        return config.getDouble(path, default)
    }

    fun getStringList(path: String): List<String> {
        return config.getStringList(path)
    }

    fun set(path: String, value: Any?) {
        config.set(path, value)
    }
}