package org.openxdata.designer.designtree;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pivot.collections.ArrayList;
import org.apache.pivot.collections.List;
import org.apache.pivot.util.Resources;
import org.apache.pivot.wtk.ListView;
import org.fcitmuk.epihandy.QuestionDef;

public class QuestionTypeMapping implements ListView.ItemBindMapping,
		ListView.ListDataBindMapping {

	private final Map<String, Byte> questionValues = new LinkedHashMap<String, Byte>();
	private final Map<String, String> typeLabels = new LinkedHashMap<String, String>();
	private final String QUESTION_TYPE_FIELD_PREFIX = "QTN_TYPE_";
	private static final Pattern uscorePattern = Pattern.compile("_(\\w)");

	public QuestionTypeMapping(Resources resources) {

		// Populate the values based on the QuestionDef static fields
		for (Field field : QuestionDef.class.getFields()) {
			if (field.getName().startsWith(QUESTION_TYPE_FIELD_PREFIX)) {
				String name = field.getName()
						.replace(QUESTION_TYPE_FIELD_PREFIX, "").toLowerCase();
				try {
					Byte value = (Byte) field.get(QuestionDef.class);
					String camelCaseTypeName = convertToCamelCase(name);
					String initLetter = camelCaseTypeName.substring(0, 1);
					String resourceName = MessageFormat.format(
							"questionType{0}Label", initLetter.toUpperCase()
									+ camelCaseTypeName.substring(1));
					questionValues.put(resourceName, value);
				} catch (Exception e) {
					System.err.println("Failed to load question type " + name
							+ ": " + e.getLocalizedMessage());
				}
			}
		}

		// Populate the label mappings using resources
		for (String typeName : questionValues.keySet()) {
			if (resources.containsKey(typeName)) {
				String label = (String) resources.get(typeName);
				typeLabels.put(label, typeName);
			} else
				typeLabels.put(typeName, typeName);
		}
	}

	public List<String> getValueNames() {
		List<String> valuesList = new ArrayList<String>();
		for (String valueName : questionValues.keySet())
			valuesList.add(valueName);
		return valuesList;
	}

	public List<String> getTypeLabels() {
		List<String> labelList = new ArrayList<String>();
		for (String typeLabel : typeLabels.keySet())
			labelList.add(typeLabel);
		return labelList;
	}

	private String getNameForValue(Byte value) {
		for (Map.Entry<String, Byte> entry : questionValues.entrySet()) {
			if (entry.getValue().equals(value)) {
				return entry.getKey();
			}
		}
		return null;
	}

	private String getLabelForName(String typeName) {
		for (Map.Entry<String, String> entry : typeLabels.entrySet()) {
			if (entry.getValue().equals(typeName)) {
				return entry.getKey();
			}
		}
		return null;
	}

	private String getLabelForValue(Byte value) {
		String result = null;
		String typeName = getNameForValue(value);
		if (typeName != null) {
			result = getLabelForName(typeName);
		}
		return result;
	}

	private Byte getValueForLabel(String label) {
		Byte result = null;
		if (typeLabels.containsKey(label)) {
			String type = typeLabels.get(label);
			if (questionValues.containsKey(type)) {
				result = questionValues.get(type);
			}
		}
		return result;
	}

	private static String convertToCamelCase(String name) {
		StringBuffer camelCaseName = new StringBuffer();
		Matcher m = uscorePattern.matcher(name);
		while (m.find()) {
			m.appendReplacement(camelCaseName, m.group(1).toUpperCase());
		}
		m.appendTail(camelCaseName);
		return camelCaseName.toString();
	}

	/**
	 * Should get a list of byte values, want to return a list of labels.
	 */
	public List<?> toListData(Object value) {
		List<Object> labelList = new ArrayList<Object>();
		for (Object item : (List<?>) value) {
			if (item instanceof Byte) {
				Byte byteValue = (Byte) item;
				String label = getLabelForValue(byteValue);
				if (label != null)
					labelList.add(label);
			}
		}
		return labelList;
	}

	/**
	 * Should get a list of labels, want to return a list of values.
	 */
	public Object valueOf(List<?> listData) {
		List<Object> valueList = new ArrayList<Object>();
		for (Object item : listData) {
			if (item instanceof String && typeLabels.containsKey(item)) {
				valueList.add(typeLabels.get(item.toString()));
			}
		}
		return valueList;
	}

	/**
	 * Take the value and return the position in the list.
	 */
	public int indexOf(List<?> listData, Object value) {
		@SuppressWarnings("unchecked")
		List<String> labelList = (List<String>) listData;
		Byte byteValue = (Byte) value;
		String labelName = getLabelForValue(byteValue);
		return labelList.indexOf(labelName);
	}

	/**
	 * Take the index of selection and translate into an assignable value.
	 */
	public Object get(List<?> listData, int index) {
		@SuppressWarnings("unchecked")
		List<String> labelList = (List<String>) listData;
		String label = labelList.get(index);
		return getValueForLabel(label);
	}
}
