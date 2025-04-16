package com.bulbul.actions

import com.intellij.openapi.ui.DialogWrapper
import java.awt.Dimension
import javax.swing.*

class BlocModuleDialog : DialogWrapper(true) {
    val nameField = JTextField()
    val useCubitCheckbox = JCheckBox("Use Cubit instead of BLoC")

    init {
        init()
        title = "Flutter BLoC Module Generator"
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        val namePanel = JPanel()
        namePanel.layout = BoxLayout(namePanel, BoxLayout.X_AXIS)
        namePanel.add(JLabel("Module Name: "))
        nameField.preferredSize = Dimension(200, 24)
        namePanel.add(nameField)

        panel.add(namePanel)
        panel.add(Box.createVerticalStrut(10))
        panel.add(useCubitCheckbox)

        return panel
    }

    fun getModuleName(): String = nameField.text.trim()
    fun isCubit(): Boolean = useCubitCheckbox.isSelected
}
