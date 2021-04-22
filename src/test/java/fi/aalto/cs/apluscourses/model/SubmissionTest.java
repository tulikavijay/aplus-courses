package fi.aalto.cs.apluscourses.model;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import org.junit.Test;

public class SubmissionTest {

  @Test
  public void testCreate() {
    Exercise exercise = new Exercise(85678, "ex", "http://localhost:1000", 0, 0, 0, true, OptionalLong.empty());
    SubmittableFile fileA = new SubmittableFile("keyA", "fileA");
    SubmittableFile fileB = new SubmittableFile("keyB", "fileB");
    String language = "de";
    SubmissionInfo info = new SubmissionInfo(
        10, Collections.singletonMap(language, List.of(fileA, fileB)));
    Map<String, Path> files = new HashMap<>();
    files.put("fileA", Paths.get("some.file"));
    files.put("fileB", Paths.get("other.file"));
    Group group = new Group(0, Collections.singletonList("Only me"));

    Submission submission = new Submission(exercise, info, files, group, language);

    assertSame(exercise, submission.getExercise());
    assertSame(info, submission.getSubmissionInfo());
    assertThat(files, is(files));
    assertSame(group, submission.getGroup());
    assertEquals(language, submission.getLanguage());
  }
}
