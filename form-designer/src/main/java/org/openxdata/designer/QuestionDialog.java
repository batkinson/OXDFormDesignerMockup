package org.openxdata.designer;

import java.net.URL;

import org.apache.pivot.beans.BXML;
import org.apache.pivot.beans.BeanAdapter;
import org.apache.pivot.beans.Bindable;
import org.apache.pivot.collections.Map;
import org.apache.pivot.util.Resources;
import org.apache.pivot.wtk.Action;
import org.apache.pivot.wtk.Component;
import org.apache.pivot.wtk.Dialog;
import org.apache.pivot.wtk.ListButton;
import org.apache.pivot.wtk.PushButton;
import org.fcitmuk.epihandy.QuestionDef;
import org.openxdata.designer.util.Question;
import org.openxdata.designer.util.StaticFieldMapping;

public class QuestionDialog extends Dialog implements Bindable {

	private StaticFieldMapping<Byte> questionMapping;

	@BXML
	private ListButton questionType;

	@BXML
	private PushButton questionDialogSaveButton;

	public void initialize(Map<String, Object> namespace, URL location,
			Resources resources) {

		// Populate dropdown with localized labels for valid question types
		questionMapping = new StaticFieldMapping<Byte>(QuestionDef.class,
				"QTN_TYPE_", "questionType", null, resources);
		questionType.setSelectedItemBindMapping(questionMapping);
		questionType.setListData(questionMapping.getLabels());

		// Install event handler for save button
		questionDialogSaveButton.setAction(new Action() {
			@Override
			public void perform(Component source) {
				Question activeQuestion = (Question) getUserData().get(
						"activeQuestion");
				store(new BeanAdapter(activeQuestion));
				close();
			}
		});
	}

}
