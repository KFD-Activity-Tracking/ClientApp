package network

import kotlinx.serialization.Serializable

@Serializable
data class ActionDto(
    val type: String,
    val performedAt: String,
    val delta_x: Float? = null,
    val delta_y: Float? = null,
    val is_click: Boolean? = null,
    val keyboard_key: Int? = null,
    val app_name: String? = null
)