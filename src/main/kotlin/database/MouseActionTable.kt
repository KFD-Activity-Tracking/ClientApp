package database

import org.jetbrains.exposed.v1.core.Table

object MouseActionTable : Table ("MouseActions"){
    val id = long("id").references(ActionTable.id)
    val deltaX = float("delta_x").nullable()
    val deltaY = float("delta_y").nullable()
    val isClick = bool("is_click")
}