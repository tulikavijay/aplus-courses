package fi.aalto.cs.apluscourses.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import fi.aalto.cs.apluscourses.presentation.AuthenticationViewModel;
import java.util.Arrays;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class APlusAuthenticationView extends DialogWrapper {
  @GuiObject
  protected JPasswordField inputField;
  private JPanel basePanel;

  AuthenticationViewModel authenticationViewModel;

  /**
   * Construct an instance with the given authentication view model.
   */
  public APlusAuthenticationView(@NotNull AuthenticationViewModel authenticationViewModel,
                                 @Nullable Project project) {
    super(project);
    this.authenticationViewModel = authenticationViewModel;
    setTitle("A+ Token");
    setButtonsAlignment(SwingConstants.CENTER);
    init();
  }

  @Override
  protected void doOKAction() {
    char[] input = inputField.getPassword();
    authenticationViewModel.setToken(input);
    Arrays.fill(input, '\0');
    super.doOKAction();
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    return new Action[]{getOKAction(), getCancelAction()};
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return basePanel;
  }

  @Nullable
  @Override
  protected ValidationInfo doValidate() {
    int length = inputField.getPassword().length;
    int maxLength = authenticationViewModel.getMaxLength();
    if (length == 0) {
      return new ValidationInfo("Token must not be empty", inputField);
    }
    if (length > maxLength) {
      return new ValidationInfo(String.format("Token must not be longer than %d characters",
          maxLength), inputField);
    }
    return null;
  }

}
