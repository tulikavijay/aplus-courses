package fi.aalto.cs.apluscourses.intellij.utils;

import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import fi.aalto.cs.apluscourses.intellij.notifications.DefaultNotifier;
import fi.aalto.cs.apluscourses.intellij.notifications.MissingModuleNotification;
import fi.aalto.cs.apluscourses.model.Module;
import fi.aalto.cs.apluscourses.model.ModuleMetadata;
import fi.aalto.cs.apluscourses.utils.Version;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CourseFileManager {

  private static final Logger logger = LoggerFactory.getLogger(CourseFileManager.class);

  private File courseFile;
  private URL courseUrl;
  private String language;
  private Map<String, ModuleMetadata> modulesMetadata;
  private final Map<String, String[]> moduleDependencies;
  private final Project project;

  private static final String COURSE_FILE_NAME = "a-plus-project.json";
  private static final String URL_KEY = "url";
  private static final String LANGUAGE_KEY = "language";
  private static final String MODULES_KEY = "modules";
  private static final String MODULE_VERSION_KEY = "version";
  private static final String MODULE_DOWNLOADED_AT_KEY = "downloadedAt";
  private static final String MODULE_DEPENDENCIES_KEY = "dependencies";

  public CourseFileManager(@NotNull Project project) {
    courseFile = getCourseFile(project);
    this.project = project;
    this.moduleDependencies = new HashMap<>();
  }

  /**
   * Attempts to create a course file and load it for the given project with the given URL and
   * language. If a course file for already exists, then the URL and language is overwritten, but
   * the existing modules metadata in the course file is preserved.
   *
   * @param courseUrl The URL that gets added to the course file.
   * @param language  The language string that gets added to the course file.
   * @throws IOException If an IO error occurs.
   */
  public synchronized void createAndLoad(@NotNull URL courseUrl,
                                         @NotNull String language) throws IOException {
    if (courseFile.exists()) {
      load();
    } else {
      // createAndLoad never overwrites modules metadata of an existing course file.
      // Should this be in the constructor?
      this.modulesMetadata = new HashMap<>();
    }
    this.courseUrl = courseUrl;
    this.language = language;
    writeCourseFile(
        new JSONObject()
            .put(URL_KEY, courseUrl.toString())
            .put(LANGUAGE_KEY, language)
            .put(MODULES_KEY, createModulesObject())
            .put(MODULE_DEPENDENCIES_KEY, createDependenciesObject())
    );
  }

  /**
   * Attempts to load the course file corresponding to the given project. Returns {@code false} if
   * the course file doesn't exist, {@code true} otherwise.
   *
   * @return {@code true} if the course file was successfully loaded, {@code false} if the course
   * file doesn't exist.
   * @throws IOException If an IO error occurs while reading the course file.
   */
  public synchronized boolean load() throws IOException {
    if (courseFile.exists()) {
      JSONObject jsonObject = readCourseFile();
      loadFromJsonObject(jsonObject);
      return true;
    }
    return false;
  }

  /**
   * Adds an entry for the given module to the currently loaded course file. If an entry already
   * exists for the given module, then it is overwritten with the new entry.
   *
   * @param module The module for which an entry is added.
   * @throws IOException If an IO error occurs while writing to the course file.
   */
  public synchronized void addModuleEntry(@NotNull Module module) throws IOException {
    ModuleMetadata newModuleMetadata = module.getMetadata();
    JSONObject newModuleObject = new JSONObject()
        .put(MODULE_VERSION_KEY, newModuleMetadata.getVersion())
        .put(MODULE_DOWNLOADED_AT_KEY, newModuleMetadata.getDownloadedAt());

    JSONObject modulesObject = createModulesObject();

    modulesObject.put(module.getName(), newModuleObject);

    addAllDependencies();
    fixDependencies();

    JSONObject jsonObject = new JSONObject()
        .put(URL_KEY, courseUrl.toString())
        .put(LANGUAGE_KEY, language)
        .put(MODULES_KEY, modulesObject)
        .put(MODULE_DEPENDENCIES_KEY, createDependenciesObject());

    writeCourseFile(jsonObject);

    // Only add the entry to the map after writing to the file, so that if the write fails, the map
    // is still in the correct state.
    modulesMetadata.put(module.getName(), newModuleMetadata);
  }

  private void addModuleDependencies(@NotNull String moduleName) {
    var module = ModuleManager.getInstance(project).findModuleByName(moduleName);
    if (module == null) {
      return;
    }
    var deps = Arrays.stream(ModuleRootManager.getInstance(module).getDependencies())
        .map(com.intellij.openapi.module.Module::getName)
        .toArray(String[]::new);

    moduleDependencies.computeIfAbsent(moduleName, m -> deps);
    if (moduleDependencies.get(moduleName).length < deps.length) {
      moduleDependencies.put(moduleName, deps);
    }
  }

  private void addAllDependencies() {
    var modules = Arrays.stream(ModuleManager.getInstance(project).getModules())
        .map(com.intellij.openapi.module.Module::getName)
        .toArray(String[]::new);
    for (String moduleName : modules) {
      addModuleDependencies(moduleName);
    }
  }

  private void fixDependencies() {
    var moduleManager = ModuleManager.getInstance(project);
    var modules = Arrays.stream(moduleManager.getModules())
        .map(com.intellij.openapi.module.Module::getName)
        .toArray(String[]::new);
    var missingModules = new HashSet<String>();
    for (String moduleName : modules) {
      var module = moduleManager.findModuleByName(moduleName);
      if (module == null) {
        continue;
      }
      var moduleRootManager = ModuleRootManager.getInstance(module);
      var depsInIml = Arrays.stream(moduleRootManager.getDependencies())
          .map(com.intellij.openapi.module.Module::getName).collect(Collectors.toCollection(HashSet::new));
      var depsInJson = new HashSet<>(Arrays.asList(moduleDependencies.get(moduleName)));
      depsInJson.removeAll(depsInIml);
      if (!depsInJson.isEmpty()) {
        var model = moduleRootManager.getModifiableModel();
        for (var missingModelName : depsInJson) {
          var missingModule = moduleManager.findModuleByName(missingModelName);
          if (missingModule != null) {
            model.addModuleOrderEntry(missingModule);
            model.commit();
          } else {
            missingModules.add(missingModelName);
          }
        }
      }
    }
    if (!missingModules.isEmpty()) {
      new DefaultNotifier().notify(new MissingModuleNotification(String.join(", ", missingModules)), project);
    }
  }

  /**
   * Returns the URL of the course for the currently loaded course file. This should only be called
   * after a course file has been successfully loaded.
   */
  @NotNull
  public synchronized URL getCourseUrl() {
    return courseUrl;
  }

  /**
   * Returns the language chosen by the user for the course corresponding to the currently loaded
   * course file. This should only be called after a course file has been successfully loaded.
   */
  @NotNull
  public synchronized String getLanguage() {
    return language;
  }

  /**
   * Returns the metadata of modules in the currently loaded course file, or an empty map if no
   * course file has been loaded.
   */
  @NotNull
  public synchronized Map<String, ModuleMetadata> getModulesMetadata() {
    // Return a copy so that later changes to the map aren't visible in the returned map.
    return modulesMetadata != null ? new HashMap<>(modulesMetadata) : Collections.emptyMap();
  }

  @NotNull
  private JSONObject readCourseFile() throws IOException {
    return new JSONObject(FileUtils.readFileToString(courseFile, StandardCharsets.UTF_8));
  }

  private void writeCourseFile(@NotNull JSONObject jsonObject) throws IOException {
    FileUtils.writeStringToFile(courseFile, jsonObject.toString(), StandardCharsets.UTF_8);
  }

  /*
   * Returns the course file corresponding to the given project.
   */
  @NotNull
  private File getCourseFile(@NotNull Project project) {
    return Paths
        .get(Objects.requireNonNull(project.getBasePath()),
            Project.DIRECTORY_STORE_FOLDER,
            COURSE_FILE_NAME)
        .toFile();
  }

  /*
   * Returns a JSONObject corresponding to the contents of the modulesMetadata map.
   */
  @NotNull
  private JSONObject createModulesObject() {
    JSONObject modulesObject = new JSONObject();
    modulesMetadata.forEach((name, metadata) -> modulesObject
        .put(name, new JSONObject()
            .put(MODULE_VERSION_KEY, metadata.getVersion())
            .put(MODULE_DOWNLOADED_AT_KEY, metadata.getDownloadedAt())));
    return modulesObject;
  }

  @NotNull
  private JSONObject createDependenciesObject() {
    var dependenciesObject = new JSONObject();
    moduleDependencies.forEach(dependenciesObject::put);
    return dependenciesObject;
  }

  /*
   * Initializes local variables from the given JSON object.
   */
  private void loadFromJsonObject(@NotNull JSONObject jsonObject) throws IOException {
    this.courseUrl = new URL(jsonObject.getString(URL_KEY));

    try {
      this.language = jsonObject.getString(LANGUAGE_KEY);
    } catch (JSONException e) {
      logger.error("Course file missing language, defaulting to 'en'", e);
      language = "en";
    }

    this.modulesMetadata = new HashMap<>();
    JSONObject modulesObject = jsonObject.optJSONObject(MODULES_KEY);
    if (modulesObject == null) {
      return;
    }

    Iterable<String> moduleNames = modulesObject::keys;
    for (String moduleName : moduleNames) {
      JSONObject moduleObject = modulesObject.getJSONObject(moduleName);

      Version moduleVersion = Version.fromString(moduleObject.optString(MODULE_VERSION_KEY, "1.0"));

      ZonedDateTime downloadedAt;
      try {
        downloadedAt = ZonedDateTime.parse(moduleObject.getString(MODULE_DOWNLOADED_AT_KEY));
      } catch (JSONException e) {
        logger
            .error(String.format("Module %s missing 'downloadedAt' in course file", moduleName), e);
        downloadedAt = Instant.EPOCH.atZone(ZoneId.systemDefault());
      }

      this.modulesMetadata
          .put(moduleName, new ModuleMetadata(moduleVersion, downloadedAt));
    }


    JSONObject dependenciesObject = jsonObject.optJSONObject(MODULE_DEPENDENCIES_KEY);
    if (dependenciesObject == null) {
      return;
    }

    Iterable<String> depModuleNames = dependenciesObject::keys;

    for (String moduleName : depModuleNames) {
      List<String> dependencies = new ArrayList<>();
      JSONArray jsonDependencies = dependenciesObject.getJSONArray(moduleName);
      for (int i = 0; i < jsonDependencies.length(); i++) {
        dependencies.add(jsonDependencies.getString(i));
      }

      moduleDependencies.put(moduleName, dependencies.toArray(String[]::new));
    }
    addAllDependencies();
    fixDependencies();

    writeCourseFile(new JSONObject()
        .put(URL_KEY, courseUrl.toString())
        .put(LANGUAGE_KEY, language)
        .put(MODULES_KEY, modulesObject)
        .put(MODULE_DEPENDENCIES_KEY, createDependenciesObject()));
  }
}
