package org.openxdata.modelutils;

import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.fcitmuk.epihandy.Condition;
import org.fcitmuk.epihandy.DynamicOptionDef;
import org.fcitmuk.epihandy.FormDef;
import org.fcitmuk.epihandy.QuestionDef;
import org.fcitmuk.epihandy.SkipRule;
import org.fcitmuk.epihandy.ValidationRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormUtils {

	private static Logger log = LoggerFactory.getLogger(FormUtils.class);

	/**
	 * Useful to clean up references to an old form variable name after it is
	 * updated. It *does not* update the value in the form itself. This is
	 * provided as a utility, but the data model should guarantee this when
	 * setting the method. If anything, this is precise documentation of the
	 * necessary updates that it should include.
	 * 
	 * @param form
	 *            the form to update.
	 * @param newName
	 *            the new bind variable name for the form.
	 */
	@SuppressWarnings("unchecked")
	public static void updateFormVarName(FormDef form, String oldName,
			String newName) {

		if (newName == null)
			throw new IllegalArgumentException(
					"form requires a non-null variable name");

		// Use varName/ in attempt to avoid replacing non-variable refs
		String varRegex = String.format("/?%s/", oldName);
		String varReplacement = String.format("/%s/", newName);

		// Update the description template
		String dt = form.getDescriptionTemplate();
		if (dt != null)
			form.setDescriptionTemplate(dt.replaceAll(varRegex, varReplacement));

		// Update questions
		QuestionTree qTree = QuestionTree.constructTreeFromFormDef(form);
		updateFormVarName(qTree, varRegex, varReplacement);

		// Update the validation rule condition operands
		Vector<ValidationRule> vrules = (Vector<ValidationRule>) form
				.getValidationRules();
		if (vrules != null)
			for (ValidationRule vr : vrules) {
				for (Condition vc : (Vector<Condition>) vr.getConditions()) {
					String value = vc.getValue();
					if (value != null) {
						vc.setValue(value
								.replaceFirst(varRegex, varReplacement));
					}
					String secondValue = vc.getSecondValue();
					if (secondValue != null) {
						vc.setSecondValue(secondValue.replaceFirst(varRegex,
								varReplacement));
					}
				}
			}

		// Update the skip rule condition operands
		Vector<SkipRule> srules = (Vector<SkipRule>) form.getSkipRules();
		if (srules != null)
			for (SkipRule sr : srules) {
				for (Condition sc : (Vector<Condition>) sr.getConditions()) {
					String value = sc.getValue();
					if (value != null) {
						sc.setValue(value
								.replaceFirst(varRegex, varReplacement));
					}
					String secondValue = sc.getSecondValue();
					if (secondValue != null) {
						sc.setSecondValue(secondValue.replaceFirst(varRegex,
								varReplacement));
					}
				}
			}
	}

	private static void updateFormVarName(QuestionTree tree,
			String matchPattern, String replacement) {

		// If the question has child questions, update them
		if (!tree.isLeaf()) {
			for (QuestionTree childTree : tree.getChildren())
				updateFormVarName(childTree, matchPattern, replacement);
		}

		// Root doesn't contain a question, no-op
		if (tree.isRoot())
			return;

		// Update the question...
		QuestionDef q = tree.getQuestion();

		String text = q.getText();
		if (text != null)
			q.setText(text.replaceAll(matchPattern, replacement));

		String varName = q.getVariableName();
		if (varName != null)
			q.setVariableName(varName.replaceFirst(matchPattern, replacement));
	}

	/**
	 * Used to update references to old question ids when they are changed. This
	 * method doesn't change the question ids, just the references to them.
	 * 
	 * @param form
	 *            the form to update
	 * @param renamedIdMap
	 *            map of old->new question ids.
	 */
	@SuppressWarnings("unchecked")
	public static void changeQuestionIds(FormDef form,
			Map<Short, Short> renamedIdMap) {

		Hashtable<Short, DynamicOptionDef> dynOptionMap = form
				.getDynamicOptions();
		if (dynOptionMap != null) {
			if (log.isDebugEnabled())
				log.debug("remapping dynamic options after qid changes");
			Hashtable<Short, DynamicOptionDef> renamedOptionMap = new Hashtable<Short, DynamicOptionDef>();
			for (Map.Entry<Short, DynamicOptionDef> entry : dynOptionMap
					.entrySet()) {

				Short origParentId = entry.getKey();
				DynamicOptionDef optionDef = entry.getValue();
				Short origChildId = optionDef.getQuestionId();

				boolean parentMoved = renamedIdMap.containsKey(origParentId);
				boolean childMoved = renamedIdMap.containsKey(origChildId);

				Short newSourceId = origParentId, newTargetId = origChildId;

				if (parentMoved && childMoved) {
					newSourceId = renamedIdMap.get(origParentId);
					newTargetId = renamedIdMap.get(origChildId);
					optionDef.setQuestionId(newTargetId);
					renamedOptionMap.put(newSourceId, optionDef);
				} else if (parentMoved) {
					newSourceId = renamedIdMap.get(origParentId);
					renamedOptionMap.put(newSourceId, optionDef);
				} else if (childMoved) {
					newTargetId = renamedIdMap.get(origChildId);
					optionDef.setQuestionId(newTargetId);
				} else {
					renamedOptionMap.put(origParentId, optionDef);
				}
				if (log.isDebugEnabled())
					log.debug(form.getQuestion(origParentId) + "<-"
							+ form.getQuestion(origChildId) + " became "
							+ form.getQuestion(newSourceId) + "<-"
							+ form.getQuestion(newTargetId));
			}
			form.setDynamicOptions(renamedOptionMap);
		}

		log.debug("patching up validation rules");
		for (ValidationRule validationRule : (Vector<ValidationRule>) form
				.getValidationRules()) {
			Short origId = validationRule.getQuestionId();
			if (renamedIdMap.keySet().contains(origId)) {
				Short newId = renamedIdMap.get(origId);
				validationRule.setQuestionId(newId);

				if (log.isDebugEnabled())
					log.debug(origId + " became " + newId);

				log.debug("patching condition references");
				for (Condition condition : (Vector<Condition>) validationRule
						.getConditions()) {
					Short origCondId = condition.getQuestionId();
					if (renamedIdMap.keySet().contains(origCondId)) {
						Short newCondId = renamedIdMap.get(origCondId);
						condition.setQuestionId(newCondId);
						if (log.isDebugEnabled())
							log.debug(origCondId + " became " + newCondId);
					}
				}
			}
		}

		log.debug("patching up skip rules");
		for (SkipRule skipRule : (Vector<SkipRule>) form.getSkipRules()) {
			log.debug("patching up skip rule conditions");
			for (Condition condition : (Vector<Condition>) skipRule
					.getConditions()) {
				Short origCondId = condition.getQuestionId();
				if (renamedIdMap.containsKey(origCondId)) {
					Short newCondId = renamedIdMap.get(origCondId);
					condition.setQuestionId(newCondId);
					if (log.isDebugEnabled())
						log.debug(origCondId + " became " + newCondId);
				}
			}

			Vector<Short> actionTargets = skipRule.getActionTargets();
			if (log.isDebugEnabled())
				log.debug("patching up skip rules targets: " + actionTargets);
			for (int i = 0; i < actionTargets.size(); i++) {
				if (renamedIdMap.containsKey(actionTargets.get(i))) {
					actionTargets
							.set(i, renamedIdMap.get(actionTargets.get(i)));
				}
			}
			if (log.isDebugEnabled())
				log.debug("targets became: " + actionTargets);
		}
	}
}
