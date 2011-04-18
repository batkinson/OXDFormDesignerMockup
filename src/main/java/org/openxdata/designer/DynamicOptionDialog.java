package org.openxdata.designer;

import java.net.URL;
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
import org.apache.pivot.wtk.Label;
import org.apache.pivot.wtk.PushButton;
import org.apache.pivot.wtk.TreeView;
import org.fcitmuk.epihandy.DynamicOptionDef;
import org.fcitmuk.epihandy.OptionDef;
import org.openxdata.designer.util.DynamicOption;
import org.openxdata.designer.util.Form;
import org.openxdata.designer.util.Option;
import org.openxdata.designer.util.Question;

public class DynamicOptionDialog extends Dialog implements Bindable {

	@BXML
	private PushButton dynamicOptionDialogSaveButton;

	@BXML
	private TreeView dynamicOptionTree;

	@BXML
	private Label parentQuestionLabel;

	public void initialize(
			org.apache.pivot.collections.Map<String, Object> namespace,
			URL location, Resources resources) {

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
		updateDialog(getForm(), getQuestion());
	}

	private void updateDialog(Form form, Question question) {

		Hashtable<Short, Vector<OptionDef>> pcOpts = getOptionMap(form,
				question);
		Question parentQuestion = getParentQuestion(form, question);
		Hashtable<Short, Vector<OptionDef>> parentPcOpts = getOptionMap(form,
				parentQuestion);

		java.util.Collection<OptionDef> possibleParentValues = getPossibleValues(
				parentQuestion, parentPcOpts);

		parentQuestionLabel.setText("Options depend on values of "
				+ parentQuestion.getText());

		List<DynamicOption> treeData = getOptionValueTree(pcOpts,
				possibleParentValues);

		dynamicOptionTree.setTreeData(treeData);
	}

	@SuppressWarnings("unchecked")
	private void updateForm(Form form, Question question) {

		List<DynamicOption> newOptionList = (List<DynamicOption>) dynamicOptionTree
				.getTreeData();
		Hashtable<Short, Vector<OptionDef>> pcOpts = getOptionMap(form,
				question);

		// Re-populate the dynamic options based on user-manipulated data
		pcOpts.clear();
		for (DynamicOption option : newOptionList)
			pcOpts.put(option.getValue().getId(), option.getOptionVector());
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

	private List<DynamicOption> getOptionValueTree(
			Hashtable<Short, Vector<OptionDef>> pcOpts,
			java.util.Collection<OptionDef> possibleParentValues) {
		List<DynamicOption> treeData = new ArrayList<DynamicOption>();
		for (Map.Entry<Short, Vector<OptionDef>> entry : pcOpts.entrySet()) {
			for (OptionDef option : possibleParentValues) {
				if (option.getId() == entry.getKey()) {
					DynamicOption dynOption = new DynamicOption((Option) option);
					for (OptionDef value : entry.getValue())
						dynOption.add(new Option(value));
					treeData.add(dynOption);
				}
			}
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
