package cn.fandmc.fandtpaplus.config

import cn.fandmc.fandtpaplus.Main
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

class LanguageManager(private val plugin: Main) {
    private val messages = ConcurrentHashMap<String, String>()
    private val miniMessage = MiniMessage.miniMessage()
    private val legacySerializer = LegacyComponentSerializer.legacyAmpersand()

    private var currentLanguage = "zh_CN"

    init {
        loadLanguage()
    }

    private fun loadLanguage() {
        currentLanguage = plugin.configManager.getString("language", "zh_CN")

        try {
            val langFile = File(plugin.dataFolder, "languages/$currentLanguage.yml")

            if (!langFile.exists()) {
                saveDefaultLanguages()
            }

            val langConfig = if (langFile.exists()) {
                YamlConfiguration.loadConfiguration(langFile)
            } else {
                loadDefaultLanguage()
            }

            // 加载所有消息
            langConfig.getKeys(true).forEach { key ->
                langConfig.getString(key)?.let { value ->
                    messages[key] = value
                }
            }

            plugin.logger.info("成功加载语言: $currentLanguage")
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "加载语言文件失败", e)
            loadDefaultMessages()
        }
    }

    private fun saveDefaultLanguages() {
        val langDir = File(plugin.dataFolder, "languages")
        if (!langDir.exists()) {
            langDir.mkdirs()
        }

        // 保存中文语言文件
        saveLanguageFile("zh_CN.yml")

        // 保存英文语言文件
        saveLanguageFile("en_US.yml")
    }

    private fun saveLanguageFile(fileName: String) {
        try {
            val file = File(plugin.dataFolder, "languages/$fileName")
            if (!file.exists()) {
                // 创建目录
                file.parentFile.mkdirs()

                // 从资源中读取并写入文件（语言文件直接在resources根目录）
                plugin.getResource(fileName)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: run {
                    plugin.logger.warning("找不到资源文件: $fileName，创建默认文件")
                    // 创建默认文件
                    val config = YamlConfiguration()
                    if (fileName == "zh_CN.yml") {
                        loadDefaultMessages()
                        messages.forEach { (key, value) ->
                            config.set(key, value)
                        }
                    } else {
                        // 创建英文版本
                        loadDefaultEnglishMessages()
                        messages.forEach { (key, value) ->
                            config.set(key, value)
                        }
                    }
                    config.save(file)
                }
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "保存语言文件失败: $fileName", e)
        }
    }

    private fun loadDefaultLanguage(): YamlConfiguration {
        val config = YamlConfiguration()

        // 如果无法从资源加载，使用内置的默认消息
        loadDefaultMessages()
        messages.forEach { (key, value) ->
            config.set(key, value)
        }

        return config
    }

    private fun loadDefaultMessages() {
        // 命令消息
        messages["command.no-permission"] = "&c你没有权限使用此命令！"
        messages["command.player-only"] = "&c此命令只能由玩家使用！"
        messages["command.player-not-found"] = "&c找不到玩家 &e%player%&c！"
        messages["command.invalid-usage"] = "&c用法错误！正确用法: &e%usage%"

        // TPA消息
        messages["tpa.request-sent"] = "&a已向 &e%player% &a发送传送请求！"
        messages["tpa.request-received"] = "&e%player% &a请求传送到你这里！"
        messages["tpa.request-received-here"] = "&e%player% &a请求你传送到他那里！"
        messages["tpa.request-accept-hint"] = "&a输入 &e/tpaccept &a接受，&e/tpdeny &a拒绝"
        messages["tpa.request-timeout"] = "&c传送请求已超时！"
        messages["tpa.request-accepted"] = "&a传送请求已接受！"
        messages["tpa.request-denied"] = "&c传送请求已拒绝！"
        messages["tpa.request-cancelled"] = "&c传送请求已取消！"
        messages["tpa.no-pending-request"] = "&c你没有待处理的传送请求！"
        messages["tpa.already-pending"] = "&c你已经向该玩家发送了传送请求！"
        messages["tpa.cannot-tpa-self"] = "&c你不能传送到自己！"
        messages["tpa.cooldown"] = "&c请等待 &e%time% &c秒后再使用此命令！"
        messages["tpa.warmup"] = "&a传送将在 &e%time% &a秒后开始，请不要移动！"
        messages["tpa.warmup-cancelled"] = "&c传送已取消，因为你移动了！"
        messages["tpa.warmup-cancelled-damage"] = "&c传送已取消，因为你受到了伤害！"
        messages["tpa.cross-world-disabled"] = "&c不允许跨世界传送！"

        // Home消息
        messages["home.set"] = "&a成功设置家 &e%home%&a！"
        messages["home.deleted"] = "&a成功删除家 &e%home%&a！"
        messages["home.not-found"] = "&c找不到名为 &e%home% &c的家！"
        messages["home.list"] = "&a你的家: &e%homes%"
        messages["home.list-empty"] = "&c你还没有设置任何家！"
        messages["home.limit-reached"] = "&c你已达到家的数量上限 (&e%limit%&c)！"
        messages["home.teleporting"] = "&a正在传送到家 &e%home%&a..."
        messages["home.teleport-cancelled"] = "&c传送已取消！"
        messages["home.invalid-name"] = "&c家的名称只能包含字母、数字和下划线！"

        // Back消息
        messages["back.no-location"] = "&c没有可返回的位置！"
        messages["back.returning"] = "&a正在返回上一个位置..."
        messages["back.saved"] = "&a已保存返回点！"
        messages["back.cooldown"] = "&c请等待 &e%time% &c秒后再使用返回功能！"

        // 称号消息
        messages["title.set"] = "&a成功设置称号为: %title%"
        messages["title.removed"] = "&a成功移除称号！"
        messages["title.invalid-length"] = "&c称号长度不能超过 &e%max% &c个字符！"
        messages["title.blocked-word"] = "&c称号包含被禁用的词汇！"
        messages["title.list"] = "&a可用称号: &e%titles%"
        messages["title.no-permission-title"] = "&c你没有权限使用这个称号！"

        // 主命令消息
        messages["plugin.reload"] = "&a插件已重新加载！"
        messages["plugin.version"] = "&a当前版本: &e%version%"
        messages["plugin.help.header"] = "&6&m--------&r &e&lFandTpaPlus 帮助 &6&m--------"
        messages["plugin.help.tpa"] = "&e/tpa <玩家> &7- 请求传送到玩家"
        messages["plugin.help.tpahere"] = "&e/tpahere <玩家> &7- 请求玩家传送到你"
        messages["plugin.help.home"] = "&e/home [家名] &7- 传送到家"
        messages["plugin.help.sethome"] = "&e/sethome <家名> &7- 设置家"
        messages["plugin.help.back"] = "&e/back &7- 返回上一个位置"
        messages["plugin.help.title"] = "&e/title <称号|remove> &7- 设置或移除称号"
        messages["plugin.help.footer"] = "&6&m-----------------------------"

        // 更新消息
        messages["update.checking"] = "&a正在检查更新..."
        messages["update.available"] = "&a发现新版本: &e%version%"
        messages["update.downloading"] = "&a正在下载更新..."
        messages["update.success"] = "&a更新下载成功！重启服务器以应用更新。"
        messages["update.failed"] = "&c更新失败: %error%"
        messages["update.up-to-date"] = "&a你正在使用最新版本！"
    }

    fun getMessage(key: String, vararg placeholders: Pair<String, Any>): String {
        var message = messages[key] ?: run {
            plugin.logger.warning("找不到语言键: $key")
            return key
        }

        // 替换占位符
        placeholders.forEach { (placeholder, value) ->
            message = message.replace("%$placeholder%", value.toString())
        }

        return message
    }

    fun getComponent(key: String, vararg placeholders: Pair<String, Any>): Component {
        val message = getMessage(key, *placeholders)
        return legacySerializer.deserialize(message)
    }

    fun sendMessage(sender: CommandSender, key: String, vararg placeholders: Pair<String, Any>) {
        val component = getComponent(key, *placeholders)
        sender.sendMessage(component)
    }

    fun reload() {
        messages.clear()
        loadLanguage()
    }

    fun setLanguage(language: String) {
        currentLanguage = language
        plugin.configManager.set("language", language)
        plugin.configManager.saveConfig()
        reload()
    }

    fun getAvailableLanguages(): List<String> {
        val langDir = File(plugin.dataFolder, "languages")
        return if (langDir.exists() && langDir.isDirectory) {
            langDir.listFiles { file -> file.extension == "yml" }
                ?.map { it.nameWithoutExtension }
                ?: emptyList()
        } else {
            listOf("zh_CN", "en_US")
        }
    }

    private fun loadDefaultEnglishMessages() {
        messages.clear()

        // Command messages
        messages["command.no-permission"] = "&cYou don't have permission to use this command!"
        messages["command.player-only"] = "&cThis command can only be used by players!"
        messages["command.player-not-found"] = "&cPlayer &e%player% &cnot found!"
        messages["command.invalid-usage"] = "&cInvalid usage! Correct usage: &e%usage%"

        // TPA messages
        messages["tpa.request-sent"] = "&aTeleport request sent to &e%player%&a!"
        messages["tpa.request-received"] = "&e%player% &awants to teleport to you!"
        messages["tpa.request-received-here"] = "&e%player% &awants you to teleport to them!"
        messages["tpa.request-accept-hint"] = "&aType &e/tpaccept &ato accept, &e/tpdeny &ato deny"
        messages["tpa.request-timeout"] = "&cTeleport request timed out!"
        messages["tpa.request-accepted"] = "&aTeleport request accepted!"
        messages["tpa.request-denied"] = "&cTeleport request denied!"
        messages["tpa.request-cancelled"] = "&cTeleport request cancelled!"
        messages["tpa.no-pending-request"] = "&cYou have no pending teleport requests!"
        messages["tpa.already-pending"] = "&cYou already have a pending request to this player!"
        messages["tpa.cannot-tpa-self"] = "&cYou cannot teleport to yourself!"
        messages["tpa.cooldown"] = "&cPlease wait &e%time% &cseconds before using this command again!"
        messages["tpa.warmup"] = "&aTeleporting in &e%time% &aseconds, don't move!"
        messages["tpa.warmup-cancelled"] = "&cTeleport cancelled because you moved!"
        messages["tpa.warmup-cancelled-damage"] = "&cTeleport cancelled because you took damage!"
        messages["tpa.cross-world-disabled"] = "&cCross-world teleportation is disabled!"

        // Home messages
        messages["home.set"] = "&aHome &e%home% &aset successfully!"
        messages["home.deleted"] = "&aHome &e%home% &adeleted successfully!"
        messages["home.not-found"] = "&cHome &e%home% &cnot found!"
        messages["home.list"] = "&aYour homes: &e%homes%"
        messages["home.list-empty"] = "&cYou haven't set any homes yet!"
        messages["home.limit-reached"] = "&cYou've reached your home limit (&e%limit%&c)!"
        messages["home.teleporting"] = "&aTeleporting to home &e%home%&a..."
        messages["home.teleport-cancelled"] = "&cTeleport cancelled!"
        messages["home.invalid-name"] = "&cHome names can only contain letters, numbers, and underscores!"

        // Back messages
        messages["back.no-location"] = "&cNo location to return to!"
        messages["back.returning"] = "&aReturning to previous location..."
        messages["back.saved"] = "&aReturn point saved!"
        messages["back.cooldown"] = "&cPlease wait &e%time% &cseconds before using back again!"

        // Title messages
        messages["title.set"] = "&aTitle set to: %title%"
        messages["title.removed"] = "&aTitle removed successfully!"
        messages["title.invalid-length"] = "&cTitle cannot exceed &e%max% &ccharacters!"
        messages["title.blocked-word"] = "&cTitle contains blocked words!"
        messages["title.list"] = "&aAvailable titles: &e%titles%"
        messages["title.no-permission-title"] = "&cYou don't have permission to use this title!"

        // Plugin messages
        messages["plugin.reload"] = "&aPlugin reloaded successfully!"
        messages["plugin.version"] = "&aCurrent version: &e%version%"

        // Update messages
        messages["update.checking"] = "&aChecking for updates..."
        messages["update.available"] = "&aNew version available: &e%version%"
        messages["update.up-to-date"] = "&aYou're using the latest version!"
    }
}