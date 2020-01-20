package fi.aalto.cs.intellij.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

public class ResourcesTest {

  @Test
  public void test_getProperties() {
    final MockInputStream stream = new MockInputStream("a=x\nb=y\n");

    Resources res = new Resources(name -> {
      Assert.assertEquals("correct-properties", name);
      return stream;
    });

    Assert.assertFalse(stream.hasCloseMethodBeenCalled);

    Properties props = res.getProperties("correct-properties");

    Assert.assertTrue(stream.hasCloseMethodBeenCalled);

    Assert.assertEquals("x", props.getProperty("a"));
    Assert.assertEquals("y", props.getProperty("b"));
  }

  @Test
  public void test_getProperties_withError() {
    Resources res = new Resources(name -> new InputStream() {
      @Override
      public int read() throws IOException {
        throw new IOException();
      }
    });

    Properties props = res.getProperties("inaccessible-resource");
    Assert.assertNull(props);
  }

  @Test
  public void test_getProperties_withNullStream() {
    Resources res = new Resources(name -> null);

    Properties props = res.getProperties("null-stream-resource");
    Assert.assertNull(props);
  }

  private static class MockInputStream extends ByteArrayInputStream {
    boolean hasCloseMethodBeenCalled = false;

    public MockInputStream(String content) {
      super(content.getBytes());
    }

    @Override
    public void close() throws IOException {
      super.close();
      hasCloseMethodBeenCalled = true;
    }
  }
}
