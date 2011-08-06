package org.openxdata.modelutils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.fcitmuk.epihandy.DynamicOptionDef;
import org.fcitmuk.epihandy.FormDef;
import org.fcitmuk.epihandy.OptionDef;
import org.fcitmuk.epihandy.QuestionDef;

public class OptionUtils {

	@SuppressWarnings("unchecked")
	public static Map<Short, QuestionDef> getDynOptDepMap(FormDef formDef) {
		Map<Short, QuestionDef> dynOptDepMap = new HashMap<Short, QuestionDef>();
		if (formDef.getDynamicOptions() != null) {
			for (Map.Entry<Short, DynamicOptionDef> dynOptEntry : (Set<Map.Entry<Short, DynamicOptionDef>>) formDef
					.getDynamicOptions().entrySet()) {
				Short parentId = dynOptEntry.getKey();
				Short childId = dynOptEntry.getValue().getQuestionId();
				dynOptDepMap.put(childId, formDef.getQuestion(parentId));
			}
		}
		return dynOptDepMap;
	}

	@SuppressWarnings("unchecked")
	public static Map<Short, OptionDef> getPossibleValues(FormDef form,
			QuestionDef question, QuestionDef parentQuestion) {
		Map<Short, OptionDef> valuesById = new HashMap<Short, OptionDef>();
		byte questionType = question.getType();
		if (questionType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE
				|| questionType == QuestionDef.QTN_TYPE_LIST_MULTIPLE) {
			for (OptionDef option : (Vector<OptionDef>) question.getOptions())
				valuesById.put(option.getId(), option);
		} else if (questionType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC) {
			Map<Short, Vector<OptionDef>> optMap = (Map<Short, Vector<OptionDef>>) form
					.getDynamicOptions(parentQuestion.getId())
					.getParentToChildOptions();
			for (Map.Entry<Short, Vector<OptionDef>> entry : optMap.entrySet())
				for (OptionDef option : entry.getValue())
					valuesById.put(option.getId(), option);
		}
		return valuesById;
	}
}
