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
import org.apache.pivot.collections.Sequence;
import org.apache.pivot.util.Resources;
import org.apache.pivot.wtk.Action;
import org.apache.pivot.wtk.CardPane;
import org.apache.pivot.wtk.Checkbox;
import org.apache.pivot.wtk.Component;
import org.apache.pivot.wtk.Dialog;
import org.apache.pivot.wtk.ListButton;
import org.apache.pivot.wtk.ListButtonSelectionListener;
import org.apache.pivot.wtk.ListView;
import org.apache.pivot.wtk.ListViewSelectionListener;
import org.apache.pivot.wtk.PushButton;
import org.apache.pivot.wtk.RadioButton;
import org.apache.pivot.wtk.Span;
import org.apache.pivot.wtk.TableView;
import org.apache.pivot.wtk.TableViewSelectionListener;
import org.apache.pivot.wtk.TableView.Column;
import org.apache.pivot.wtk.TableView.ColumnSequence;
import org.apache.pivot.wtk.TextInput;
import org.apache.pivot.wtk.content.ListItem;
import org.apache.pivot.wtk.content.TableViewCellRenderer;
import org.apache.pivot.wtk.content.TableViewRowEditor;
import org.fcitmuk.epihandy.Condition;
import org.fcitmuk.epihandy.EpihandyConstants;
import org.fcitmuk.epihandy.SkipRule;
import org.openxdata.designer.util.Form;
import org.openxdata.designer.util.Page;
import org.openxdata.designer.util.Question;
import org.openxdata.designer.util.StaticFieldMapping;

public class SkipRuleDialog extends Dialog implements Bindable {

	@BXML
	private Dialog skipRuleDialog;

	@BXML
	private PushButton skipRuleDialogSaveButton;

	@BXML
	private ListView skipRuleList;

	@BXML
	private ListView skipRuleTargetList;

	@BXML
	private ListButton skipRuleJunctionButton;

	@BXML
	private TableView skipRuleConditionTable;

	@BXML
	private RadioButton skipRuleShowRadioButton;

	@BXML
	private RadioButton skipRuleHideRadioButton;

	@BXML
	private RadioButton skipRuleEnableRadioButton;

	@BXML
	private RadioButton skipRuleDisableRadioButton;

	@BXML
	private PushButton skipRuleAddButton;

	@BXML
	private PushButton skipRuleDeleteButton;

	@BXML
	private PushButton skipRuleConditionAddButton;

	@BXML
	private PushButton skipRuleConditionDeleteButton;

	@BXML
	private Checkbox skipRuleRequireCheckbox;

	@BXML
	private CardPane skipRuleCardPane;

	private TableViewRowEditor conditionTableRowEditor;

