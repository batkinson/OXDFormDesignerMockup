package org.openxdata.designer.designtree;

import org.apache.pivot.beans.BXML;
import org.apache.pivot.beans.BeanAdapter;
import org.apache.pivot.collections.List;
import org.apache.pivot.collections.Sequence;
import org.apache.pivot.collections.Sequence.Tree.Path;
import org.apache.pivot.wtk.Action;
import org.apache.pivot.wtk.Component;
import org.apache.pivot.wtk.Dialog;
import org.apache.pivot.wtk.Menu;
import org.apache.pivot.wtk.Menu.Section;
import org.apache.pivot.wtk.MenuBar;
import org.apache.pivot.wtk.TreeView;
import org.openxdata.designer.DynamicOptionDialog;
import org.openxdata.designer.SkipRuleDialog;
import org.openxdata.designer.ValidationRuleDialog;
import org.openxdata.designer.util.DynamicOptionProxy;
import org.openxdata.designer.util.Form;
import org.openxdata.designer.util.Option;
import org.openxdata.designer.util.Page;
import org.openxdata.designer.util.Question;

public class MenuHandler implements org.apache.pivot.wtk.MenuHandler {

	@BXML
	private Dialog formDialog;

	@BXML
	private Dialog pageDialog;

	@BXML
	private Dialog questionDialog;

	@BXML
	private Dialog optionDialog;

	@BXML
	private DynamicOptionDialog dynamicOptionDialog;

	@BXML
	private SkipRuleDialog skipRuleDialog;

	@BXML
	private ValidationRuleDialog validationRuleDialog;

	public void configureMenuBar(Component component, MenuBar menuBar) {
	}

	public void cleanupMenuBar(Component component, MenuBar menuBar) {
	}

