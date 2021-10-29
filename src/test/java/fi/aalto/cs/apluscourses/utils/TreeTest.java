package fi.aalto.cs.apluscourses.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TreeTest {

  Tree root;
  Tree node1;
  Tree node2;
  Tree node3;
  Tree node21;
  Tree node22;

  /**
   * Called before each test.
   */
  @BeforeEach
  public void setUp() {
    root = mock(Tree.class, CALLS_REAL_METHODS);
    node1 = mock(Tree.class, CALLS_REAL_METHODS);
    node2 = mock(Tree.class, CALLS_REAL_METHODS);
    node3 = mock(Tree.class, CALLS_REAL_METHODS);
    doReturn(List.of(node1, node2, node3)).when(root).getChildren();
    node21 = mock(Tree.class, CALLS_REAL_METHODS);
    node22 = mock(Tree.class, CALLS_REAL_METHODS);
    doReturn(List.of(node21, node22)).when(node2).getChildren();
  }

  @Test
  public void testTraverseAndFindDescendant() {
    List<Tree> path = root.traverseAndFind(node -> node == node21);
    assertThat(path, is(List.of(root, node2, node21)));
  }

  @Test
  public void testTraverseAndFindSelf() {
    List<Tree> path = root.traverseAndFind(node -> node == root);
    assertThat(path, is(Collections.singletonList(root)));
  }

  @Test
  public void testTraverseAndFindNone() {
    List<Tree> path = root.traverseAndFind(node -> false);
    assertNull(path);
  }
}