	public void initialize(Map<String, Object> namespace, URL location,
			Resources resources) {

		skipRuleAddButton.setAction(new Action() {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void perform(Component source) {
				SkipRule newRule = new SkipRule();
				newRule.setConditions(new Vector());
				newRule.setActionTargets(new Vector());
				ListItem ruleItem = new ListItem("Rule "
						+ (skipRuleList.getListData().getLength() + 1));
				ruleItem.setUserData(newRule);
				List<ListItem> ruleList = (List<ListItem>) skipRuleList
						.getListData();
				ruleList.add(ruleItem);
			}
		});

		skipRuleDeleteButton.setAction(new Action() {
			@SuppressWarnings("unchecked")
			@Override
			public void perform(Component source) {
				ListItem selectedItem = (ListItem) skipRuleList
						.getSelectedItem();
				List<ListItem> ruleList = (List<ListItem>) skipRuleList
						.getListData();
				ruleList.remove(selectedItem);
			}
		});

		// Deletion shouldn't be possible until a rule is selected.
		skipRuleDeleteButton.getAction().setEnabled(false);

		skipRuleConditionAddButton.setAction(new Action() {
			@SuppressWarnings("unchecked")
			@Override
			public void perform(Component source) {
				List<Condition> tableData = (List<Condition>) skipRuleConditionTable
						.getTableData();
				SkipRule selectedRule = (SkipRule) ((ListItem) skipRuleList
						.getSelectedItem()).getUserData();
				selectedRule.getConditions().add(new Condition());
				tableData.add(new Condition());
			}
		});

		skipRuleConditionDeleteButton.setAction(new Action() {
			@SuppressWarnings("unchecked")
			@Override
			public void perform(Component source) {
				List<Condition> tableData = (List<Condition>) skipRuleConditionTable
						.getTableData();
				SkipRule selectedRule = (SkipRule) ((ListItem) skipRuleList
						.getSelectedItem()).getUserData();
				Condition selectedItem = (Condition) skipRuleConditionTable
						.getSelectedRow();
				selectedRule.getConditions().remove(selectedItem);
				tableData.remove(selectedItem);
			}
		});

		skipRuleConditionDeleteButton.getAction().setEnabled(false);

		skipRuleConditionTable.getTableViewSelectionListeners().add(
				new TableViewSelectionListener.Adapter() {
					@Override
					public void selectedRowChanged(TableView tableView,
							Object previousSelectedRow) {
						skipRuleConditionDeleteButton.getAction().setEnabled(
								tableView.getSelectedIndex() >= 0);
					}
				});

		StaticFieldMapping<Short> junctionBindMapping = new StaticFieldMapping<Short>(
				EpihandyConstants.class, "CONDITIONS_OPERATOR_",
				"conditionsOperator", null, resources);
		skipRuleJunctionButton.setSelectedItemKey("conditionsOperator");
		skipRuleJunctionButton.setSelectedItemBindMapping(junctionBindMapping);
		skipRuleJunctionButton.setListData(junctionBindMapping.getLabels());
		skipRuleJunctionButton.getListButtonSelectionListeners().add(
				new ListButtonSelectionListener.Adapter() {
					@Override
					public void selectedItemChanged(ListButton listButton,
							Object previousSelectedItem) {
						ListItem item = (ListItem) skipRuleList
								.getSelectedItem();
						if (item != null) {
							listButton.store(new BindContext((SkipRule) item
									.getUserData()));
						}
					}
				});

		skipRuleList.getListViewSelectionListeners().add(
				new ListViewSelectionListener.Adapter() {
					@Override
					public void selectedItemChanged(ListView listView,
							Object previousSelectedItem) {
						ListItem item = (ListItem) listView.getSelectedItem();
						if (item != null) {
							SkipRule selectedRule = (SkipRule) item
									.getUserData();
							updateForSelectedRule(selectedRule);
							skipRuleCardPane.setSelectedIndex(1);
						} else {
							skipRuleCardPane.setSelectedIndex(0);
						}
						skipRuleDeleteButton.getAction().setEnabled(
								item != null);
					}
				});

		skipRuleTargetList.getListViewSelectionListeners().add(
				new ListViewSelectionListener.Adapter() {
					@Override
					public void selectedRangesChanged(ListView listView,
							Sequence<Span> previousSelectedRanges) {
						ListItem item = (ListItem) skipRuleList
								.getSelectedItem();
						if (item != null) {
							SkipRule selectedRule = (SkipRule) item
									.getUserData();
							Vector<Short> selectedTargets = new Vector<Short>();
							for (int i = 0; i < listView.getSelectedItems()
									.getLength(); i++) {
								ListItem selectedItem = (ListItem) listView
										.getSelectedItems().get(i);
								selectedTargets.add(((Question) selectedItem
										.getUserData()).getId());
							}
							selectedRule.setActionTargets(selectedTargets);
						}
					};
				});

		Action actionAction = new Action() {
			@Override
			public void perform(Component source) {
				ListItem item = (ListItem) skipRuleList.getSelectedItem();
				SkipRule selectedRule = (SkipRule) item.getUserData();
				byte action = selectedRule.getAction();

				if (skipRuleShowRadioButton.isSelected())
					action |= EpihandyConstants.ACTION_SHOW;
				else
					action &= ~EpihandyConstants.ACTION_SHOW;

				if (skipRuleHideRadioButton.isSelected())
					action |= EpihandyConstants.ACTION_HIDE;
				else
					action &= ~EpihandyConstants.ACTION_HIDE;

				if (skipRuleEnableRadioButton.isSelected())
					action |= EpihandyConstants.ACTION_ENABLE;
				else
					action &= ~EpihandyConstants.ACTION_ENABLE;

				if (skipRuleDisableRadioButton.isSelected())
					action |= EpihandyConstants.ACTION_DISABLE;
				else
					action &= ~EpihandyConstants.ACTION_DISABLE;

				if (skipRuleRequireCheckbox.isSelected())
					action |= EpihandyConstants.ACTION_MAKE_MANDATORY;
				else
					action &= ~EpihandyConstants.ACTION_MAKE_MANDATORY;

				selectedRule.setAction(action);
			}
		};

		skipRuleShowRadioButton.setAction(actionAction);
		skipRuleHideRadioButton.setAction(actionAction);
		skipRuleEnableRadioButton.setAction(actionAction);
		skipRuleDisableRadioButton.setAction(actionAction);
		skipRuleRequireCheckbox.setAction(actionAction);

		// Set up cell editors for the conditions
		conditionTableRowEditor = new TableViewRowEditor();

		ListButton questionList = new ListButton();
		questionList.setSelectedItemBindMapping(new ListView.ItemBindMapping() {

			public int indexOf(List<?> listData, Object value) {
				Short questionId = (Short) value;

				if (questionId != null)
					for (int i = 0; i < listData.getLength(); i++) {
						ListItem item = (ListItem) listData.get(i);
						if (((Question) item.getUserData()).getId() == questionId)
							return i;
					}
				return -1;
			}

			public Object get(List<?> listData, int index) {
				ListItem item = (ListItem) listData.get(index);
				Question q = (Question) item.getUserData();
				return q.getId();
			}
		});
		questionList.setSelectedItemKey("questionId");

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

		TextInput secondValueInput = new TextInput();
		secondValueInput.setTextKey("secondValue");

		// Install cell editors for condition table
		conditionTableRowEditor.getCellEditors()
				.put("questionId", questionList);
		conditionTableRowEditor.getCellEditors().put("operator", operatorList);
		conditionTableRowEditor.getCellEditors().put("function", functionList);
		conditionTableRowEditor.getCellEditors().put("value", valueInput);
		conditionTableRowEditor.getCellEditors().put("secondValue",
				secondValueInput);
		skipRuleConditionTable.setRowEditor(conditionTableRowEditor);

		// Install cell renderers for condition table
		ColumnSequence columns = skipRuleConditionTable.getColumns();
		for (Column column : columns) {

			if ("questionId".equals(column.getName())) {
				column.setCellRenderer(new TableViewCellRenderer() {
					@Override
					public void render(Object row, int rowIndex,
							int columnIndex, TableView tableView,
							String columnName, boolean selected,
							boolean highlighted, boolean disabled) {

						renderStyles(tableView, selected, disabled);

						String text = null;
						if (row != null && columnName != null) {
							text = toString(row, columnName);
							Question q = (Question) getForm().getQuestion(
									Short.parseShort(text));
							text = q == null ? "Select Question" : q.getText();
						}

						setText(text);
					}
				});
			}

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

		skipRuleDialogSaveButton.setAction(new Action() {
			@Override
			public void perform(Component source) {
				storeRules();
				skipRuleDialog.close();
			}
		});
	}

	public static class BindContext extends BeanAdapter {

		public BindContext(SkipRule rule) {
			super(rule);
		}

		// TODO: OMG clean this up
		public Object put(String key, Object value) {
			if ("conditionsOperator".equals(key)) {
				Byte byteValue = (Byte) value;
				return super
						.put(key,
								byteValue != null ? byteValue.shortValue()
										: (short) EpihandyConstants.CONDITIONS_OPERATOR_NULL);
			} else if ("action".equals(key)) {
				Byte byteValue = (Byte) value;
				return super.put(key, byteValue != null ? byteValue
						: EpihandyConstants.ACTION_NONE);
			} else if ("actionShow".equals(key)) {
				Byte actionValue = (Byte) super.get("action");
				Boolean selected = (Boolean) super.get("actionShow");
				if (selected)
					super.put(
							"action",
							(byte) (actionValue | EpihandyConstants.ACTION_SHOW));
				else
					super.put("action", actionValue);
			} else if ("actionHide".equals(key)) {
				Byte actionValue = (Byte) super.get("action");
				Boolean selected = (Boolean) super.get("actionHide");
				if (selected)
					return super
							.put("action",
									(byte) (actionValue | EpihandyConstants.ACTION_HIDE));
				else
					super.put("action", actionValue);
			} else if ("actionEnable".equals(key)) {
				Byte actionValue = (Byte) super.get("action");
				Boolean selected = (Boolean) super.get("actionEnable");
				if (selected)
					return super
							.put("action",
									(byte) (actionValue | EpihandyConstants.ACTION_ENABLE));
				else
					return super.put("action", actionValue);
			} else if ("actionDisable".equals(key)) {
				Byte actionValue = (Byte) super.get("action");
				Boolean selected = (Boolean) super.get("actionDisable");
				if (selected)
					return super
							.put("action",
									(byte) (actionValue | EpihandyConstants.ACTION_DISABLE));
				else
					return super.put("action", actionValue);

			} else if ("actionRequire".equals(key)) {
				Byte actionValue = (Byte) super.get("action");
				Boolean selected = (Boolean) super.get("actionRequire");
				if (selected)
					return super
							.put("action",
									(byte) (actionValue | EpihandyConstants.ACTION_MAKE_MANDATORY));
				else
					return super.put("action", actionValue);
			}
			return super.put(key, value);
		}

		@Override
		public Object get(String key) {
			if ("conditionsOperator".equals(key)) {
				Short shortValue = (Short) super.get(key);
				return shortValue != null ? shortValue.byteValue()
						: (byte) EpihandyConstants.CONDITIONS_OPERATOR_NULL;
			} else if ("action".equals(key)) {
				Byte byteValue = (Byte) super.get(key);
				return byteValue != null ? byteValue
						: EpihandyConstants.ACTION_NONE;
			} else if ("actionShow".equals(key)) {
				Byte byteValue = (Byte) super.get("action");
				boolean selected = (byteValue & EpihandyConstants.ACTION_SHOW) > 0;
				return selected;
			} else if ("actionHide".equals(key)) {
				Byte byteValue = (Byte) super.get("action");
				boolean selected = (byteValue & EpihandyConstants.ACTION_HIDE) > 0;
				return selected;
			} else if ("actionEnable".equals(key)) {
				Byte byteValue = (Byte) super.get("action");
				boolean selected = (byteValue & EpihandyConstants.ACTION_ENABLE) > 0;
				return selected;
			} else if ("actionDisable".equals(key)) {
				Byte byteValue = (Byte) super.get("action");
				boolean selected = (byteValue & EpihandyConstants.ACTION_DISABLE) > 0;
				return selected;
			} else if ("actionRequire".equals(key)) {
				Byte byteValue = (Byte) super.get("action");
				boolean selected = (byteValue & EpihandyConstants.ACTION_MAKE_MANDATORY) > 0;
				return selected;
			}
			return super.get(key);
		}

		@Override
		public boolean containsKey(String key) {
			Set<String> virtualProperties = new HashSet<String>();
			virtualProperties.add("actionShow");
			virtualProperties.add("actionHide");
			virtualProperties.add("actionEnable");
			virtualProperties.add("actionDisable");
			virtualProperties.add("actionRequire");
			return virtualProperties.contains(key) || super.containsKey(key);
		}
	}

	@SuppressWarnings({ "unchecked" })
	private void updateForSelectedRule(SkipRule selectedRule) {

		Vector<Condition> conditions = selectedRule.getConditions();
		Vector<Short> targetQuestionIds = selectedRule.getActionTargets();

		BindContext context = new BindContext(selectedRule);
		load(context);

		List<ListItem> targetList = (List<ListItem>) skipRuleTargetList
				.getListData();
		List<Condition> conditionList = (List<Condition>) skipRuleConditionTable
				.getTableData();

		if (targetQuestionIds != null) {
			Sequence<Object> selectedTargets = new ArrayList<Object>();
			for (ListItem targetItem : targetList) {
				Question candidateTarget = (Question) targetItem.getUserData();
				if (targetQuestionIds.contains(candidateTarget.getId()))
					selectedTargets.add(targetItem);
			}
			skipRuleTargetList.setSelectedItems(selectedTargets);
		}

		conditionList.clear();
		if (conditions != null) {
			for (Condition condition : conditions) {
				conditionList.add(condition);
			}
		}
	}

	private Form getForm() {
		Form form = (Form) getUserData().get("activeForm");
		return form;
	}

	@SuppressWarnings("unchecked")
	public void loadRules() {

		List<ListItem> skipRuleData = (List<ListItem>) skipRuleList
				.getListData();

		skipRuleData.clear();

		Vector<SkipRule> rules = (Vector<SkipRule>) getForm().getSkipRules();

		int ruleCount = 1;
		for (SkipRule rule : rules) {
			ListItem item = new ListItem("Rule " + ruleCount++);
			item.setUserData(new SkipRule(rule));
			skipRuleData.add(item);
		}

		List<ListItem> skipRuleTargetData = (List<ListItem>) skipRuleTargetList
				.getListData();

		skipRuleTargetData.clear();

		for (Page page : getForm())
			for (Question question : page) {
				ListItem item = new ListItem(question.getText());
				item.setUserData(question);
				skipRuleTargetData.add(item);
			}

		ListButton questionIdButton = (ListButton) conditionTableRowEditor
				.getCellEditors().get("questionId");
		questionIdButton.setListData(skipRuleTargetData);
	}

	@SuppressWarnings("unchecked")
	public void storeRules() {

		Form form = (Form) getUserData().get("activeForm");
		List<ListItem> skipRuleData = (List<ListItem>) skipRuleList
				.getListData();

		Vector<SkipRule> rules = (Vector<SkipRule>) form.getSkipRules();
		rules.clear();

		for (ListItem item : skipRuleData)
			rules.add((SkipRule) item.getUserData());
	}
}