	@SuppressWarnings("unchecked")
	public boolean configureContextMenu(Component component, Menu menu, int x,
			int y) {

		final TreeView designTree = (TreeView) component;
		final Path clickedPath = designTree.getNodeAt(y);
		List<?> treeData = designTree.getTreeData();

		if (clickedPath != null) {

			Path parentPath = new Path(clickedPath, clickedPath.getLength() - 1);

			Object clickedParent = Sequence.Tree.get(treeData, parentPath);
			Object clickedObject = Sequence.Tree.get(treeData, clickedPath);

			// TODO: Use localized Strings
			Section section = new Section();
			if (clickedObject instanceof Form) {

				final Form form = (Form) clickedObject;

				Menu.Item newPageItem = new Menu.Item("Add Page");
				Menu.Item manageSkipRules = new Menu.Item(
						"Manage Skip Rules...");
				Menu.Item propertiesItem = new Menu.Item("Properties...");

				newPageItem.setAction(new Action() {
					@Override
					public void perform(Component source) {
						designTree.setBranchExpanded(clickedPath, true);
						form.addPage();
					}
				});

				manageSkipRules.setAction(new Action() {
					@Override
					public void perform(Component source) {
						skipRuleDialog.getUserData().put("activeForm", form);
						skipRuleDialog.loadRules();
						skipRuleDialog.open(designTree.getDisplay(),
								designTree.getWindow());
					}
				});

				propertiesItem.setAction(new Action() {
					@Override
					public void perform(Component source) {
						formDialog.getUserData().put("activeForm", form);
						formDialog.load(new BeanAdapter(form));
						formDialog.open(designTree.getDisplay(),
								designTree.getWindow());
					}
				});

				section.add(newPageItem);
				section.add(manageSkipRules);
				section.add(propertiesItem);
			} else if (clickedObject instanceof Page) {

				final Form form = (Form) clickedParent;
				final Page page = (Page) clickedObject;

				Menu.Item newQuestionItem = new Menu.Item("New Question");
				Menu.Item removePageItem = new Menu.Item("Remove Page");
				Menu.Item propertiesItem = new Menu.Item("Properties...");

				newQuestionItem.setAction(new Action() {
					@Override
					public void perform(Component source) {
						designTree.setBranchExpanded(clickedPath, true);
						page.newQuestion();
					}
				});

				removePageItem.setAction(new Action() {
					@Override
					public void perform(Component source) {
						form.remove(page);
					}
				});

				propertiesItem.setAction(new Action() {
					@Override
					public void perform(Component source) {
						pageDialog.getUserData().put("activePage", page);
						pageDialog.load(new BeanAdapter(page));
						pageDialog.open(designTree.getDisplay(),
								designTree.getWindow());
					}
				});

				section.add(newQuestionItem);
				section.add(removePageItem);
				section.add(propertiesItem);
			} else if (clickedObject instanceof Question) {

				final Form form = (Form) Sequence.Tree.get(treeData,
						new Sequence.Tree.Path(0));
				final List<Question> questionList = (List<Question>) clickedParent;
				final Question question = (Question) clickedObject;

				Menu.Item removeQuestionItem = new Menu.Item("Remove Question");
				Menu.Item newQuestionItem = new Menu.Item("New Question");
				Menu.Item newOptionItem = new Menu.Item("New Option");
				Menu.Item validationRuleItem = new Menu.Item(
						"Validation Rules...");
				Menu.Item propertiesItem = new Menu.Item("Properties...");

				removeQuestionItem.setAction(new Action() {
					public void perform(Component source) {
						questionList.remove(question);
					}
				});

				newQuestionItem.setAction(new Action() {
					@Override
					public void perform(Component source) {
						designTree.setBranchExpanded(clickedPath, true);
						question.newQuestion();
					}
				});

				newOptionItem.setAction(new Action() {
					@Override
					public void perform(Component source) {
						designTree.setBranchExpanded(clickedPath, true);
						question.newOption();
					}
				});

				validationRuleItem.setAction(new Action() {
					@Override
					public void perform(Component source) {

						validationRuleDialog.getUserData().put("activeForm",
								form);
						validationRuleDialog.getUserData().put(
								"activeQuestion", question);
						validationRuleDialog.loadRule();
						validationRuleDialog.open(designTree.getDisplay(),
								designTree.getWindow());
					}
				});

				propertiesItem.setAction(new Action() {
					@Override
					public void perform(Component source) {
						questionDialog.getUserData().put("activeQuestion",
								question);
						questionDialog.load(new BeanAdapter(question));
						questionDialog.open(designTree.getDisplay(),
								designTree.getWindow());
					}
				});

				section.add(removeQuestionItem);
				if (question.isQuestionList())
					section.add(newQuestionItem);
				if (question.isStaticOptionList())
					section.add(newOptionItem);
				section.add(validationRuleItem);
				section.add(propertiesItem);
			} else if (clickedObject instanceof Option) {

				final List<Option> optionList = (List<Option>) clickedParent;
				final Option option = (Option) clickedObject;

				Menu.Item removeOptionItem = new Menu.Item("Remove Option");
				Menu.Item propertiesItem = new Menu.Item("Properties...");

				removeOptionItem.setAction(new Action() {
					public void perform(Component source) {
						optionList.remove(option);
					}
				});

				propertiesItem.setAction(new Action() {
					@Override
					public void perform(Component source) {
						optionDialog.getUserData().put("activeOption", option);
						optionDialog.load(new BeanAdapter(option));
						optionDialog.open(designTree.getDisplay(),
								designTree.getWindow());
					}
				});

				section.add(removeOptionItem);
				section.add(propertiesItem);
			} else if (clickedObject instanceof DynamicOptionProxy) {

				final Form form = (Form) treeData.get(0);
				final Question parentQuestion = (Question) clickedParent;

				Menu.Item manageOptionsItem = new Menu.Item("Manage Options...");

				manageOptionsItem.setAction(new Action() {
					@Override
					public void perform(Component source) {
						dynamicOptionDialog.getUserData().put("activeForm",
								form);
						dynamicOptionDialog.getUserData().put("activeQuestion",
								parentQuestion);
						dynamicOptionDialog.updateDialog();
						dynamicOptionDialog.open(designTree.getDisplay(),
								designTree.getWindow());
					}
				});

				section.add(manageOptionsItem);
			}

			// Only add menu section if there were items added
			if (section.getLength() > 0)
				menu.getSections().add(section);
		}

		return false;
	}
}
