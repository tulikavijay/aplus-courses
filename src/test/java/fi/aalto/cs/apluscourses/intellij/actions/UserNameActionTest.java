package fi.aalto.cs.apluscourses.intellij.actions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import fi.aalto.cs.apluscourses.intellij.model.CourseProject;
import fi.aalto.cs.apluscourses.intellij.notifications.Notifier;
import fi.aalto.cs.apluscourses.intellij.services.CourseProjectProvider;
import fi.aalto.cs.apluscourses.model.Authentication;
import fi.aalto.cs.apluscourses.model.ModelExtensions;
import fi.aalto.cs.apluscourses.utils.async.RepeatedTask;
import java.net.URL;
import org.junit.Before;
import org.junit.Test;

public class UserNameActionTest {
  private AnActionEvent event;
  private CourseProject courseProject;
  private UserNameAction action;

  /**
   * Called before each test.
   */
  @Before
  public void setUp() throws Exception {
    event = mock(AnActionEvent.class);
    var project = mock(Project.class);
    when(event.getProject()).thenReturn(project);
    var presentation = new Presentation();
    when(event.getPresentation()).thenReturn(presentation);

    var course = new ModelExtensions.TestCourse("oe1");
    courseProject = new CourseProject(course,
        RepeatedTask.create(() -> { }), RepeatedTask.create(() -> { }),
        project, mock(Notifier.class));
    var courseProjectProvider = mock(CourseProjectProvider.class);
    when(courseProjectProvider.getCourseProject(project)).thenReturn(courseProject);

    action = new UserNameAction(courseProjectProvider);
  }

  @Test
  public void testUserNameAction() {
    action.update(event);
    assertEquals("Not logged in", event.getPresentation().getText());
    var authentication = mock(Authentication.class);
    courseProject.setAuthentication(authentication);
    action.update(event);
    assertEquals("test", event.getPresentation().getText());
  }
}
