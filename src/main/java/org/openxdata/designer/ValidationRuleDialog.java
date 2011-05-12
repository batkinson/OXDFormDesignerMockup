package org.openxdata.designer;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.apache.pivot.beans.BXML;
import org.apache.pivot.beans.BeanAdapter;
import org.apache.pivot.beans.Bindable;
import org.apache.pivot.collections.ArrayList;
import org.apache.pivot.collections.List;
import org.apache.pivot.collections.Map;
import org.apache.pivot.util.Resources;
import org.apache.pivot.wtk.Action;
import org.apache.pivot.wtk.Component;
import org.apache.pivot.wtk.Dialog;
import org.apache.pivot.wtk.Label;
import org.apache.pivot.wtk.ListButton;
import org.apache.pivot.wtk.PushButton;
import org.apache.pivot.wtk.TableView;
import org.apache.pivot.wtk.TableView.Column;
import org.apache.pivot.wtk.TableView.ColumnSequence;
import org.apache.pivot.wtk.TableViewSelectionListener;
import org.apache.pivot.wtk.TextInput;
import org.apache.pivot.wtk.content.TableViewCellRenderer;
import org.apache.pivot.wtk.content.TableViewRowEditor;
import org.fcitmuk.epihandy.Condition;
import org.fcitmuk.epihandy.EpihandyConstants;
import org.fcitmuk.epihandy.ValidationRule;
import org.openxdata.designer.util.Form;
import org.openxdata.designer.util.Question;
import org.openxdata.designer.util.StaticFieldMapping;

public class ValidationRuleDialog extends Dialog implements Bindable {

	@BXML
	private Label validationRuleQuestionText;

	@BXML
	private ListButton validationRuleJunctionButton;

	@BXML
	private TableView validationRuleConditionTable;

	@BXML
	private PushButton validationRuleConditionAddButton;

	@BXML
	private PushButton validationRuleConditionDeleteButton;

	@BXML
	private PushButton validationRuleDialogSaveButton;

