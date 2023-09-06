package lk.ijse.javafx.plugin.action;

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class GenerateControllerAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        Editor editor = e.getDataContext().getData(CommonDataKeys.EDITOR);
        if (psiFile != null && editor != null) {
            try {
                createEventHandlers(e.getProject(), editor, psiFile);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            createIds(e.getProject(), editor, psiFile);
        }
    }

    private void createEventHandlers(Project project, Editor editor, PsiFile file) throws InterruptedException {
        List<HighlightInfo> highlights = DaemonCodeAnalyzerImpl.getHighlights(editor.getDocument(), HighlightSeverity.ERROR, project);
        for (HighlightInfo highlight : highlights) {
            if (highlight.getDescription().startsWith("Cannot resolve class")) {
                highlight.quickFixActionRanges.get(0).getFirst().getAction().invoke(project, editor, file);
            } else {
                if (highlight.quickFixActionRanges == null) continue;
                CommandProcessor.getInstance().executeCommand(project, () -> {
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        highlight.quickFixActionRanges.get(0).getFirst().getAction().invoke(project, editor, file);
                    });
                }, highlight.getText(), null);
            }
        }
    }

    private void createIds(Project project, Editor editor, PsiFile file) {
        List<HighlightInfo> highlights = DaemonCodeAnalyzerImpl.getHighlights(editor.getDocument(), HighlightSeverity.WARNING, project).stream().filter(h -> h.getDescription().equals("Unresolved fx:id reference")).collect(Collectors.toList());
        for (HighlightInfo highlight : highlights) {
            CommandProcessor.getInstance().executeCommand(project, () -> {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    highlight.quickFixActionRanges.get(0).getFirst().getAction().invoke(project, editor, file);
                });
            }, highlight.getText(), null);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        e.getPresentation().setVisible(file != null && file.getExtension() != null && file.getExtension().equalsIgnoreCase("fxml"));
    }
}
