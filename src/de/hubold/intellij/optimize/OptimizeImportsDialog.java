package de.hubold.intellij.optimize;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.find.FindSettings;
import com.intellij.find.impl.FindDialog;
import com.intellij.find.impl.FindInProjectUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.scopeChooser.ScopeChooserCombo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.search.SearchScope;
import com.intellij.ui.IdeBorderFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.PatternSyntaxException;

public class OptimizeImportsDialog extends DialogWrapper implements OptimizeImportsOptions {

  private final Project myProject;
  private final String myText;

  private JLabel myTitle;

  private JCheckBox myUseScopeFilteringCb;
  private ScopeChooserCombo myScopeCombo;

  private JCheckBox myEnableFileNameFilterCb;
  private ComboBox myFileFilter;

  private JPanel myWholePanel;
  private JPanel myFiltersPanel;
  private JLabel myMaskWarningLabel;

  public OptimizeImportsDialog(@NotNull Project project,
                               @NotNull String title,
                               @NotNull String text) {
    super(project, false);
    myText = text;
    myProject = project;

    setOKButtonText(CodeInsightBundle.message("reformat.code.accept.button.text"));
    setTitle(title);
    init();
  }

  @Override
  protected JComponent createCenterPanel() {
    myTitle.setText(myText);
    myFiltersPanel.setBorder(IdeBorderFactory.createTitledBorder(CodeInsightBundle.message("reformat.directory.dialog.filters")));

    myMaskWarningLabel.setIcon(AllIcons.General.Warning);
    myMaskWarningLabel.setVisible(false);

    initFileTypeFilter();
    initScopeFilter();

    return myWholePanel;
  }

  private void initScopeFilter() {
    myUseScopeFilteringCb.setSelected(false);
    myScopeCombo.setEnabled(false);
    myUseScopeFilteringCb.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myScopeCombo.setEnabled(myUseScopeFilteringCb.isSelected());
      }
    });
  }

  private void initFileTypeFilter() {
    FindDialog.initFileFilter(myFileFilter, myEnableFileNameFilterCb);
    myEnableFileNameFilterCb.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateMaskWarning();
      }
    });
    myFileFilter.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        updateMaskWarning();
      }
    });
  }

  private void updateMaskWarning() {
    if (myEnableFileNameFilterCb.isSelected()) {
      String mask = (String)myFileFilter.getEditor().getItem();
      if (mask == null || !isMaskValid(mask)) {
        showWarningAndDisableOK();
        return;
      }
    }

    if (myMaskWarningLabel.isVisible()) {
      clearWarningAndEnableOK();
    }
  }

  private void showWarningAndDisableOK() {
    myMaskWarningLabel.setVisible(true);
    setOKActionEnabled(false);
  }

  private void clearWarningAndEnableOK() {
    myMaskWarningLabel.setVisible(false);
    setOKActionEnabled(true);
  }

  private static boolean isMaskValid(@NotNull String mask) {
    try {
      FindInProjectUtil.createFileMaskRegExp(mask);
    } catch (PatternSyntaxException e) {
      return false;
    }
    return true;
  }

  @Nullable
  public String getFileTypeMask() {
    if (myEnableFileNameFilterCb.isSelected()) {
      return (String)myFileFilter.getSelectedItem();
    }

    return null;
  }

  protected void createUIComponents() {
    myScopeCombo = new ScopeChooserCombo(myProject, false, false, FindSettings.getInstance().getDefaultScopeName());
    Disposer.register(myDisposable, myScopeCombo);
  }

  @Nullable
  @Override
  public SearchScope getSearchScope() {
    if (myUseScopeFilteringCb.isSelected()) {
      return myScopeCombo.getSelectedScope();
    }

    return null;
  }

}
