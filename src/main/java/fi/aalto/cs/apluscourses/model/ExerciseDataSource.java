package fi.aalto.cs.apluscourses.model;

import fi.aalto.cs.apluscourses.utils.cache.CachePreference;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ExerciseDataSource {

  @NotNull
  List<Group> getGroups(@NotNull Course course, @NotNull Authentication authentication)
      throws IOException;

  @NotNull
  List<ExerciseGroup> getExerciseGroups(@NotNull Course course,
                                        @NotNull Authentication authentication) throws IOException;

  @NotNull
  Points getPoints(@NotNull Course course, @NotNull Authentication authentication)
      throws IOException;

  @NotNull
  Points getPoints(@NotNull Course course, @NotNull Authentication authentication, @Nullable Student student)
      throws IOException;

  @NotNull
  Exercise getExercise(long exerciseId,
                       @NotNull Points points,
                       @NotNull Map<Long, Tutorial> tutorials,
                       @NotNull Authentication authentication,
                       @NotNull CachePreference cachePreference) throws IOException;

  @NotNull
  SubmissionResult getSubmissionResult(@NotNull String submissionUrl,
                                       @NotNull Exercise exercise,
                                       @NotNull Authentication authentication,
                                       @NotNull CachePreference cachePreference) throws IOException;

  @NotNull
  User getUser(@NotNull Authentication authentication) throws IOException;

  @NotNull
  List<Student> getStudents(@NotNull Course course,
                            @NotNull Authentication authentication,
                            @NotNull CachePreference cachePreference) throws IOException;

  @NotNull
  ZonedDateTime getEndingTime(@NotNull Course course,
                              @NotNull Authentication authentication) throws IOException;

  @Nullable
  String submit(@NotNull Submission submission, @NotNull Authentication authentication)
      throws IOException;
}
