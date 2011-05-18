package org.openxdata.designer;

import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.apache.pivot.beans.BXML;
import org.apache.pivot.beans.Bindable;
import org.apache.pivot.collections.ArrayList;
import org.apache.pivot.collections.List;
import org.apache.pivot.util.Resources;
import org.apache.pivot.wtk.Action;
import org.apache.pivot.wtk.Component;
import org.apache.pivot.wtk.Dialog;
import org.apache.pivot.wtk.ListButton;
import org.apache.pivot.wtk.ListButtonSelectionListener;
import org.apache.pivot.wtk.PushButton;
import org.apache.pivot.wtk.TreeView;
import org.fcitmuk.epihandy.DynamicOptionDef;
import org.fcitmuk.epihandy.OptionDef;
import org.openxdata.designer.util.DynamicOption;
import org.openxdata.designer.util.Form;
import org.openxdata.designer.util.Option;
import org.openxdata.designer.util.Page;
import org.openxdata.designer.util.Question;

public class DynamicOptionDialog extends Dialog implements Bindable {

	// Stores user work, cleared when updateDialog is called
	private Map<Question, List<DynamicOption>> work = new HashMap<Question, List<DynamicOption>>();

	@BXML
	private PushButton dynamicOptionDialogSaveButton;

	@BXML
	private TreeView dynamicOptionTree;

	@BXML
	private ListButton parentQuestionListButton;

	public void initialize(
			org.apache.pivot.collections.Map<String, Object> namespace,
			URL location, Resources resources) {

		parentQuestionListButton.getListButtonSelectionListeners().add(
				new ListButtonSelectionListener.Adapter() {
					@Override
					public void selectedItemChanged(ListButton listButton,
							Object previousSelectedItem) {
						Object selectedItem = listButton.getSelectedItem();
						if (selectedItem != null
								&& selectedItem instanceof Question) {
							Question parentQuestion = (Question) selectedItem;
							List<DynamicOption> treeData = getDynamicOptionTree(
									getForm(), getQuestion(), parentQuestion);
							dynamicOptionTree.setTreeData(treeData);
						}
					}
				});

		dynamicOptionDialogSaveButton.setAction(new Action() {
			@Override
			public void perform(Component source) {
				// Save dynamic options to form
				updateForm();
				close();
			}
		});
	}

	public Form getForm() {
		return (Form) getUserData().get("activeForm");
	}

	public Question getQuestion() {
		return (Question) getUserData().get("activeQuestion");
	}

	public void updateForm() {
		updateForm(getForm(), getQuestion());
	}

	public void updateDialog() {
		work.clear();
		updateDialog(getForm(), getQuestion());
	}

	private void updateDialog(Form form, Question question) {

		Question parentQuestion = getParentQuestion(form, question);

		List<Question> possibleParents = getPossibleParents(form, question);
		parentQuestionListButton.setListData(possibleParents);
		parentQuestionListButton.setSelectedItem(parentQuestion);
	}

	private List<DynamicOption> getDynamicOptionTree(Form form,
			Question question, Question parentQuestion) {

		// Use prior working copy if use already started using parent question
		if (work.containsKey(parentQuestion))
			return work.get(parentQuestion);

		// Otherwise, build a working copy from possible parent values
		Hashtable<Short, Vector<OptionDef>> pcOpts = getOptionMap(form,
				question);
		Hashtable<Short, Vector<OptionDef>> parentPcOpts = getOptionMap(form,
				parentQuestion);
		java.util.Collection<OptionDef> possibleParentValues = getPossibleValues(
				parentQuestion, parentPcOpts);

		List<DynamicOption> treeData;

		// If manipulating existing parent, use existing tree
		if (getParentQuestion(form, question) == parentQuestion)
			treeData = getExistingOptionTree(pcOpts, possibleParentValues);
		else {
			// Otherwise, use empty tree based on possbile parent values
			treeData = new ArrayList<DynamicOption>();
			for (OptionDef possibleParentValue : possibleParentValues)
				treeData.add(new DynamicOption(new Option(possibleParentValue)));
		}

		// Cache the tree so user can switch parents without losing work.
		work.put(parentQuestion, treeData);

		return treeData;
	}

	private List<Question> getPossibleParents(Form form, Question question) {
		List<Question> possibleParentQuestions = new ArrayList<Question>();
		for (Page page : form)
			for (Question q : page)
				if (q != question
						&& (q.isStaticOptionList() || q.isDynamicOptionList()))
					possibleParentQuestions.add(q);
		return possibleParentQuestions;
	}

