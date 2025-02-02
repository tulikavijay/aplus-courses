package fi.aalto.cs.apluscourses.ui.ideactivities;

import com.intellij.util.ui.JBUI;
import fi.aalto.cs.apluscourses.ui.GuiObject;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BalloonPopup extends JPanel implements TransparentComponent, MouseListener {
  private final @NotNull Component anchorComponent;

  @GuiObject
  private final BalloonLabel titleLabel;

  @GuiObject
  private final BalloonLabel messageLabel;

  @NotNull
  private final TransparencyAnimationHandler transparencyHandler;

  private float transparencyCoefficient;

  public static final int BORDER_WIDTH = 6;
  public static final int POPUP_MARGIN = 20;

  /**
   * Creates a popup with the given text. The popup is permanently attached to the specified
   * component. Optionally, an icon can be provided which will be displayed to the left of
   * the popup's title.
   */
  public BalloonPopup(@NotNull Component anchorComponent, @NotNull String title, @NotNull String message,
                      @Nullable Icon icon, @NotNull Action @NotNull [] actions) {
    this.anchorComponent = anchorComponent;
    transparencyHandler = new TransparencyAnimationHandler(this);

    addMouseListener(this);

    setOpaque(false);
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(new BalloonShadowBorder(BORDER_WIDTH, JBUI.insets(0, 5, 10, 5)));

    // introduce a limit to the popup's width (so it doesn't take the entire screen width)
    setMaximumSize(new Dimension(500, 0));

    titleLabel = new BalloonLabel("<html><h1>" + title + "</h1></html>");
    titleLabel.setIcon(icon);

    var titleBox = Box.createHorizontalBox();
    titleBox.setOpaque(false);
    titleBox.setAlignmentX(LEFT_ALIGNMENT);
    titleBox.add(titleLabel);
    add(titleBox);

    messageLabel = new BalloonLabel("<html>" + message + "</html>");
    messageLabel.setAlignmentX(LEFT_ALIGNMENT);
    add(messageLabel);

    if (actions.length > 0) {
      var buttonBox = Box.createHorizontalBox();
      buttonBox.setOpaque(false);
      buttonBox.setAlignmentX(LEFT_ALIGNMENT);
      buttonBox.add(Box.createHorizontalGlue());
      for (var action : actions) {
        var button = new JButton(action);
        button.setAlignmentX(RIGHT_ALIGNMENT);
        buttonBox.add(button);
      }
      add(buttonBox);
    }

    setTransparencyCoefficient(0.3f);
    recalculateBounds();
  }

  @Override
  public boolean isVisible() {
    return anchorComponent.isShowing();
  }

  @SuppressWarnings("SuspiciousNameCombination")
  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    Graphics g2 = g.create();

    final var bgColor = getBackground();
    final var newColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(),
        (int) (transparencyCoefficient * 255));

    g2.setColor(newColor);
    g2.fillRect(BORDER_WIDTH, BORDER_WIDTH, getWidth() - BORDER_WIDTH * 2, getHeight() - BORDER_WIDTH * 2);

    g2.dispose();
  }

  @Override
  public float getTransparencyCoefficient() {
    return transparencyCoefficient;
  }

  /**
   * Sets the popup transparency level.
   *
   * @param coefficient The transparency coefficient - a value between 0.0f and 1.0f.
   *                    0.0f means completely transparent, 1.0f means completely opaque.
   */
  @Override
  public void setTransparencyCoefficient(float coefficient) {
    transparencyCoefficient = coefficient;
    titleLabel.setTransparencyCoefficient(coefficient);
    messageLabel.setTransparencyCoefficient(coefficient);
  }

  /**
   * Recomputes the popups bounds and triggers a reposition if needed. Should ideally be called
   * every time anything changes in the parent frame.
   * The algorithm for popup positioning works as follows:
   * 1. Calculate, how much available space (pixels) there is in each direction.
   * 2. Verify that the popup can indeed be placed on some side relative to the component.
   * 2a. If yes, then find the side which has the most space and place the popup there.
   * 2b. If not, then place the popup in the right corner of the component.
   */
  public void recalculateBounds() {
    // the origin of the component that this popup is attached to must be converted to the
    // overlay pane's coordinate system, because that overlay uses a null layout and requires
    // that this popup specify its bounds
    var componentWindowPos = SwingUtilities.convertPoint(anchorComponent, 0, 0, getParent());

    final var windowSize = JOptionPane.getRootFrame().getSize();
    final var componentSize = anchorComponent.getSize();

    final int popupWidth = titleLabel.getPreferredSize().width + 20;
    final int popupHeight = getMinimumSize().height;

    final int availableSizeLeft = componentWindowPos.x;
    final int availableSizeRight = windowSize.width - (componentWindowPos.x + componentSize.width);
    final int availableSizeTop = componentWindowPos.y;
    final int availableSizeBottom = windowSize.height - (componentWindowPos.y + componentSize.height);

    final int mostHorizontalSpace = Integer.max(availableSizeLeft, availableSizeRight);
    final int mostVerticalSpace = Integer.max(availableSizeTop, availableSizeBottom);

    final boolean canPlaceHorizontally = mostHorizontalSpace > popupWidth + 2 * POPUP_MARGIN;
    final boolean canPlaceVertically = mostVerticalSpace > popupHeight + 2 * POPUP_MARGIN;

    int popupX;
    int popupY;

    if (!canPlaceHorizontally && !canPlaceVertically) {
      // if there's no space on any side of the component, place the popup on the component
      popupX = componentWindowPos.x + componentSize.width - popupWidth - POPUP_MARGIN;
      popupY = componentWindowPos.y + POPUP_MARGIN;

      final var popupBounds = new Rectangle(0, 0, popupWidth, popupHeight);
      final var mousePos = getMousePosition();

      transparencyHandler.update(mousePos != null && popupBounds.contains(mousePos));
      if (transparencyHandler.isInAnimation()) {
        repaint();
      }
    } else {
      if (!canPlaceVertically || (mostHorizontalSpace > mostVerticalSpace && canPlaceHorizontally)) {
        if (availableSizeRight > availableSizeLeft) {
          popupX = componentWindowPos.x + anchorComponent.getWidth() + 5;
        } else {
          popupX = componentWindowPos.x - popupWidth - 5;
        }

        popupY = componentWindowPos.y + (anchorComponent.getHeight() - popupHeight) / 2;
      } else {
        if (availableSizeBottom > availableSizeTop) {
          popupY = componentWindowPos.y + anchorComponent.getHeight() + 5;
        } else {
          popupY = componentWindowPos.y - popupHeight - 5;
        }

        popupX = componentWindowPos.x + (anchorComponent.getWidth() - popupWidth) / 2;
      }

      setTransparencyCoefficient(1.0f);
    }

    setBounds(popupX, popupY, popupWidth, popupHeight);
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    // not used
  }

  @Override
  public void mousePressed(MouseEvent e) {
    // not used
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    // not used
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    transparencyHandler.resetAnimationProgress();
    transparencyHandler.update(true);

    repaint();
  }

  @Override
  public void mouseExited(MouseEvent e) {
    transparencyHandler.resetAnimationProgress();
    transparencyHandler.update(false);

    repaint();
  }
}
