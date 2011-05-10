package org.openxdata.designer.designtree;

import java.awt.Color;
import java.awt.Font;
import java.net.URL;

import org.apache.pivot.collections.Sequence.Tree.Path;
import org.apache.pivot.util.concurrent.TaskExecutionException;
import org.apache.pivot.wtk.Bounds;
import org.apache.pivot.wtk.BoxPane;
import org.apache.pivot.wtk.HorizontalAlignment;
import org.apache.pivot.wtk.ImageView;
import org.apache.pivot.wtk.Label;
import org.apache.pivot.wtk.TreeView;
import org.apache.pivot.wtk.VerticalAlignment;
import org.apache.pivot.wtk.content.TreeNode;
import org.apache.pivot.wtk.media.Image;
import org.openxdata.designer.util.DynamicOptionProxy;
import org.openxdata.designer.util.Form;
import org.openxdata.designer.util.Option;
import org.openxdata.designer.util.Page;
import org.openxdata.designer.util.Question;

public class NodeRenderer extends BoxPane implements TreeView.NodeRenderer {

	protected ImageView imageView = new ImageView();
	protected Label label = new Label();

	public static final int DEFAULT_ICON_WIDTH = 16;
	public static final int DEFAULT_ICON_HEIGHT = 16;
	public static boolean DEFAULT_SHOW_ICON = true;

	private static Image formImage;
	private static Image pageImage;
	private static Image questionImage;
	private static Image optionImage;
	private static Image dynOptionImage;

	private static Image loadImage(String resourcePath) {
		URL imageUrl = Thread.currentThread().getContextClassLoader()
				.getResource(resourcePath);
		try {
			return Image.load(imageUrl);
		} catch (TaskExecutionException e) {
			return null;
		}
	}

	static {
		String iconPath = "org/openxdata/designer/icons/";
		formImage = loadImage(iconPath + "application_form.png");
		pageImage = loadImage(iconPath + "page.png");
		questionImage = loadImage(iconPath + "question.png");
		optionImage = loadImage(iconPath + "bullet_black.png");
		dynOptionImage = loadImage(iconPath + "text_list_bullets.png");
	}

	public NodeRenderer() {
		super();

		getStyles().put("horizontalAlignment", HorizontalAlignment.LEFT);
		getStyles().put("verticalAlignment", VerticalAlignment.CENTER);

		add(imageView);
		add(label);

		imageView.setPreferredSize(DEFAULT_ICON_WIDTH, DEFAULT_ICON_HEIGHT);
		imageView.setVisible(DEFAULT_SHOW_ICON);
	}

	@Override
	public void setSize(int width, int height) {
		super.setSize(width, height);
		validate();
	}

	@Override
	public void render(Object node, Path path, int rowIndex, TreeView treeView,
			boolean expanded, boolean selected,
			TreeView.NodeCheckState checkState, boolean highlighted,
			boolean disabled) {
		if (node != null) {
			Image icon = null;
			String text = null;
			StringBuffer textBuf = new StringBuffer();

			if (node instanceof Form) {
				Form form = (Form) node;
				textBuf.append(form.getName());
				icon = formImage;
			} else if (node instanceof Page) {
				Page page = (Page) node;
				textBuf.append(page.getName());
				icon = pageImage;
			} else if (node instanceof Question) {
				Question question = (Question) node;
				textBuf.append(question.getText());
				icon = questionImage;
			} else if (node instanceof Option) {
				Option option = (Option) node;
				textBuf.append(option.getText());
				icon = optionImage;
			} else if (node instanceof DynamicOptionProxy) {
				textBuf.append("Dynamic Options..."); // Localize
				icon = dynOptionImage;
			} else
				throw new IllegalArgumentException(
						"Unrecognized tree node type: "
								+ node.getClass().getCanonicalName());

			text = textBuf.toString();

			// Update the image view
			imageView.setImage(icon);
			imageView.getStyles().put("opacity",
					(treeView.isEnabled() && !disabled) ? 1.0f : 0.5f);

			// Update the label
			label.setText(text);

			if (text == null) {
				label.setVisible(false);
			} else {
				label.setVisible(true);

				Font font = (Font) treeView.getStyles().get("font");
				label.getStyles().put("font", font);

				Color color;
				if (treeView.isEnabled() && !disabled) {
					if (selected) {
						if (treeView.isFocused()) {
							color = (Color) treeView.getStyles().get(
									"selectionColor");
						} else {
							color = (Color) treeView.getStyles().get(
									"inactiveSelectionColor");
						}
					} else {
						color = (Color) treeView.getStyles().get("color");
					}
				} else {
					color = (Color) treeView.getStyles().get("disabledColor");
				}

				label.getStyles().put("color", color);
			}
		}
	}

	public String toString(Object node) {
		String string = null;

		if (node instanceof TreeNode) {
			TreeNode treeNode = (TreeNode) node;
			string = treeNode.getText();
		} else {
			if (node != null) {
				string = node.toString();
			}
		}

		return string;
	}

	public int getIconWidth() {
		return imageView.getPreferredWidth(-1);
	}

	public void setIconWidth(int iconWidth) {
		if (iconWidth == -1) {
			throw new IllegalArgumentException();
		}

		imageView.setPreferredWidth(iconWidth);
	}

	public int getIconHeight() {
		return imageView.getPreferredHeight(-1);
	}

	public void setIconHeight(int iconHeight) {
		if (iconHeight == -1) {
			throw new IllegalArgumentException();
		}

		imageView.setPreferredHeight(iconHeight);
	}

	public boolean getShowIcon() {
		return imageView.isVisible();
	}

	public void setShowIcon(boolean showIcon) {
		imageView.setVisible(showIcon);
	}

	public boolean getFillIcon() {
		return (Boolean) imageView.getStyles().get("fill");
	}

	public void setFillIcon(boolean fillIcon) {
		imageView.getStyles().put("fill", fillIcon);
	}

	/**
	 * Gets the bounds of the text that is rendered by this renderer.
	 * 
	 * @return The bounds of the rendered text, or <tt>null</tt> if this
	 *         renderer did not render any text.
	 */
	public Bounds getTextBounds() {
		return (label.isVisible() ? label.getBounds() : null);
	}
}
