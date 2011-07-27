package org.openxdata.designer.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

	private static final Pattern uscorePattern = Pattern.compile("_(\\w)");

	public static String convertUnderscoreToCamelCase(String name) {
		StringBuffer camelCaseName = new StringBuffer();
		Matcher m = uscorePattern.matcher(name);
		while (m.find()) {
			m.appendReplacement(camelCaseName, m.group(1).toUpperCase());
		}
		m.appendTail(camelCaseName);
		return camelCaseName.toString();
	}
}