	@Override
	public void initialize(Map<String, Object> namespace, URL location,
			Resources resources) {

		StaticFieldMapping<Byte> junctionBindMapping = new StaticFieldMapping<Byte>(
				EpihandyConstants.class, "CONDITIONS_OPERATOR_",
				"conditionsOperator", null, resources);
		validationRuleJunctionButton.setSelectedItemKey("conditionsOperator");
		validationRuleJunctionButton
				.setSelectedItemBindMapping(junctionBindMapping);
		validationRuleJunctionButton.setListData(junctionBindMapping
				.getLabels());

		String[] skippedOperators = { "NULL", "IN_LIST", "NOT_IN_LIST",
				"IS_NULL", "IS_NOT_NULL" };
		Set<String> skippedOperatorSet = new HashSet<String>();
		for (String op : skippedOperators)
			skippedOperatorSet.add(op);
		final StaticFieldMapping<Byte> operatorBindMapping = new StaticFieldMapping<Byte>(
				EpihandyConstants.class, "OPERATOR_", "conditionOperator",
				skippedOperatorSet, resources);
		ListButton operatorList = new ListButton();
		operatorList.setSelectedItemBindMapping(operatorBindMapping);
		operatorList.setListData(operatorBindMapping.getLabels());
		operatorList.setSelectedItemKey("operator");

		final StaticFieldMapping<Byte> functionBindMapping = new StaticFieldMapping<Byte>(
				EpihandyConstants.class, "FUNCTION_", "conditionFunction",
				null, resources);
		ListButton functionList = new ListButton();
		functionList.setSelectedItemBindMapping(functionBindMapping);
		functionList.setListData(functionBindMapping.getLabels());
		functionList.setSelectedItemKey("function");

		TextInput valueInput = new TextInput();
		valueInput.setTextKey("value");

		TableViewRowEditor rowEditor = new TableViewRowEditor();

		rowEditor.getCellEditors().put("operator", operatorList);
		rowEditor.getCellEditors().put("function", functionList);
		rowEditor.getCellEditors().put("value", valueInput);
		validationRuleConditionTable.setRowEditor(rowEditor);

		// Install cell renderers for condition table
		ColumnSequence columns = validationRuleConditionTable.getColumns();
		for (Column column : columns) {

			if ("operator".equals(column.getName())) {
				column.setCellRenderer(new TableViewCellRenderer() {
					public void render(Object row, int rowIndex,
							int columnIndex, TableView tableView,
							String columnName, boolean selected,
							boolean highlighted, boolean disabled) {

						renderStyles(tableView, selected, disabled);

						String text = null;
						if (row != null && columnName != null) {
							text = toString(row, columnName);
							text = operatorBindMapping.getLabelForValue(Byte
									.parseByte(text));
						}

						setText(text);
					}
				});
			}

			if ("function".equals(column.getName())) {
				column.setCellRenderer(new TableViewCellRenderer() {
					public void render(Object row, int rowIndex,
							int columnIndex, TableView tableView,
							String columnName, boolean selected,
							boolean highlighted, boolean disabled) {

						renderStyles(tableView, selected, disabled);

						String text = null;
						if (row != null && columnName != null) {
							text = toString(row, columnName);
							text = functionBindMapping.getLabelForValue(Byte
									.parseByte(text));
						}

						setText(text);
					}
				});
			}
		}

		validationRuleConditionAddButton.setAction(new Action() {
			@SuppressWarnings("unchecked")
			@Override
			public void perform(Component source) {
				List<Condition> tableData = (List<Condition>) validationRuleConditionTable
						.getTableData();
				Condition newCondition = new Condition();
				newCondition.setQuestionId(getQuestion().getId());
				tableData.add(newCondition);
			}
		});

		validationRuleConditionDeleteButton.setAction(new Action() {
			@SuppressWarnings("unchecked")
			@Override
			public void perform(Component source) {
				List<Condition> tableData = (List<Condition>) validationRuleConditionTable
						.getTableData();
				Condition selectedCondition = (Condition) validationRuleConditionTable
						.getSelectedRow();
				tableData.remove(selectedCondition);
			}
		});

		validationRuleConditionDeleteButton.getAction().setEnabled(false);

		validationRuleConditionTable.getTableViewSelectionListeners().add(
				new TableViewSelectionListener.Adapter() {
					@Override
					public void selectedRowChanged(TableView tableView,
							Object previousSelectedRow) {
						validationRuleConditionDeleteButton.getAction()
								.setEnabled(tableView.getSelectedIndex() >= 0);
					}
				});

		validationRuleDialogSaveButton.setAction(new Action() {
			@Override
			public void perform(Component source) {
				storeRule();
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

	class BindContext extends BeanAdapter {

		public BindContext(Object bean) {
			super(bean);
		}

		@Override
		public Object get(String key) {
			if ("conditionsOperator".equals(key)) {
				Integer intValue = (Integer) super.get(key);
				return intValue != null ? intValue.byteValue()
						: (byte) EpihandyConstants.CONDITIONS_OPERATOR_NULL;
			}
			return super.get(key);
		}

		@Override
		public Object put(String key, Object value) {
			if ("conditionsOperator".equals(key)) {
				Byte byteValue = (Byte) value;
				return super.put(key, byteValue != null ? byteValue.intValue()
						: (byte) EpihandyConstants.CONDITIONS_OPERATOR_NULL);
			}
			return super.put(key, value);
		}
	}

	public void loadRule() {

		ValidationRule rule = getForm()
				.getValidationRule(getQuestion().getId());

		if (rule == null)
			rule = new ValidationRule(getQuestion().getId(),
					new Vector<Condition>(), "Please enter an error message");

		validationRuleQuestionText.setText(getQuestion().getText());

		List<Condition> conditions = new ArrayList<Condition>();
		for (int i = 0; i < rule.getConditionCount(); i++)
			conditions.add((Condition) rule.getConditions().get(i));
		validationRuleConditionTable.setTableData(conditions);

		load(new BindContext(rule));
	}

	@SuppressWarnings("unchecked")
	public void storeRule() {
		ValidationRule rule = getForm()
				.getValidationRule(getQuestion().getId());
		if (rule == null) {
			rule = new ValidationRule();
			rule.setQuestionId(getQuestion().getId());
			getForm().getValidationRules().add(rule);
		}

		Vector<Condition> conditions = new Vector<Condition>();
		for (Condition condition : (List<Condition>) validationRuleConditionTable
				.getTableData())
			conditions.add(condition);
		rule.setConditions(conditions);

		store(new BindContext(rule));
	}
}
