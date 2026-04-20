package database

import org.jetbrains.exposed.v1.core.Table

object KeyboardActionTable : Table("KeyboardActions"){
    val id = long("id").references(ActionTable.id)
    val keyboardKey = integer("Keyboard_code")
}