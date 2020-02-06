package fi.aalto.cs.intellij.model.impl;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtilRt;
import fi.aalto.cs.intellij.model.Module;
import fi.aalto.cs.intellij.model.ModuleLoadException;
import fi.aalto.cs.intellij.utils.DomUtil;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FileUtils;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

class IntelliJModule extends Module {
  private static final Logger logger = LoggerFactory.getLogger(IntelliJModule.class);

  @NotNull
  private final Project project;

  private static final String DEPENDENCY_NAMES =
      "/module/component/orderEntry[@type='module']/@module-name";

  IntelliJModule(@NotNull String name, @NotNull URL url, @NotNull Project project) {
    super(name, url);

    this.project = project;
  }

  @Override
  @NotNull
  public List<String> getDependencies() throws IOException, ModuleLoadException {
    try {
      return DomUtil.getNodesFromXPath(DEPENDENCY_NAMES, getImlFile())
          .stream()
          .map(Node::getTextContent)
          .collect(Collectors.toList());
    } catch (SAXException e) {
      throw new ModuleLoadException(e);
    }
  }

  @Override
  public void fetch() throws IOException {
    File file = createTempFile();
    fetchZipTo(file);
    extractZip(file);
  }

  @Override
  public void load() throws ModuleLoadException {
    ModuleManager moduleManager = ModuleManager.getInstance(getProject());
    String imlFileName = getImlFile().toString();
    WriteAction.runAndWait(() -> {
      try {
        moduleManager.loadModule(imlFileName);
      } catch (IOException | JDOMException | ModuleWithNameAlreadyExists e) {
        throw new ModuleLoadException(e);
      }
    });
  }

  @NotNull
  protected File createTempFile() throws IOException {
    return FileUtilRt.createTempFile(getName(), ".zip");
  }

  protected void extractZip(File file) throws IOException {
    new ZipFile(file).extractAll(getBasePath());
  }

  protected void fetchZipTo(File file) throws IOException {
    FileUtils.copyURLToFile(getUrl(), file);
  }

  protected String getBasePath() {
    return Objects.requireNonNull(getProject().getBasePath());
  }

  protected File getImlFile() {
    String name = getName();
    return Paths.get(getBasePath(), name, name + ".iml").toFile();
  }

  @NotNull
  public Project getProject() {
    return project;
  }
}
