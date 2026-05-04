package database

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun init(){
    Database.connect("jdbc:sqlite:activity.db", driver = "org.sqlite.JDBC")
    transaction {
        SchemaUtils.createMissingTablesAndColumns(ActionTable, MouseActionTable, KeyboardActionTable, AppActionTable)
    }
}