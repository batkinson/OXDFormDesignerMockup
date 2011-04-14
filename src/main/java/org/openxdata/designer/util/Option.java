package org.openxdata.designer.util;

import org.fcitmuk.epihandy.OptionDef;

public class Option extends OptionDef implements PageElement {

	public Option() {
		super((short) -1, "New Option", "newOption");
	}

	public Option(OptionDef optionDef) {
		super(optionDef);
	}

}
