package cn.fandmc.fandtpaplus.utils

import cn.fandmc.fandtpaplus.Main
import com.google.gson.JsonParser
import org.bukkit.Bukkit
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture
import java.util.logging.Level

class UpdateChecker(private val plugin: Main) {

    companion object {
        // 使用一个示例URL，你需要替换为你实际的GitHub仓库
        private const val GITHUB_API_URL = "https://api.github.com/repos/FandMC/FandTpaPlus/releases/latest"
        private const val UPDATE_CHECK_INTERVAL = 3600L * 20L // 1小时
        private const val CONNECTION_TIMEOUT = 5000 // 5秒超时
    }

    private var lastCheck: Long = 0
    private var latestVersion: String? = null
    private var downloadUrl: String? = null

    init {
        startUpdateCheckTask()
    }

    private fun startUpdateCheckTask() {
        if (!plugin.configManager.getBoolean("update-checker.enabled", true)) {
            return
        }

        // 延迟首次检查，避免服务器启动时的网络问题
        plugin.scheduler.runTaskTimerAsynchronously({
            checkForUpdate { hasUpdate, version ->
                if (hasUpdate && !notifiedThisSession) {
                    notifyUpdate(version)
                    notifiedThisSession = true
                }
            }
        }, 600L, UPDATE_CHECK_INTERVAL) // 30秒后首次检查
    }

    private var notifiedThisSession = false

    fun checkForUpdate(callback: (Boolean, String) -> Unit) {
        if (!plugin.configManager.getBoolean("update-checker.enabled", true)) {
            return
        }

        if (System.currentTimeMillis() - lastCheck < 60000) {
            // 避免频繁检查
            latestVersion?.let {
                callback(isNewerVersion(it), it)
            } ?: callback(false, plugin.description.version)
            return
        }

        CompletableFuture.runAsync {
            try {
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection()
                connection.addRequestProperty("User-Agent", "FandTpaPlus/${plugin.description.version}")
                connection.connectTimeout = CONNECTION_TIMEOUT
                connection.readTimeout = CONNECTION_TIMEOUT

                BufferedReader(InputStreamReader(connection.getInputStream())).use { reader ->
                    val response = reader.readText()
                    val json = JsonParser.parseString(response).asJsonObject

                    latestVersion = json.get("tag_name").asString.removePrefix("v")
                    downloadUrl = json.getAsJsonArray("assets")
                        ?.firstOrNull()
                        ?.asJsonObject
                        ?.get("browser_download_url")
                        ?.asString

                    lastCheck = System.currentTimeMillis()

                    val hasUpdate = isNewerVersion(latestVersion!!)

                    plugin.scheduler.runTask {
                        callback(hasUpdate, latestVersion!!)
                    }
                }
            } catch (e: Exception) {
                // 静默处理网络错误，只在调试模式下打印
                if (plugin.configManager.getBoolean("debug.enabled", false)) {
                    plugin.logger.log(Level.WARNING, "检查更新失败", e)
                }
                plugin.scheduler.runTask {
                    callback(false, plugin.description.version)
                }
            }
        }
    }

    fun downloadAndUpdate(version: String) {
        if (downloadUrl == null) {
            plugin.logger.warning("无法获取下载链接")
            return
        }

        plugin.languageManager.sendMessage(Bukkit.getConsoleSender(), "update.downloading")

        CompletableFuture.runAsync {
            try {
                val url = URL(downloadUrl)
                val connection = url.openConnection()
                connection.addRequestProperty("User-Agent", "FandTpaPlus/${plugin.description.version}")

                val updateFolder = File(plugin.dataFolder.parentFile.parentFile, "update")
                if (!updateFolder.exists()) {
                    updateFolder.mkdirs()
                }

                val targetFile = File(updateFolder, "FandTpaPlus.jar")

                connection.getInputStream().use { input ->
                    Files.copy(input, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }

                plugin.scheduler.runTask {
                    plugin.languageManager.sendMessage(Bukkit.getConsoleSender(), "update.success")

                    // 通知所有OP
                    if (plugin.configManager.getBoolean("update-checker.notify-ops", true)) {
                        Bukkit.getOnlinePlayers()
                            .filter { it.isOp }
                            .forEach { op ->
                                plugin.languageManager.sendMessage(op, "update.success")
                            }
                    }
                }
            } catch (e: Exception) {
                plugin.logger.log(Level.SEVERE, "下载更新失败", e)
                plugin.scheduler.runTask {
                    plugin.languageManager.sendMessage(
                        Bukkit.getConsoleSender(),
                        "update.failed",
                        "error" to e.message.toString()
                    )
                }
            }
        }
    }

    private fun isNewerVersion(version: String): Boolean {
        val current = parseVersion(plugin.description.version)
        val latest = parseVersion(version)

        for (i in 0 until minOf(current.size, latest.size)) {
            if (latest[i] > current[i]) return true
            if (latest[i] < current[i]) return false
        }

        return latest.size > current.size
    }

    private fun parseVersion(version: String): List<Int> {
        return version.split(".")
            .mapNotNull { it.toIntOrNull() }
    }

    private fun notifyUpdate(version: String) {
        plugin.logger.info("发现新版本: $version (当前版本: ${plugin.description.version})")

        if (plugin.configManager.getBoolean("update-checker.notify-ops", true)) {
            Bukkit.getOnlinePlayers()
                .filter { it.isOp }
                .forEach { op ->
                    plugin.languageManager.sendMessage(op, "update.available", "version" to version)
                }
        }

        if (plugin.configManager.getBoolean("update-checker.auto-update", false)) {
            downloadAndUpdate(version)
        }
    }
}