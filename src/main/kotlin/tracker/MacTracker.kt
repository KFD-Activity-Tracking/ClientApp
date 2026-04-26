package tracker

class MacTracker : DesktopTracker() {

    override fun getActiveApp(): String? {
        return try {
            ProcessBuilder(
                "osascript", "-e",
                "tell application \"System Events\" to get name of first application process whose frontmost is true"
            ).start().inputStream.bufferedReader().readText().trim().takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }
}