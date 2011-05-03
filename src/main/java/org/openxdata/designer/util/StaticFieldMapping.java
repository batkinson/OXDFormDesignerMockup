package org.openxdata.designer.util;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.pivot.collections.ArrayList;
import org.apache.pivot.collections.List;
import org.apache.pivot.util.Resources;
import org.apache.pivot.wtk.ListView;

/**
 * Mapping for static values used commonly for constant declarations.
 * 
 * @author brent
 * 
 * @param <T>
 */
public class StaticFieldMapping<T> implements ListView.ListDataBindMapping,
		ListView.ItemBindMapping {

	private final Map<String, T> valueMap = new LinkedHashMap<String, T>();
	private final Map<String, String> labelMap = new LinkedHashMap<String, String>();

	@SuppressWarnings("unchecked")
	public StaticFieldMapping(Class<?> constClass, String fieldPrefix,
			String labelPrefix, Set<String> skippedSuffixes, Resources resources) {

		for (Field field : constClass.getFields()) {
			String fieldName = field.getName();
			if (fieldName.startsWith(fieldPrefix)) {

				String name = fieldName.replace(fieldPrefix, "");

				if (skippedSuffixes != null && skippedSuffixes.contains(name))
					continue;

				name = name.toLowerCase();

				try {
					T value = (T) field.get(constClass);
					String camelCaseName = Util
							.convertUnderscoreToCamelCase(name);
					String initLetter = camelCaseName.substring(0, 1);
					String resourceName = MessageFormat.format(labelPrefix
							+ "{0}Label", initLetter.toUpperCase()
							+ camelCaseName.substring(1));
					valueMap.put(resourceName, value);
				} catch (Exception e) {
					System.err.println("Failed to load " + name + ": "
							+ e.getLocalizedMessage());
				}
			}

			// Populate the label mappings using resources
			for (String labelName : valueMap.keySet()) {
				if (resources.containsKey(labelName)) {
					String label = (String) resources.get(labelName);
					labelMap.put(label, labelName);
				} else
					labelMap.put(labelName, labelName);
			}
		}

	}

	public List<String> getValueNames() {
		List<String> valuesList = new ArrayList<String>();
		for (String valueName : valueMap.keySet())
			valuesList.add(valueName);
		return valuesList;
	}

	public List<String> getLabels() {
		List<String> labelList = new ArrayList<String>();
		for (String typeLabel : labelMap.keySet())
			labelList.add(typeLabel);
		return labelList;
	}

	private String getNameForValue(T value) {
		for (Map.Entry<String, T> entry : valueMap.entrySet()) {
			if (entry.getValue().equals(value)) {
				return entry.getKey();
			}
		}
		return null;
	}

	private String getLabelForName(String typeName) {
		for (Map.Entry<String, String> entry : labelMap.entrySet()) {
			if (entry.getValue().equals(typeName)) {
				return entry.getKey();
			}
		}
		return null;
	}

	private String getLabelForValue(T value) {
		String result = null;
		String typeName = getNameForValue(value);
		if (typeName != null) {
			result = getLabelForName(typeName);
		}
		return result;
	}

	private T getValueForLabel(String label) {
		T result = null;
		if (labelMap.containsKey(label)) {
			String type = labelMap.get(label);
			if (valueMap.containsKey(type)) {
				result = valueMap.get(type);
			}
		}
		return result;
	}

	/**
	 * Should get a list of byte values, want to return a list of labels.
	 */
	@SuppressWarnings("unchecked")
	public List<?> toListData(Object value) {
		List<Object> labelList = new ArrayList<Object>();
		for (T item : (List<T>) value) {
			T castValue = (T) item;
			String label = getLabelForValue(castValue);
			if (label != null)
				labelList.add(label);
		}
		return labelList;
	}

	/**
	 * Should get a list of labels, want to return a list of values.
	 */
	public Object valueOf(List<?> listData) {
		List<Object> valueList = new ArrayList<Object>();
		for (Object item : listData) {
			if (item instanceof String && labelMap.containsKey(item)) {
				valueList.add(labelMap.get(item.toString()));
			}
		}
		return valueList;
	}

	/**
	 * Take the value and return the position in the list.
	 */
	@SuppressWarnings("unchecked")
	public int indexOf(List<?> listData, Object value) {
		List<String> labelList = (List<String>) listData;
		T byteValue = (T) value;
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
