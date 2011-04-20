package org.openxdata.designer;

import java.net.URL;

import org.apache.pivot.beans.BXML;
import org.apache.pivot.beans.Bindable;
import org.apache.pivot.collections.Map;
import org.apache.pivot.util.Resources;
import org.apache.pivot.wtk.Action;
import org.apache.pivot.wtk.Component;
import org.apache.pivot.wtk.Dialog;
import org.apache.pivot.wtk.PushButton;

public class SkipRuleDialog extends Dialog implements Bindable {

	@BXML
	private Dialog skipRuleDialog;

	@BXML
	private PushButton skipRuleDialogSaveButton;

	public void initialize(Map<String, Object> namespace, URL location,
			Resources resources) {

		skipRuleDialogSaveButton.setAction(new Action() {
			@Override
			public void perform(Component source) {
				skipRuleDialog.close();
			}
		});

	}

}
