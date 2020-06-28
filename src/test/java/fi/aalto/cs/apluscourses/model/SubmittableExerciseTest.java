package fi.aalto.cs.apluscourses.model;

import java.util.Arrays;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class SubmittableExerciseTest {

  @Test
  public void testSubmittableExercise() {
    SubmittableExercise exercise = new SubmittableExercise(123, "yay",
        Arrays.asList("file1", "file2"));
    Assert.assertEquals("The ID is the same as the one given to the constructor",
        123, exercise.getId());
    Assert.assertEquals("The name is the same as the one given to the constructor",
        "yay", exercise.getName());
    Assert.assertEquals("The filenames are the same as those given to the constructor",
        "file1", exercise.getFilenames().get(0));
    Assert.assertEquals("The filenames are the same as those given to the constructor",
        "file2", exercise.getFilenames().get(1));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetFilenamesReturnsUnmodifiableList() {
    SubmittableExercise exercise = new SubmittableExercise(0, "", Collections.emptyList());
    exercise.getFilenames().add("");
  }

  @Test
  public void testFromJsonObject() {
    JSONArray formSpec = new JSONArray()
        .put(new JSONObject()
            .put("title", "i18n_coolFilename.scala")
            .put("type", "file"))
        .put(new JSONObject()
            .put("title", "ignored because")
            .put("type", "is not file"));

    JSONObject formLocalization = new JSONObject()
        .put("i18n_coolFilename.scala", new JSONObject()
            .put("en", "coolFilename.scala"));

    JSONObject exerciseInfo = new JSONObject()
        .put("form_spec", formSpec)
        .put("form_i18n", formLocalization);

    JSONObject json = new JSONObject()
        .put("id", 321)
        .put("name", "test exercise")
        .put("exercise_info", exerciseInfo);

    SubmittableExercise exercise = SubmittableExercise.fromJsonObject(json);

    Assert.assertEquals("The ID is the same as that in the JSON", 321, exercise.getId());
    Assert.assertEquals("The name is the same as that in the JSON",
        "test exercise", exercise.getName());
    Assert.assertEquals("The filenames are parsed from the JSON",
        "coolFilename.scala", exercise.getFilenames().get(0));
  }

}
