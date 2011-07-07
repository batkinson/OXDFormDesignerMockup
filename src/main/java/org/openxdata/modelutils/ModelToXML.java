package org.openxdata.modelutils;

import java.text.MessageFormat;
import java.util.Stack;
import java.util.Vector;

import org.fcitmuk.epihandy.FormDef;
import org.fcitmuk.epihandy.PageDef;
import org.fcitmuk.epihandy.QuestionDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelToXML {

	private static Logger log = LoggerFactory.getLogger(ModelToXML.class);

	public static String BASE64_XSDTYPE = "xsd:base64Binary";
	public static String BOOLEAN_XSDTYPE = "xsd:boolean";
	public static String STRING_XSDTYPE = "xsd:string";
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

	@SuppressWarnings("unchecked")
	public static String convert(FormDef formDef) {
		StringBuilder buf = new StringBuilder();
		if (formDef == null)
			throw new IllegalArgumentException("form def can not be null");
		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>");
		buf.append('\n');
		buf.append("<xf:xforms xmlns:xf=\"http://www.w3.org/2002/xforms\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">");
		buf.append('\n');
		buf.append("\t<xf:model>");
		buf.append('\n');
		buf.append(MessageFormat.format("\t\t<xf:instance id=\"{0}\">",
				formDef.getVariableName()));
		buf.append('\n');

		// Generate the main instance
		buf.append(MessageFormat
				.format("\t\t\t<{0} description-template=\"{1}\" id=\"{2}\" name=\"{3}\">",
						formDef.getVariableName(),
						formDef.getDescriptionTemplate(), formDef.getId(),
						formDef.getName()));
		buf.append('\n');
		for (PageDef p : (Vector<PageDef>) formDef.getPages())
			for (QuestionDef q : (Vector<QuestionDef>) p.getQuestions()) {
				String[] tree = q.getVariableName().split("/\\s*");
				Stack<String> stack = new Stack<String>();
				for (int i = 0; i < tree.length; i++) {
					if ("".equals(tree[i])
							|| formDef.getVariableName().equals(tree[i]))
						continue;
					buf.append('<');
					buf.append(tree[i]);
					buf.append('>');
					stack.push(tree[i]);
				}
				while (!stack.isEmpty()) {
					String item = stack.pop();
					buf.append("</");
					buf.append(item);
					buf.append('>');
					buf.append('\n');
				}
			}
		buf.append(MessageFormat.format("\t\t\t</{0}>",
				formDef.getVariableName()));
		buf.append('\n');
		buf.append("\t\t</xf:instance>");
		buf.append('\n');

		for (PageDef p : (Vector<PageDef>) formDef.getPages())
			for (QuestionDef q : (Vector<QuestionDef>) p.getQuestions()) {
				String[] tree = q.getVariableName().split("/\\s*");
				for (int i = 0; i < tree.length; i++) {
					if ("".equals(tree[i])
							|| formDef.getVariableName().equals(tree[i]))
						continue;

					boolean generateBind = questionTypeGeneratesBind(q
							.getType());
					boolean generateFormat = questionTypeGeneratesBindFormat(q
							.getType());
					String qid = tree[tree.length - 1];
					if (generateBind) {
						buf.append("\t\t");
						if (generateFormat) {
							buf.append(MessageFormat
									.format("<xf:bind id=\"{0}\" nodeset=\"{1}\" type=\"{2}\" format=\"{3}\" />",
											qid, q.getVariableName(),
											questionTypeToSchemaType(q
													.getType()),
											questionTypeToFormat(q.getType())));
						} else {
							buf.append(MessageFormat
									.format("<xf:bind id=\"{0}\" nodeset=\"{1}\" type=\"{2}\"/>",
											qid, q.getVariableName(),
											questionTypeToSchemaType(q
													.getType())));
						}
						buf.append('\n');
					}
				}
			}

		buf.append("\t</xf:model>");
		buf.append('\n');

		for (PageDef p : (Vector<PageDef>) formDef.getPages()) {
			buf.append(MessageFormat.format("\t<xf:group id=\"{0}\">",
					p.getPageNo()));
			buf.append('\n');
			buf.append(MessageFormat.format("\t\t<xf:label>{0}</xf:label>",
					p.getName()));
			buf.append('\n');

			for (QuestionDef q : (Vector<QuestionDef>) p.getQuestions()) {
				if (q.getType() == QuestionDef.QTN_TYPE_REPEAT) {
					buf.append(MessageFormat.format(
							"\t\t<xf:group id=\"{0}\">", q.getVariableName()));
					buf.append('\n');
					buf.append(MessageFormat.format(
							"\t\t\t<xf:label>{0}</xf:label>", q.getText()));
					buf.append('\n');
					buf.append("\t\t</xf:group>");
				} else if (q.getType() == QuestionDef.QTN_TYPE_TEXT) {
					buf.append(MessageFormat.format(
							"\t\t<xf:input bind=\"{0}\">", q.getVariableName()));
					buf.append('\n');
					buf.append(MessageFormat.format(
							"\t\t\t<xf:label>{0}</xf:label>", q.getText()));
					buf.append('\n');
					buf.append("\t\t</xf:input>");
					buf.append('\n');
				}
			}

			buf.append('\n');
			buf.append("\t</xf:group>");
			buf.append('\n');
		}

		buf.append("</xf:xforms>");
		buf.append('\n');

		if (log.isDebugEnabled())
			log.debug("converted form:\n" + buf.toString());

		return buf.toString();
	}

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
}
