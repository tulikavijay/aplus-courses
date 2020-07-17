package fi.aalto.cs.apluscourses.intellij.actions;

import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import fi.aalto.cs.apluscourses.intellij.notifications.MissingFileNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.NetworkErrorNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.NotSubmittableNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.Notifier;
import fi.aalto.cs.apluscourses.intellij.services.Dialogs;
import fi.aalto.cs.apluscourses.intellij.services.MainViewModelProvider;
import fi.aalto.cs.apluscourses.intellij.services.PluginSettings;
import fi.aalto.cs.apluscourses.intellij.utils.VfsUtil;
import fi.aalto.cs.apluscourses.model.Course;
import fi.aalto.cs.apluscourses.model.Exercise;
import fi.aalto.cs.apluscourses.model.ExerciseDataSource;
import fi.aalto.cs.apluscourses.model.FileDoesNotExistException;
import fi.aalto.cs.apluscourses.model.FileFinder;
import fi.aalto.cs.apluscourses.model.Group;
import fi.aalto.cs.apluscourses.model.SubmissionHistory;
import fi.aalto.cs.apluscourses.model.SubmissionInfo;
import fi.aalto.cs.apluscourses.model.SubmittableFile;
import fi.aalto.cs.apluscourses.presentation.AuthenticationViewModel;
import fi.aalto.cs.apluscourses.presentation.CourseViewModel;
import fi.aalto.cs.apluscourses.presentation.MainViewModel;
import fi.aalto.cs.apluscourses.presentation.ModuleSelectionViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExerciseViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExercisesTreeViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.SubmissionViewModel;
import fi.aalto.cs.apluscourses.utils.ArrayUtil;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SubmitExerciseAction extends AnAction {

  public static final String ACTION_ID = SubmitExerciseAction.class.getCanonicalName();

  @NotNull
  private final MainViewModelProvider mainViewModelProvider;

  @NotNull
  private final FileFinder fileFinder;

  @NotNull
  private final ModuleSource moduleSource;
  @NotNull
  private final Dialogs dialogs;

  @NotNull
  private final Notifier notifier;


  /**
   * Constructor with reasonable defaults.
   */
  public SubmitExerciseAction() {
    this(
        PluginSettings.getInstance(),
        VfsUtil::findFileInDirectory,
        project -> ModuleManager.getInstance(project).getModules(),
        Dialogs.DEFAULT,
        Notifications.Bus::notify
    );
  }

  /**
   * Construct an exercise submission action with the given parameters. This constructor is useful
   * for testing purposes.
   */
  public SubmitExerciseAction(@NotNull MainViewModelProvider mainViewModelProvider,
                              @NotNull FileFinder fileFinder,
                              @NotNull ModuleSource moduleSource,
                              @NotNull Dialogs dialogs,
                              @NotNull Notifier notifier) {
    this.mainViewModelProvider = mainViewModelProvider;
    this.fileFinder = fileFinder;
    this.moduleSource = moduleSource;
    this.dialogs = dialogs;
    this.notifier = notifier;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    MainViewModel mainViewModel = mainViewModelProvider.getMainViewModel(e.getProject());
    CourseViewModel courseViewModel = mainViewModel.courseViewModel.get();
    ExercisesTreeViewModel exercisesViewModel = mainViewModel.exercisesViewModel.get();
    AuthenticationViewModel authenticationViewModel = mainViewModel.getAuthentication();

    e.getPresentation().setEnabled(e.getProject() != null && exercisesViewModel != null
        && authenticationViewModel.isSet() && courseViewModel != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    try {
      trySubmit(project);
    } catch (IOException ex) {
      notifyNetworkError(ex, project);
    } catch (FileDoesNotExistException ex) {
      notifier.notify(new MissingFileNotification(ex.getPath(), ex.getName()), project);
    }
  }

  private void trySubmit(@NotNull Project project) throws IOException, FileDoesNotExistException {
    MainViewModel mainViewModel = mainViewModelProvider.getMainViewModel(project);
    CourseViewModel courseViewModel = mainViewModel.courseViewModel.get();
    ExercisesTreeViewModel exercisesViewModel = mainViewModel.exercisesViewModel.get();

    if (courseViewModel == null || exercisesViewModel == null) {
      return;
    }

    ExerciseViewModel selectedExercise = exercisesViewModel.getSelectedExercise();
    if (selectedExercise == null) {
      return;
    }

    Exercise exercise = selectedExercise.getModel();
    ExerciseDataSource exerciseDataSource = mainViewModel.getModel().getExerciseDataSource();
    SubmissionInfo submissionInfo = exerciseDataSource.getSubmissionInfo(exercise);

    if (!submissionInfo.isSubmittable()) {
      notifier.notify(new NotSubmittableNotification(), project);
      return;
    }

    Module[] modules = moduleSource.getModules(project);
    ModuleSelectionViewModel moduleSelectionViewModel = new ModuleSelectionViewModel(modules);
    if (!dialogs.create(moduleSelectionViewModel, project).showAndGet()) {
      return;
    }

    Module selectedModule = moduleSelectionViewModel.selectedModule.get();
    if (selectedModule == null) {
      return;
    }

    Path modulePath = Paths.get(ModuleUtilCore.getModuleDirPath(selectedModule));
    String[] fileNames =
        ArrayUtil.mapArray(submissionInfo.getFiles(), SubmittableFile::getName, String[]::new);
    Path[] filePaths = fileFinder.findFiles(modulePath, fileNames);

    SubmissionHistory history = exerciseDataSource.getSubmissionHistory(exercise);

    Course course = courseViewModel.getModel();
    List<Group> groups = exerciseDataSource.getGroups(course);

    SubmissionViewModel submission =
        new SubmissionViewModel(exercise, submissionInfo, history, groups, filePaths);

    if (!dialogs.create(submission, project).showAndGet()) {
      return;
    }

    exerciseDataSource.submit(submission.buildSubmission());
  }

  private void notifyNetworkError(@NotNull IOException exception, @Nullable Project project) {
    notifier.notify(new NetworkErrorNotification(exception), project);
  }

  public interface ModuleSource {
    Module[] getModules(Project project);
  }
}
