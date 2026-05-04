package database

import model.ActionType
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun saveMouseAction(deltaX: Int, deltaY: Int) {
    transaction {
        val actionId = ActionTable.insert {
            it[performedAt] = System.currentTimeMillis()
            it[type] = ActionType.MOUSE.name
        } get ActionTable.id

        MouseActionTable.insert {
            it[id] = actionId
            it[this.deltaX] = deltaX.toFloat()
            it[this.deltaY] = deltaY.toFloat()
            it[isClick] = false
        }
    }
}

fun saveMouseClickAction(button: Int) {
    transaction {
        val actionId = ActionTable.insert {
            it[performedAt] = System.currentTimeMillis()
            it[type] = ActionType.MOUSE.name
        } get ActionTable.id

        MouseActionTable.insert {
            it[id] = actionId
            it[deltaX] = null
            it[deltaY] = null
            it[isClick] = true
        }
    }
}

fun saveKeyboardAction(keyCode: Int) {
    transaction {
        val actionId = ActionTable.insert {
            it[performedAt] = System.currentTimeMillis()
            it[type] = ActionType.KEYBOARD.name
        } get ActionTable.id

        KeyboardActionTable.insert {
            it[id] = actionId
            it[keyboardKey] = keyCode
        }
    }
}

fun saveAppAction(appName: String) {
    transaction {
        val actionId = ActionTable.insert {
            it[performedAt] = System.currentTimeMillis()
            it[type] = ActionType.APP.name
        } get ActionTable.id

        AppActionTable.insert {
            it[id] = actionId
            it[this.appName] = appName
        }
    }
}