	@SuppressWarnings("unchecked")
	private void updateForm(Form form, Question question) {

		List<DynamicOption> newOptionList = (List<DynamicOption>) dynamicOptionTree
				.getTreeData();

		Question selectedParentQuestion = (Question) parentQuestionListButton
				.getSelectedItem();

		Hashtable<Short, Vector<OptionDef>> pcOpts;
		if (getParentQuestion(form, question) == selectedParentQuestion) {
			// Re-populate the dynamic options based on user-manipulated data
			pcOpts = getOptionMap(form, question);
			pcOpts.clear();
		} else {
			removeOptionMap(form, question);
			pcOpts = new Hashtable<Short, Vector<OptionDef>>();
			DynamicOptionDef dynOptDef = new DynamicOptionDef();
			dynOptDef.setQuestionId(question.getId());
			dynOptDef.setParentToChildOptions(pcOpts);
			form.getDynamicOptions().put(selectedParentQuestion.getId(),
					dynOptDef);
		}

		for (DynamicOption option : newOptionList)
			pcOpts.put(option.getValue().getId(), option.getOptionVector());
	}

	@SuppressWarnings("unchecked")
	private void removeOptionMap(Form form, Question question) {

		Hashtable<Short, DynamicOptionDef> optionMap = (Hashtable<Short, DynamicOptionDef>) form
				.getDynamicOptions();

		// Remove parent value => child value mapping for this question
		for (Map.Entry<Short, DynamicOptionDef> entry : optionMap.entrySet()) {
			if (entry.getValue().getQuestionId() == question.getId()) {
				optionMap.remove(entry.getKey());
				break;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Hashtable<Short, Vector<OptionDef>> getOptionMap(Form form,
			Question question) {

		Hashtable<Short, Vector<OptionDef>> result = null;

		Hashtable<Short, DynamicOptionDef> optionMap = (Hashtable<Short, DynamicOptionDef>) form
				.getDynamicOptions();

		// Locate parent value => child value mapping for this question
		for (Map.Entry<Short, DynamicOptionDef> entry : optionMap.entrySet()) {
			if (entry.getValue().getQuestionId() == question.getId()) {
				result = (Hashtable<Short, Vector<OptionDef>>) entry.getValue()
						.getParentToChildOptions();
				break;
			}
		}

		return result;
	}

	private Question getParentQuestion(Form form, Question question) {

		Question result = null;

		@SuppressWarnings("unchecked")
		Hashtable<Short, DynamicOptionDef> optionMap = (Hashtable<Short, DynamicOptionDef>) form
				.getDynamicOptions();

		for (Map.Entry<Short, DynamicOptionDef> entry : optionMap.entrySet()) {
			if (entry.getValue().getQuestionId() == question.getId()) {
				result = (Question) form.getQuestion(entry.getKey());
				break;
			}
		}

		return result;
	}

	private List<DynamicOption> getExistingOptionTree(
			Hashtable<Short, Vector<OptionDef>> pcOpts,
			java.util.Collection<OptionDef> possibleParentValues) {
		List<DynamicOption> treeData = new ArrayList<DynamicOption>();
		for (OptionDef option : possibleParentValues) {
			DynamicOption dynOption = new DynamicOption((Option) option);
			if (pcOpts.containsKey(option.getId())) {
				Vector<OptionDef> dependenOptions = pcOpts.get(option.getId());
				for (OptionDef value : dependenOptions)
					dynOption.add(new Option(value));
			}
			treeData.add(dynOption);
		}
		return treeData;
	}

	private java.util.Collection<OptionDef> getPossibleValues(
			Question parentQuestion,
			Hashtable<Short, Vector<OptionDef>> parentPcOpts) {
		// Construct map of possible values for parent
		java.util.Collection<OptionDef> possibleParentValues = new java.util.ArrayList<OptionDef>();
		if (parentQuestion.isDynamicOptionList()) {
			for (Map.Entry<Short, Vector<OptionDef>> entry : parentPcOpts
					.entrySet()) {
				possibleParentValues.addAll(entry.getValue());
			}
		} else if (parentQuestion.isStaticOptionList()) {
			possibleParentValues.addAll(parentQuestion.getOptions());
		}
		return possibleParentValues;
	}
}
