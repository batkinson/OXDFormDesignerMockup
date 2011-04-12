package org.openxdata.designer;

import java.net.URL;

import org.apache.pivot.beans.BXML;
import org.apache.pivot.beans.Bindable;
import org.apache.pivot.collections.Map;
import org.apache.pivot.util.Resources;
import org.apache.pivot.wtk.Dialog;
import org.apache.pivot.wtk.ListButton;
import org.openxdata.designer.designtree.QuestionTypeMapping;

public class QuestionDialog extends Dialog implements Bindable {

	private QuestionTypeMapping questionMapping;

	@BXML
	private ListButton questionType;

	public void initialize(Map<String, Object> namespace, URL location,
			Resources resources) {

		// Populate dropdown with localized labels for valid question types
		questionMapping = new QuestionTypeMapping(resources);
		questionType.setSelectedItemBindMapping(questionMapping);
		questionType.setListData(questionMapping.getTypeLabels());
	}

}
