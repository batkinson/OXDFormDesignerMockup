package org.openxdata.designer;

import org.apache.pivot.wtk.DesktopApplicationContext;

/**
 * A utility class that allows one to run the form designer as a standalone
 * application easily.
 * 
 * @author brent
 * 
 */
public class Main {

	public static void main(String[] args) {
		DesktopApplicationContext.main(DesignerApp.class, args);
	}

}
