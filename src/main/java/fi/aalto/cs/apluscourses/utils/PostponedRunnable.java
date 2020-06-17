package fi.aalto.cs.apluscourses.utils;

import com.intellij.openapi.application.ApplicationManager;
import java.util.concurrent.Executor;
import org.jetbrains.annotations.NotNull;

/**
 * A runnable whose {@code run} method uses some {@code invokeLater} method (by default {@code
 * ApplicationManager.getApplication()::invokeLater} is used.
 */
public class PostponedRunnable implements Runnable {
  @NotNull
  private Runnable task;
  @NotNull
  private Executor executor;

  /**
   * This constructor allows giving a custom {@invokeLater} method, which is useful mostly for
   * testing purposes.
   */
  public PostponedRunnable(@NotNull Runnable task, @NotNull Executor executor) {
    this.task = task;
    this.executor = executor;
  }

  public PostponedRunnable(@NotNull Runnable task) {
    this(task, ApplicationManager.getApplication()::invokeLater);
  }

  @Override
  public void run() {
    executor.execute(task);
  }
}
