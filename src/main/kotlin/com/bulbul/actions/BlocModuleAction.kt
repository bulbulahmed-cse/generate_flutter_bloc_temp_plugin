package com.bulbul.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File

class BlocModuleAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Get target path (use selected dir or ask user)
        val selectedDir = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val targetPath = selectedDir?.path ?: run {
            val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            descriptor.title = "Select Directory for BLoC Module"
            val chosen = FileChooser.chooseFile(descriptor, project, null)
            chosen?.path
        } ?: return

        // Show dialog
        val dialog = BlocModuleDialog()
        if (!dialog.showAndGet()) return  // user cancelled

        val moduleName = dialog.getModuleName()
        val isCubit = dialog.isCubit()
        val capitalized = moduleName.replaceFirstChar { it.uppercaseChar() }
        val lowerCase = moduleName.lowercase()

        val basePath = "$targetPath/${moduleName.lowercase()}"
        val blocDir = "$basePath/bloc"
        val viewDir = "$basePath/view"

        File(blocDir).mkdirs()
        File(viewDir).mkdirs()

        // Bloc or Cubit files
        if (isCubit) {
            File("$blocDir/${moduleName.lowercase()}_cubit.dart").writeText("class ${capitalized}Cubit {}")
            File("$blocDir/${moduleName.lowercase()}_state.dart").writeText("abstract class ${capitalized}State {}")
        } else {
            File("$blocDir/${moduleName.lowercase()}_bloc.dart").writeText(
                "import 'package:flutter_bloc/flutter_bloc.dart';\n" +
                        "\n" +
                        "import '${lowerCase}_event.dart';\n" +
                        "import '${lowerCase}_state.dart';\n" +
                        "class ${capitalized}Bloc extends Bloc<${capitalized}Event, ${capitalized}State> {\n" +
                        "  ${capitalized}Bloc() : super(${capitalized}State().init()) {\n" +
                        "    on<InitEvent>(_init);\n" +
                        "  }\n" +
                        "\n" +
                        "  void _init(${capitalized}Event event, Emitter<${capitalized}State> emit) async {\n" +
                        "    emit(state.copyWith());\n" +
                        "  }\n" +
                        "}"
            )
            File("$blocDir/${moduleName.lowercase()}_event.dart").writeText(
                "abstract class ${capitalized}Event {}\n" +
                        "\n" +
                        "class InitEvent extends ${capitalized}Event {}"
            )
            File("$blocDir/${moduleName.lowercase()}_state.dart").writeText(
                "" +
                        "class ${capitalized}State {\n" +
                        "  ${capitalized}State init() {\n" +
                        "    return ${capitalized}State();\n" +
                        "  }\n" +
                        "\n" +
                        "  ${capitalized}State copyWith() {\n" +
                        "    return ${capitalized}State();\n" +
                        "  }\n" +
                        "}"
            )
        }

        // View
        File("$viewDir/${moduleName.lowercase()}_page.dart").writeText(
            """
        import 'package:flutter/material.dart';
        import 'package:flutter_bloc/flutter_bloc.dart';

        import '../bloc/${lowerCase}_bloc.dart';
        import '../bloc/${lowerCase}_event.dart';
        import '../bloc/${lowerCase}_state.dart';

        class ${capitalized}Page extends StatelessWidget {
            const ${capitalized}Page({super.key});
    
            @override
            Widget build(BuildContext context) {
                return BlocProvider(
                   create: (BuildContext context) => ${capitalized}Bloc()..add(InitEvent()),
                   child: BlocBuilder<${capitalized}Bloc, ${capitalized}State>(
                        builder: (context, state) {
                            return _buildPage(context);
                        },
                    ),
                );
            }
    
            Widget _buildPage(BuildContext context) {
                final bloc = BlocProvider.of<${capitalized}Bloc>(context);
                return Container();
            }
        }
        """.trimIndent()
        )

        ApplicationManager.getApplication().invokeLater {
            VirtualFileManager.getInstance().asyncRefresh(null)
        }

        Messages.showInfoMessage("Generated $capitalized module at: $basePath", "Success")
    }

}

