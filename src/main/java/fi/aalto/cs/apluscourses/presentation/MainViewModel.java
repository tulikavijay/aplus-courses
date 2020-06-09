package fi.aalto.cs.apluscourses.presentation;

import fi.aalto.cs.apluscourses.utils.observable.ObservableProperty;
import fi.aalto.cs.apluscourses.utils.observable.ObservableReadWriteProperty;
import org.jetbrains.annotations.NotNull;

public class MainViewModel {

  @NotNull
  public final ObservableProperty<CourseViewModel> courseViewModel =
      new ObservableReadWriteProperty<>(null);
}
