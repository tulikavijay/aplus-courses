package fi.aalto.cs.intellij.model.impl;

import com.intellij.openapi.project.Project;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

// Remove this class when modules are no longer loaded from a local dir.
class LocalLoadingIntelliJModule extends IntelliJModule {

  LocalLoadingIntelliJModule(@NotNull String name,
                                    @NotNull URL url,
                                    @NotNull Project project) {
    super(name, url, project);
  }

  @Override
  protected void fetchZipTo(File file) throws IOException {
    Files.copy(getTestZipDirPath().resolve(getName() + ".zip"), file.toPath(),
        StandardCopyOption.REPLACE_EXISTING);
  }

  private Path getTestZipDirPath() {
    return Paths.get(Objects.requireNonNull(getBasePath())).getParent().resolve("modules");
  }
}
