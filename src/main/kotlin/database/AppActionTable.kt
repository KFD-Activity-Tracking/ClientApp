package database

import org.jetbrains.exposed.v1.core.Table

object AppActionTable : Table("AppActions"){
    val id = long("id").references(ActionTable.id)
    val appName = varchar("appName", 255)
}