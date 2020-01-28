package fi.aalto.cs.intellij.utils;

import static fi.aalto.cs.intellij.PluginsTestHelper.getDummyPluginsListOfTwo;
import static org.junit.Assert.assertEquals;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class RequiredPluginsCheckerUtilTest {

  @Test
  public void testGetPluginsNamesStringWithEmptyInputReturnsEmpty() {
    List<IdeaPluginDescriptor> validList = new ArrayList<>();
    String result = RequiredPluginsCheckerUtil.getPluginsNamesString(validList);

    assertEquals("The result of provision of an empty list should be empty. ",
        "", result);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetPluginsNamesStringWithNullInput() {
    RequiredPluginsCheckerUtil.getPluginsNamesString(null);
  }

  @Test
  public void testGetPluginsNamesStringWithFaultyPluginInputReturnsRightResult() {
    List<IdeaPluginDescriptor> validList = new ArrayList<>();
    validList.add(null);
    validList.addAll(getDummyPluginsListOfTwo());
    String result = RequiredPluginsCheckerUtil.getPluginsNamesString(validList);

    assertEquals(
        "The result of provision of a dummy list containing 'null' should contain names of"
            + " the dummy plugins and not the 'null'.", "A+ Courses, Scala", result);
  }

  @Test
  public void testGetPluginsNamesStringWithValidInputReturnsRightResult() {
    List<IdeaPluginDescriptor> validList = getDummyPluginsListOfTwo();
    String result = RequiredPluginsCheckerUtil.getPluginsNamesString(validList);

    assertEquals("The result of provision of a dummy list should contain names the "
        + "dummy plugins.", "A+ Courses, Scala", result);
  }
}