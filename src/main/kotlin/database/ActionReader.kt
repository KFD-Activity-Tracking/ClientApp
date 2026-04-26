package database

import network.ActionDto
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class PendingAction(val id: Long, val dto: ActionDto)

private fun formatMs(ms: Long): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault()).toString()

fun readPendingActions(): List<PendingAction> = transaction {
    val keyboard = (ActionTable innerJoin KeyboardActionTable).selectAll().map { row ->
        PendingAction(
            id = row[ActionTable.id],
            dto = ActionDto(
                type = "keyboard",
                performedAt = formatMs(row[ActionTable.performedAt]),
                keyboard_key = row[KeyboardActionTable.keyboardKey]
            )
        )
    }

    val mouseMoves = (ActionTable innerJoin MouseActionTable)
        .selectAll()
        .where { MouseActionTable.deltaX.isNotNull() }
        .map { row ->
            PendingAction(
                id = row[ActionTable.id],
                dto = ActionDto(
                    type = "mouse",
                    performedAt = formatMs(row[ActionTable.performedAt]),
                    delta_x = row[MouseActionTable.deltaX]!!,
                    delta_y = row[MouseActionTable.deltaY]!!
                )
            )
        }

    val apps = (ActionTable innerJoin AppActionTable).selectAll().map { row ->
        PendingAction(
            id = row[ActionTable.id],
            dto = ActionDto(
                type = "app",
                performedAt = formatMs(row[ActionTable.performedAt]),
                app_name = row[AppActionTable.appName]
            )
        )
    }

    keyboard + mouseMoves + apps
}

fun deleteSentActions(ids: List<Long>) = transaction {
    KeyboardActionTable.deleteWhere { id inList ids }
    MouseActionTable.deleteWhere { id inList ids }
    AppActionTable.deleteWhere { id inList ids }
    ActionTable.deleteWhere { ActionTable.id inList ids }
}