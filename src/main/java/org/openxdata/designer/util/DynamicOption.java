package org.openxdata.designer.util;

import org.apache.pivot.collections.ArrayList;
import org.apache.pivot.collections.List;

public class DynamicOption extends ArrayList<Option> implements List<Option> {

	private static final long serialVersionUID = -1228824671463633829L;

	private Option value;

	public DynamicOption(Option value) {
		this.value = value;
	}

	public Option getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value.getText();
	}
}
