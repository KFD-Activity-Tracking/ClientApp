package tracker

interface InputTracker {
    fun start()
    fun stop()

    var onKeyPressed: ((keyCode: Int) -> Unit)?
    var onMouseMoved: ((deltaX: Int, deltaY: Int) -> Unit)?
    var onMouseClicked: ((button: Int, x: Float, y: Float) -> Unit)?
    var onAppSwitched: ((appName: String) -> Unit)?
}