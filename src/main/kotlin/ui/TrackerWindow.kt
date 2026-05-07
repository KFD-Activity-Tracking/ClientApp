package ui

import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class TrackerWindow(
    private val onStart: (username: String, password: String) -> Unit,
    private val onStop: () -> Unit,
) {
    private val frame = JFrame("KFD Activity Tracker")

    private val statusDot   = JLabel("●")
    private val statusLabel = JLabel("Не подключено")
    private val logArea     = JTextArea(6, 40).apply {
        isEditable = false
        font = Font(Font.MONOSPACED, Font.PLAIN, 11)
        background = Color(30, 30, 30)
        foreground = Color(180, 180, 180)
        lineWrap = true
    }

    private val usernameField = JTextField("user", 18)
    private val passwordField = JPasswordField("user", 18)
    private val startBtn      = JButton("Начать сессию")
    private val stopBtn       = JButton("Остановить").apply { isEnabled = false }

    init {
        applyDarkTheme()
        buildUi()
        frame.apply {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            pack()
            setLocationRelativeTo(null)
            isVisible = true
        }
    }

    private fun buildUi() {
        val root = JPanel(BorderLayout(0, 12)).apply {
            border = EmptyBorder(20, 24, 20, 24)
            background = Color(22, 27, 34)
        }

        // ── Header ──
        val header = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
            background = Color(22, 27, 34)
            statusDot.font = Font(Font.DIALOG, Font.BOLD, 14)
            statusDot.foreground = Color(100, 100, 100)
            val title = JLabel("KFD Tracker").apply {
                font = Font(Font.DIALOG, Font.BOLD, 15)
                foreground = Color(230, 237, 243)
            }
            add(title)
            add(statusDot)
            add(statusLabel.apply {
                font = Font(Font.DIALOG, Font.PLAIN, 12)
                foreground = Color(125, 143, 168)
            })
        }

        // ── Login form ──
        val form = JPanel(GridBagLayout()).apply {
            background = Color(22, 27, 34)
            val gbc = GridBagConstraints().apply {
                insets = Insets(4, 4, 4, 4)
                fill = GridBagConstraints.HORIZONTAL
            }
            fun label(text: String) = JLabel(text).apply {
                foreground = Color(125, 143, 168)
                font = Font(Font.DIALOG, Font.PLAIN, 12)
            }
            styleField(usernameField)
            styleField(passwordField)

            gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; add(label("Логин:"), gbc)
            gbc.gridx = 1; gbc.weightx = 1.0; add(usernameField, gbc)
            gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0; add(label("Пароль:"), gbc)
            gbc.gridx = 1; gbc.weightx = 1.0; add(passwordField, gbc)
        }

        // ── Buttons ──
        styleButton(startBtn, Color(63, 185, 80))
        styleButton(stopBtn, Color(232, 74, 111))

        val btnPanel = JPanel(GridLayout(1, 2, 8, 0)).apply {
            background = Color(22, 27, 34)
            add(startBtn)
            add(stopBtn)
        }

        startBtn.addActionListener {
            startBtn.isEnabled = false
            val u = usernameField.text.trim()
            val p = String(passwordField.password)
            onStart(u, p)
        }
        stopBtn.addActionListener {
            onStop()
            stopBtn.isEnabled = false
            startBtn.isEnabled = true
            setStatus(false, "Остановлено")
        }

        // ── Log ──
        val logScroll = JScrollPane(logArea).apply {
            border = BorderFactory.createLineBorder(Color(42, 52, 71))
            preferredSize = Dimension(440, 110)
        }

        root.add(header, BorderLayout.NORTH)
        val center = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(22, 27, 34)
            add(form)
            add(Box.createVerticalStrut(8))
            add(btnPanel)
            add(Box.createVerticalStrut(10))
            add(logScroll)
        }
        root.add(center, BorderLayout.CENTER)
        frame.contentPane = root
    }

    fun setStatus(connected: Boolean, text: String) {
        SwingUtilities.invokeLater {
            statusDot.foreground = if (connected) Color(63, 185, 80) else Color(232, 74, 111)
            statusLabel.text = text
        }
    }

    fun setTracking(active: Boolean) {
        SwingUtilities.invokeLater {
            stopBtn.isEnabled = active
            startBtn.isEnabled = !active
            if (active) setStatus(true, "Отслеживание...")
        }
    }

    fun log(msg: String) {
        SwingUtilities.invokeLater {
            logArea.append("$msg\n")
            logArea.caretPosition = logArea.document.length
        }
    }

    private fun styleField(f: JTextField) {
        f.background = Color(13, 17, 23)
        f.foreground = Color(230, 237, 243)
        f.caretColor = Color(230, 237, 243)
        f.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color(42, 52, 71)),
            EmptyBorder(4, 8, 4, 8)
        )
        f.font = Font(Font.DIALOG, Font.PLAIN, 13)
    }

    private fun styleButton(btn: JButton, color: Color) {
        btn.background = Color(color.red, color.green, color.blue, 30)
        btn.foreground = color
        btn.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color(color.red, color.green, color.blue, 80)),
            EmptyBorder(7, 14, 7, 14)
        )
        btn.font = Font(Font.DIALOG, Font.BOLD, 13)
        btn.isFocusPainted = false
        btn.cursor = Cursor(Cursor.HAND_CURSOR)
    }

    private fun applyDarkTheme() {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }
}