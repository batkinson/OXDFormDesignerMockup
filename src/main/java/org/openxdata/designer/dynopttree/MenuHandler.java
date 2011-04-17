package org.openxdata.designer.dynopttree;

import org.apache.pivot.beans.BXML;
import org.apache.pivot.beans.BeanAdapter;
import org.apache.pivot.collections.List;
import org.apache.pivot.collections.Sequence;
import org.apache.pivot.wtk.Action;
import org.apache.pivot.wtk.Component;
import org.apache.pivot.wtk.Dialog;
import org.apache.pivot.wtk.Menu;
import org.apache.pivot.wtk.Menu.Section;
import org.apache.pivot.wtk.MenuBar;
import org.apache.pivot.wtk.TreeView;
import org.openxdata.designer.util.DynamicOption;
import org.openxdata.designer.util.Option;

public class MenuHandler implements org.apache.pivot.wtk.MenuHandler {

	@BXML
	private Dialog optionDialog;

	public void configureMenuBar(Component component, MenuBar menuBar) {
	}

	public void cleanupMenuBar(Component component, MenuBar menuBar) {
	}

	public boolean configureContextMenu(Component component, Menu menu, int x,
			int y) {

		final TreeView dynOptTree = (TreeView) component;
		List<?> treeData = dynOptTree.getTreeData();
		Section section = new Section();

		Sequence.Tree.Path clickedPath = dynOptTree.getNodeAt(y);
		Sequence.Tree.Path parentPath = new Sequence.Tree.Path(clickedPath,
				clickedPath.getLength() - 1);

		Object clickedObject = Sequence.Tree.get(treeData, clickedPath);
		Object clickedParent = Sequence.Tree.get(treeData, parentPath);

		if (clickedObject instanceof DynamicOption) {

			final DynamicOption dynOption = (DynamicOption) clickedObject;

			Menu.Item addOptionItem = new Menu.Item("Add Option");

			addOptionItem.setAction(new Action() {
				@Override
				public void perform(Component source) {
					dynOption.add(new Option());
				}
			});

			section.add(addOptionItem);
		} else if (clickedObject instanceof Option) {

			final Option option = (Option) clickedObject;
			final DynamicOption dynOption = (DynamicOption) clickedParent;

			Menu.Item removeOptionItem = new Menu.Item("Remove Option");
			Menu.Item propertiesItem = new Menu.Item("Properties...");

			removeOptionItem.setAction(new Action() {
				@Override
				public void perform(Component source) {
					dynOption.remove(option);
				}
			});

			propertiesItem.setAction(new Action() {
				@Override
				public void perform(Component source) {
					optionDialog.getUserData().put("activeOption", option);
					optionDialog.load(new BeanAdapter(option));
					optionDialog.open(dynOptTree.getDisplay(),
							dynOptTree.getWindow());
				}
			});

			section.add(removeOptionItem);
			section.add(propertiesItem);
		}

		// Only add menu section if there were items added
		if (section.getLength() > 0)
			menu.getSections().add(section);

		return false;
	}
}
