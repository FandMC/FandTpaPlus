package cn.fandmc.fandtpaplus.utils

import cn.fandmc.fandtpaplus.Main
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class FoliaScheduler(private val plugin: Plugin) {
    private val tasks = ConcurrentHashMap<Int, Any>()
    private var taskIdCounter = 0

    fun runTask(runnable: Runnable): Any {
        return if (Main.isFolia) {
            Bukkit.getGlobalRegionScheduler().run(plugin) { runnable.run() }
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable)
        }
    }

    fun runTaskAsynchronously(runnable: Runnable): Any {
        return if (Main.isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin) { runnable.run() }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable)
        }
    }

    fun runTaskLater(runnable: Runnable, delay: Long): Any {
        return if (Main.isFolia) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { runnable.run() }, delay)
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delay)
        }
    }

    fun runTaskLaterAsynchronously(runnable: Runnable, delay: Long): Any {
        return if (Main.isFolia) {
            Bukkit.getAsyncScheduler().runDelayed(plugin, { runnable.run() }, delay * 50, TimeUnit.MILLISECONDS)
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay)
        }
    }

    fun runTaskTimer(runnable: Runnable, delay: Long, period: Long): Any {
        val taskId = taskIdCounter++
        val task = if (Main.isFolia) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { runnable.run() }, delay, period)
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period)
        }
        tasks[taskId] = task
        return task
    }

    fun runTaskTimerAsynchronously(runnable: Runnable, delay: Long, period: Long): Any {
        val taskId = taskIdCounter++
        val task = if (Main.isFolia) {
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { runnable.run() },
                delay * 50, period * 50, TimeUnit.MILLISECONDS)
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period)
        }
        tasks[taskId] = task
        return task
    }

    fun runAtEntity(entity: Entity, runnable: Runnable): Any? {
        return if (Main.isFolia) {
            entity.scheduler.run(plugin, { runnable.run() }, null)
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable)
        }
    }

    fun runAtEntityLater(entity: Entity, runnable: Runnable, delay: Long): Any? {
        return if (Main.isFolia) {
            entity.scheduler.runDelayed(plugin, { runnable.run() }, null, delay)
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delay)
        }
    }

    fun runAtLocation(location: Location, runnable: Runnable): Any {
        return if (Main.isFolia) {
            Bukkit.getRegionScheduler().run(plugin, location) { runnable.run() }
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable)
        }
    }

    fun cancelTask(task: Any) {
        if (Main.isFolia) {
            when (task) {
                is io.papermc.paper.threadedregions.scheduler.ScheduledTask -> task.cancel()
            }
        } else {
            when (task) {
                is org.bukkit.scheduler.BukkitTask -> task.cancel()
                is Int -> Bukkit.getScheduler().cancelTask(task)
            }
        }
    }

    fun cancelAllTasks() {
        tasks.values.forEach { cancelTask(it) }
        tasks.clear()

        if (!Main.isFolia) {
            Bukkit.getScheduler().cancelTasks(plugin)
        }
    }
}