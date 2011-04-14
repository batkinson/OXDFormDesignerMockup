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
import org.openxdata.designer.util.Option;

public class OptionDialog extends Dialog implements Bindable {

	@BXML
	private PushButton optionDialogSaveButton;

	public void initialize(Map<String, Object> namespace, URL location,
			Resources resources) {

		optionDialogSaveButton.setAction(new Action() {
			@Override
			public void perform(Component source) {
				Option option = (Option) getUserData().get("activeOption");
				store(new BeanAdapter(option));
				close();
			}
		});
	}

}
