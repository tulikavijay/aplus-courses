package fi.aalto.cs.apluscourses.intellij.notifications;

import static org.junit.Assert.assertEquals;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import org.junit.Test;

public class CourseVersionOutdatedWarningTest {

  @Test
  public void testCourseVersionOutdatedNotification() {
    Notification notification = new CourseVersionOutdatedWarning();

    assertEquals("Group ID should be A+", "A+", notification.getGroupId());
    assertEquals("Notification type should be 'warning'",
        NotificationType.WARNING, notification.getType());
  }
}
