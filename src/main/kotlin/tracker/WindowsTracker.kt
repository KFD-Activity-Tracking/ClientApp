package tracker

class WindowsTracker : DesktopTracker() {

    override fun getActiveApp(): String? {
        return try {
            ProcessBuilder(
                "powershell", "-command",
                "(Get-Process | Where-Object { \$_.MainWindowHandle -ne 0 -and \$_.MainWindowTitle -ne '' } | Select-Object -First 1).Name"
            ).start().inputStream.bufferedReader().readText().trim().takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }
}