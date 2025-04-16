package com.bulbul.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiFile
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.openapi.vfs.VirtualFile

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
        val isRoute = dialog.isRoute()
        val capitalized = toPascalCase(moduleName.replaceFirstChar { it.uppercaseChar() })
        val lowerCase = moduleName.lowercase()

        val basePath = "$targetPath/${moduleName.lowercase()}"
        val blocDir = "$basePath/bloc"
        val cubitDir = "$basePath/cubit"
        val viewDir = "$basePath/view"
        var importsDir =
            "import '$targetPath/${moduleName.lowercase()}/view/${moduleName.lowercase()}_page.dart';"
        importsDir = "import '../${importsDir.split("lib/").last()}"

        if (isCubit) {
            File(cubitDir).mkdirs()
        } else {
            File(blocDir).mkdirs()
        }
        File(viewDir).mkdirs()

        // Bloc or Cubit files
        if (isCubit) {
            File("$cubitDir/${moduleName.lowercase()}_cubit.dart").writeText(
                "import 'package:flutter_bloc/flutter_bloc.dart';\n" +
                        "\n" +
                        "import '${lowerCase}_state.dart';\n" +
                        "\n" +
                        "class ${capitalized}Cubit extends Cubit<${capitalized}State> {\n" +
                        "  ${capitalized}Cubit() : super(${capitalized}State().init());\n" +
                        "}"
            )
            File("$cubitDir/${moduleName.lowercase()}_state.dart").writeText(
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
        if (isCubit) {
            File("$viewDir/${moduleName.lowercase()}_page.dart").writeText(
                "import 'package:flutter/material.dart';\n" +
                        "import 'package:flutter_bloc/flutter_bloc.dart';\n" +
                        "\n" +
                        "import '../cubit/${lowerCase}_cubit.dart';\n" +
                        "import '../cubit/${lowerCase}_state.dart';\n" +
                        "\n" +
                        "class ${capitalized}Page extends StatelessWidget {\n" +
                        "  const ${capitalized}Page({super.key});\n" +
                        "\n" +
                        "  @override\n" +
                        "  Widget build(BuildContext context) {\n" +
                        "    return BlocProvider(\n" +
                        "      create: (BuildContext context) => ${capitalized}Cubit(),\n" +
                        "      child: BlocBuilder<${capitalized}Cubit,${capitalized}State>(\n" +
                        "        builder: (context, state) {\n" +
                        "          return _buildPage(context);\n" +
                        "        },\n" +
                        "      ),\n" +
                        "    );\n" +
                        "  }\n" +
                        "\n" +
                        "  Widget _buildPage(BuildContext context) {\n" +
                        "    final cubit = BlocProvider.of<${capitalized}Cubit>(context);\n" +
                        "\n" +
                        "    return Container();\n" +
                        "  }\n" +
                        "}"
            )
        } else {
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
        }

        if (isRoute) {
            updateRoutes(project, moduleName, capitalized, importsDir)
        }

        ApplicationManager.getApplication().invokeLater {
            VirtualFileManager.getInstance().asyncRefresh(null)
        }



        Messages.showInfoMessage("Generated $capitalized module at: $basePath", "Success")
    }

    private fun toPascalCase(input: String): String {
        return input.split('_')
            .joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }
    }

    private fun updateRoutes(
        project: Project,
        moduleName: String,
        capital: String,
        importsDir: String
    ) {
        val routeFile = File(project.basePath, "lib/routes/app_routes.dart")
        val pagesFile = File(project.basePath, "lib/routes/route_helper.dart")

        val routeVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(routeFile)
        val pagesVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(pagesFile)

        routeVirtualFile?.let { file ->
            WriteCommandAction.runWriteCommandAction(project) {
                val document = FileDocumentManager.getInstance().getDocument(file)
                document?.let {
                    val content = it.text
                    if (!content.contains("'$moduleName':")) {
                        val newLine =
                            "static const ${moduleName.uppercase()} = '/${moduleName}';\n// @blocgen-routes"
                        val updated = content.replace("// @blocgen-routes", newLine)
                        it.setText(updated)
                    }
                }
            }
        }

        pagesVirtualFile?.let { file ->
            WriteCommandAction.runWriteCommandAction(project) {
                val document = FileDocumentManager.getInstance().getDocument(file)
                document?.let {
                    val content = it.text
                    if (!content.contains(importsDir)) {
                        val newLine = "$importsDir\n// @blocgen-imports"
                        val updated = content.replace("// @blocgen-imports", newLine)
                        val newCode = """
                            GoRoute(
                                path: Routes.${moduleName.uppercase()},
                                name: Routes.${moduleName.uppercase()},
                                pageBuilder: (context, state) {
                                    return RouteTransition.fadeTransition(
                                        state: state,
                                        context: context,
                                        child: const ${capital}Page(),
                                    );
                                },
                            ),
                            
                            // @blocgen-code
                        """.trimIndent()
                        val updatedCode = updated.replace("// @blocgen-code", newCode)
                        it.setText(updatedCode)
                    }

                }
            }
        }

        // Refresh to show updated files in project view
        VirtualFileManager.getInstance().asyncRefresh(null)
        if (routeVirtualFile != null) {
            reformatFile(project, routeVirtualFile)
        }
        if (pagesVirtualFile != null) {
            reformatFile(project, pagesVirtualFile)
        }
    }

    private fun reformatFile(project: Project, virtualFile: VirtualFile) {
        val psiFile: PsiFile? = PsiManager.getInstance(project).findFile(virtualFile)
        psiFile?.let {
            ReformatCodeProcessor(project, it, null, false).run()
        }
    }

}

