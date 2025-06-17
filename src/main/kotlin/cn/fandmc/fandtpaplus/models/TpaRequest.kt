package cn.fandmc.fandtpaplus.models

import java.util.UUID

enum class TpaType {
    TPA,
    TPAHERE
}

data class TpaRequest(
        val sender: UUID,
        val target: UUID,
        val type: TpaType,
        val createTime: Long,
        val timeout: Long
) {
    fun isExpired(): Boolean {
        return System.currentTimeMillis() - createTime > timeout
    }
}

data class Home(
        val name: String,
        val world: String,
        val x: Double,
        val y: Double,
        val z: Double,
        val yaw: Float,
        val pitch: Float
)