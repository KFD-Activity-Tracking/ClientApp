package database

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun init(reset: Boolean = false) {
    Database.connect("jdbc:sqlite:activity.db", driver = "org.sqlite.JDBC")
    transaction {
        if (reset) {
            SchemaUtils.drop(AppActionTable, KeyboardActionTable, MouseActionTable, ActionTable)
        }
        SchemaUtils.createMissingTablesAndColumns(ActionTable, MouseActionTable, KeyboardActionTable, AppActionTable)
    }
}