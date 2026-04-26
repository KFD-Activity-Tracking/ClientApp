package network

import kotlinx.serialization.Serializable

@Serializable
data class ActionDto(
    val type: String,
    val performedAt: String,
    val delta_x: Int? = null,
    val delta_y: Int? = null,
    val keyboard_key: Int? = null,
    val app_name: String? = null
)