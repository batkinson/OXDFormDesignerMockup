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
import org.apache.pivot.wtk.PushButton;
import org.openxdata.designer.util.Form;

public class FormDialog extends Dialog implements Bindable {

	@BXML
	private PushButton formDialogSaveButton;

	public void initialize(Map<String, Object> namespace, URL location,
			Resources resources) {

		formDialogSaveButton.setAction(new Action() {
			@Override
			public void perform(Component source) {
				Form form = (Form) getUserData().get("activeForm");
				store(new BeanAdapter(form));
				close();
			}
		});
	}

}
