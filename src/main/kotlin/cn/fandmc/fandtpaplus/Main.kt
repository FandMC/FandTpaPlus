package cn.fandmc.fandtpaplus

import cn.fandmc.fandtpaplus.commands.*
import cn.fandmc.fandtpaplus.config.ConfigManager
import cn.fandmc.fandtpaplus.config.LanguageManager
import cn.fandmc.fandtpaplus.listeners.PlayerListener
import cn.fandmc.fandtpaplus.managers.*
import cn.fandmc.fandtpaplus.utils.UpdateChecker
import cn.fandmc.fandtpaplus.utils.FoliaScheduler
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.CompletableFuture
import java.util.logging.Level

class Main : JavaPlugin() {
    companion object {
        lateinit var instance: Main
            private set

        var isFolia: Boolean = false
            private set
    }

    lateinit var configManager: ConfigManager
    lateinit var languageManager: LanguageManager
    lateinit var tpaManager: TpaManager
    lateinit var homeManager: HomeManager
    lateinit var backManager: BackManager
    lateinit var titleManager: TitleManager
    lateinit var scoreboardManager: ScoreboardManager
    lateinit var tabManager: TabManager
    lateinit var updateChecker: UpdateChecker
    lateinit var scheduler: FoliaScheduler

    override fun onEnable() {
        instance = this

        // 检测是否为 Folia
        isFolia = try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            true
        } catch (e: ClassNotFoundException) {
            false
        }

        // 异步初始化
        CompletableFuture.runAsync {
            try {
                initializePlugin()
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "插件初始化失败，但会继续运行", e)
            }
        }.exceptionally { throwable ->
            logger.log(Level.SEVERE, "严重错误：${throwable.message}", throwable)
            null
        }
    }

    private fun initializePlugin() {
        // 保存默认配置
        saveDefaultConfig()

        // 初始化调度器
        scheduler = FoliaScheduler(this)

        // 初始化管理器
        configManager = ConfigManager(this)
        languageManager = LanguageManager(this)

        // 初始化功能管理器
        tpaManager = TpaManager(this)
        homeManager = HomeManager(this)
        backManager = BackManager(this)
        titleManager = TitleManager(this)
        scoreboardManager = ScoreboardManager(this)
        tabManager = TabManager(this)

        // 注册命令
        registerCommands()

        // 注册监听器
        server.pluginManager.registerEvents(PlayerListener(this), this)

        // 初始化更新检查器
        updateChecker = UpdateChecker(this)

        // 延迟检查更新
        scheduler.runTaskLaterAsynchronously({
            checkForUpdates()
        }, 60L) // 3秒后检查

        logger.info("FandTpaPlus v${description.version} 已成功启用！")
        logger.info("运行模式: ${if (isFolia) "Folia" else "Paper/Spigot"}")
    }

    private fun registerCommands() {
        getCommand("tpa")?.setExecutor(TpaCommand(this))
        getCommand("tpahere")?.setExecutor(TpaHereCommand(this))
        getCommand("tpaccept")?.setExecutor(TpAcceptCommand(this))
        getCommand("tpdeny")?.setExecutor(TpDenyCommand(this))
        getCommand("tpcancel")?.setExecutor(TpCancelCommand(this))
        getCommand("home")?.setExecutor(HomeCommand(this))
        getCommand("sethome")?.setExecutor(SetHomeCommand(this))
        getCommand("delhome")?.setExecutor(DelHomeCommand(this))
        getCommand("back")?.setExecutor(BackCommand(this))
        getCommand("title")?.setExecutor(TitleCommand(this))
        getCommand("fandtpaplus")?.setExecutor(MainCommand(this))
    }

    private fun checkForUpdates() {
        if (configManager.config.getBoolean("update-checker.enabled", true)) {
            updateChecker.checkForUpdate { hasUpdate, latestVersion ->
                if (hasUpdate) {
                    logger.info("发现新版本: $latestVersion")
                    logger.info("当前版本: ${description.version}")

                    if (configManager.config.getBoolean("update-checker.auto-update", false)) {
                        updateChecker.downloadAndUpdate(latestVersion)
                    }
                }
            }
        }
    }

    override fun onDisable() {
        try {
            // 保存所有数据
            saveAllData()

            // 关闭调度器
            if (::scheduler.isInitialized) {
                scheduler.cancelAllTasks()
            }

            logger.info("FandTpaPlus 已成功关闭！")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "插件关闭时出错", e)
        }
    }

    private fun saveAllData() {
        CompletableFuture.allOf(
            CompletableFuture.runAsync { homeManager.saveData() },
            CompletableFuture.runAsync { titleManager.saveData() },
            CompletableFuture.runAsync { configManager.saveConfig() }
        ).join()
    }

    fun reload() {
        CompletableFuture.runAsync {
            try {
                // 重载配置
                reloadConfig()
                configManager.reload()
                languageManager.reload()

                // 重载各个管理器
                tpaManager.reload()
                homeManager.reload()
                titleManager.reload()
                scoreboardManager.reload()
                tabManager.reload()

                logger.info("插件重载完成！")
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "重载插件时出错", e)
            }
        }
    }
}