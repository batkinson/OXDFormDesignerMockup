package org.openxdata.modelutils;

import org.fcitmuk.epihandy.QuestionDef;

public class QuestionUtils {

	private static String BASE64_XSDTYPE = "xsd:base64Binary";
	private static String BOOLEAN_XSDTYPE = "xsd:boolean";
	private static String STRING_XSDTYPE = "xsd:string";

	private static final String DATE_XSDTYPE = "xsd:date";
	private static final String DATETIME_XSDTYPE = "xsd:dateTime";
	private static final String INTEGER_XSDTYPE = "xsd:int";
	private static final String DECIMAL_XSDTYPE = "xsd:decimal";
	private static final String TIME_XDSTYPE = "xsd:time";

	private static final String AUDIO_BINDFORMAT = "audio";
	private static final String VIDEO_BINDFORMAT = "video";
	private static final String IMAGE_BINDFORMAT = "image";
	private static final String GPS_BINDFORMAT = "gps";
	private static final String PHONENUMBER_BINDFORMAT = "phonenumber";

	public static boolean questionTypeGeneratesBind(byte type) {
		switch (type) {
		case QuestionDef.QTN_TYPE_AUDIO:
		case QuestionDef.QTN_TYPE_BARCODE:
		case QuestionDef.QTN_TYPE_BOOLEAN:
		case QuestionDef.QTN_TYPE_DATE:
		case QuestionDef.QTN_TYPE_DATE_TIME:
		case QuestionDef.QTN_TYPE_DECIMAL:
		case QuestionDef.QTN_TYPE_GPS:
		case QuestionDef.QTN_TYPE_IMAGE:
		case QuestionDef.QTN_TYPE_LIST_EXCLUSIVE:
		case QuestionDef.QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC:
		case QuestionDef.QTN_TYPE_LIST_MULTIPLE:
		case QuestionDef.QTN_TYPE_NUMERIC:
		case QuestionDef.QTN_TYPE_PHONENUMBER:
		case QuestionDef.QTN_TYPE_TEXT:
		case QuestionDef.QTN_TYPE_TIME:
		case QuestionDef.QTN_TYPE_VIDEO:
		case QuestionDef.QTN_TYPE_REPEAT:
			return true;
		default:
			return false;
		}
	}

	public static String questionTypeToSchemaType(byte type) {

		switch (type) {
		case QuestionDef.QTN_TYPE_AUDIO:
			return BASE64_XSDTYPE;
		case QuestionDef.QTN_TYPE_BARCODE:
			return STRING_XSDTYPE;
		case QuestionDef.QTN_TYPE_BOOLEAN:
			return BOOLEAN_XSDTYPE;
		case QuestionDef.QTN_TYPE_DATE:
			return DATE_XSDTYPE;
		case QuestionDef.QTN_TYPE_DATE_TIME:
			return DATETIME_XSDTYPE;
		case QuestionDef.QTN_TYPE_DECIMAL:
			return DECIMAL_XSDTYPE;
		case QuestionDef.QTN_TYPE_GPS:
			return STRING_XSDTYPE;
		case QuestionDef.QTN_TYPE_IMAGE:
			return BASE64_XSDTYPE;
		case QuestionDef.QTN_TYPE_LIST_EXCLUSIVE:
		case QuestionDef.QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC:
		case QuestionDef.QTN_TYPE_LIST_MULTIPLE:
			return STRING_XSDTYPE;
		case QuestionDef.QTN_TYPE_NUMERIC:
			return INTEGER_XSDTYPE;
		case QuestionDef.QTN_TYPE_PHONENUMBER:
			return STRING_XSDTYPE;
		case QuestionDef.QTN_TYPE_TEXT:
			return STRING_XSDTYPE;
		case QuestionDef.QTN_TYPE_TIME:
			return TIME_XDSTYPE;
		case QuestionDef.QTN_TYPE_VIDEO:
			return BASE64_XSDTYPE;
		default:
			return null;
		}
	}

	public static String questionTypeToMediaType(byte type) {
		switch (type) {
		case QuestionDef.QTN_TYPE_AUDIO:
			return "audio/*";
		case QuestionDef.QTN_TYPE_VIDEO:
			return "video/*";
		case QuestionDef.QTN_TYPE_IMAGE:
			return "image/*";
		default:
			return null;
		}
	}

	public static boolean questionTypeGeneratesBindFormat(byte type) {
		return questionTypeToFormat(type) != null;
	}

	public static String questionTypeToFormat(byte type) {
		switch (type) {
		case QuestionDef.QTN_TYPE_AUDIO:
			return AUDIO_BINDFORMAT;
		case QuestionDef.QTN_TYPE_VIDEO:
			return VIDEO_BINDFORMAT;
		case QuestionDef.QTN_TYPE_IMAGE:
			return IMAGE_BINDFORMAT;
		case QuestionDef.QTN_TYPE_GPS:
			return GPS_BINDFORMAT;
		case QuestionDef.QTN_TYPE_PHONENUMBER:
			return PHONENUMBER_BINDFORMAT;
		default:
			return null;
		}
	}

	public static boolean questionTypeGeneratesBoundInput(byte type) {
		switch (type) {
		case QuestionDef.QTN_TYPE_AUDIO:
		case QuestionDef.QTN_TYPE_VIDEO:
		case QuestionDef.QTN_TYPE_IMAGE:
		case QuestionDef.QTN_TYPE_LIST_EXCLUSIVE:
		case QuestionDef.QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC:
		case QuestionDef.QTN_TYPE_LIST_MULTIPLE:
		case QuestionDef.QTN_TYPE_REPEAT:
			return false;
		default:
			return true;
		}
	}

	public static boolean questionTypeGeneratesBoundUpload(byte type) {
		switch (type) {
		case QuestionDef.QTN_TYPE_AUDIO:
		case QuestionDef.QTN_TYPE_VIDEO:
		case QuestionDef.QTN_TYPE_IMAGE:
			return true;
		default:
			return false;
		}
	}

	public static String[] getPathFromVariableName(String varName) {
		String trimmedString = varName.trim();
		String[] path = trimmedString.split("/");
		return path;
	}

	public static String getIdFromVarName(String varName) {
		String[] path = getPathFromVariableName(varName);
		return path[path.length - 1];
	}
}
