package database

import org.jetbrains.exposed.v1.core.Table

object MouseActionTable : Table ("MouseActions"){
    val id = long("id").references(ActionTable.id)
    val deltaX = integer("delta_x")
    val deltaY = integer("delta_y")
}