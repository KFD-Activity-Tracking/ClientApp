package tracker

object InputTrackerFactory {
    fun create(): InputTracker {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("linux") -> LinuxEvdevTracker()
            os.contains("win") -> WindowsTracker()
            os.contains("mac") -> MacTracker()
            else -> WindowsTracker()
        }
    }
}