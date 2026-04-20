package database

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

object ActionTable : Table("Actions") {
    val id = long("id").autoIncrement()
    override val primaryKey = PrimaryKey(id)
    val performedAt = long("time")
    val type = varchar("type", 50)
}