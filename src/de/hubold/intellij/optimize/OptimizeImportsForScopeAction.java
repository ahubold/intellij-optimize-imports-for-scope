package de.hubold.intellij.optimize;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.actions.AbstractLayoutCodeProcessor;
import com.intellij.codeInsight.actions.OptimizeImportsProcessor;
import com.intellij.find.impl.FindInProjectUtil;
import com.intellij.lang.LanguageImportStatements;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * An action to optimize imports across the whole project with the possibility
 * to restrict the files by scope and/or file mask.
 */
public class OptimizeImportsForScopeAction extends AnAction {

  private static final Logger LOG = Logger.getInstance(OptimizeImportsForScopeAction.class);

  @Override
  public void actionPerformed(AnActionEvent event) {
    Project project = getEventProject(event);
    if (project == null) {
      return;
    }
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    OptimizeImportsOptions options = getOptimizeImportsOptions(project);
    if (options != null) {
      optimizeImports(project, options);
    }
  }

  @Override
  public void update(AnActionEvent event){
    if (!LanguageImportStatements.INSTANCE.hasAnyExtensions()) {
      event.getPresentation().setVisible(false);
      return;
    }
    Project project = getEventProject(event);
    event.getPresentation().setEnabled(project != null);
  }

  @Nullable
  private static OptimizeImportsOptions getOptimizeImportsOptions(@NotNull Project project) {
    String title = CodeInsightBundle.message("process.optimize.imports");
    String text = CodeInsightBundle.message("process.scope.project", project.getPresentableUrl());
    OptimizeImportsDialog dialog = new OptimizeImportsDialog(project, title, text);
    return dialog.showAndGet() ? dialog : null;
  }

  private static void optimizeImports(Project project, OptimizeImportsOptions selectedFlags) {
    OptimizeImportsProcessor processor = new OptimizeImportsProcessor(project);
    registerScopeFilter(processor, selectedFlags.getSearchScope());
    registerFileMaskFilter(processor, selectedFlags.getFileTypeMask());
    processor.run();
  }

  public static void registerScopeFilter(@NotNull AbstractLayoutCodeProcessor processor, @Nullable final SearchScope scope) {
    if (scope == null) {
      return;
    }

    processor.addFileFilter(new VirtualFileFilter() {
      @Override
      public boolean accept(@NotNull VirtualFile file) {
        return scope instanceof LocalSearchScope
          ? ((LocalSearchScope)scope).isInScope(file)
          : scope instanceof GlobalSearchScope && ((GlobalSearchScope)scope).contains(file);

      }
    });
  }

  public static void registerFileMaskFilter(@NotNull AbstractLayoutCodeProcessor processor, @Nullable String fileTypeMask) {
    if (fileTypeMask == null)
      return;

    final Pattern pattern = getFileTypeMaskPattern(fileTypeMask);
    if (pattern != null) {
      processor.addFileFilter(new VirtualFileFilter() {
        @Override
        public boolean accept(@NotNull VirtualFile file) {
          return pattern.matcher(file.getName()).matches();
        }
      });
    }
  }

  @Nullable
  private static Pattern getFileTypeMaskPattern(@Nullable String mask) {
    try {
      return FindInProjectUtil.createFileMaskRegExp(mask);
    } catch (PatternSyntaxException e) {
      LOG.info("Error while processing file mask: ", e);
      return null;
    }
  }

}
