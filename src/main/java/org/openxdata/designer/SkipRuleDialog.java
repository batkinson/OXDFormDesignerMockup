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
import org.apache.pivot.wtk.content.ListItem;
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
	private Checkbox skipRuleRequireCheckbox;

	public void initialize(Map<String, Object> namespace, URL location,
			Resources resources) {

		StaticFieldMapping<Short> junctionBindMapping = new StaticFieldMapping<Short>(
				EpihandyConstants.class, "CONDITIONS_OPERATOR_",
				"skipRuleConditionOperator", null, resources);
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
							listButton.store(new BeanAdapter(item.getUserData()));
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
						}
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

		Sequence<Object> selectedTargets = new ArrayList<Object>();
		for (ListItem targetItem : targetList) {
			Question candidateTarget = (Question) targetItem.getUserData();
			if (targetQuestionIds.contains(candidateTarget.getId()))
				selectedTargets.add(targetItem);
		}
		skipRuleTargetList.setSelectedItems(selectedTargets);

		conditionList.clear();
		for (Condition condition : conditions) {
			conditionList.add(new Condition(condition));
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

		// Select first rule to update UI via event handlers
		skipRuleList.setSelectedIndex(0);
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
