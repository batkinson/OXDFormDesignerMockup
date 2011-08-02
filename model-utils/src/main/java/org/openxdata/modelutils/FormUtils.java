package org.openxdata.modelutils;

import java.util.Vector;

import org.fcitmuk.epihandy.Condition;
import org.fcitmuk.epihandy.FormDef;
import org.fcitmuk.epihandy.QuestionDef;
import org.fcitmuk.epihandy.SkipRule;
import org.fcitmuk.epihandy.ValidationRule;

public class FormUtils {

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
}
