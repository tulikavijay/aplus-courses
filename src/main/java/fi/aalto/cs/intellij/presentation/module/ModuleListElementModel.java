package fi.aalto.cs.intellij.presentation.module;

import fi.aalto.cs.intellij.model.Module;
import fi.aalto.cs.intellij.presentation.base.BaseModel;
import fi.aalto.cs.intellij.presentation.base.ListElementModel;
import fi.aalto.cs.intellij.utils.ObservableProperty;
import java.awt.font.TextAttribute;
import org.jetbrains.annotations.NotNull;

public class ModuleListElementModel extends ListElementModel<Module> {

  public ModuleListElementModel(@NotNull Module module) {
    super(module);
    module.stateChanged.addListener(this, BaseModel::changed);
  }

  public String getName() {
    return getModel().getName();
  }

  public String getUrl() {
    return getModel().getUrl().toString();
  }

  /**
   * Returns a textual representation of the status of the module.
   * @return A {@link String} describing the status.
   */
  public String getStatus() {
    switch (getModel().getState()) {
      case Module.NOT_INSTALLED:
      case Module.FETCHED:
        return "Not installed";
      case Module.FETCHING:
        return "Downloading...";
      case Module.LOADING:
        return "Installing...";
      case Module.LOADED:
        return "Loaded";
      case Module.WAITING_FOR_DEPS:
        return "Waiting for dependencies...";
      case Module.INSTALLED:
        return "Installed";
      default:
        return "Error";
    }
  }

  /**
   * Returns a font weight in which the module is shown on a list.
   * @return A {@link Float} that can be set to font weight.
   */
  public float getFontWeight() {
    return getModel().getState() == Module.INSTALLED
        ? TextAttribute.WEIGHT_BOLD
        : TextAttribute.WEIGHT_REGULAR;
  }
}
