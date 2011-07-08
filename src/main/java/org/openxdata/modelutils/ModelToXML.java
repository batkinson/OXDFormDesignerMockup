package org.openxdata.modelutils;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import org.fcitmuk.epihandy.DynamicOptionDef;
import org.fcitmuk.epihandy.FormDef;
import org.fcitmuk.epihandy.OptionDef;
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

		Map<Short, QuestionDef> dynOptDepMap = new HashMap<Short, QuestionDef>();
		for (Map.Entry<Short, DynamicOptionDef> dynOptEntry : (Set<Map.Entry<Short, DynamicOptionDef>>) formDef
				.getDynamicOptions().entrySet()) {
			dynOptDepMap.put(dynOptEntry.getValue().getQuestionId(),
					formDef.getQuestion(dynOptEntry.getKey()));
		}

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

					if (tree.length > 3 && stack.size() > 0)
						buf.append('\n');

					for (int depth = 0; depth < stack.size(); depth++)
						buf.append('\t');

					buf.append("\t\t\t\t");
					buf.append('<');
					buf.append(tree[i]);
					buf.append('>');
					stack.push(tree[i]);
				}
				while (!stack.isEmpty()) {
					String item = stack.pop();
					if (tree.length > 3) {
						for (int depth = 0; depth < stack.size(); depth++)
							buf.append('\t');
						buf.append("\t\t\t\t");
					}
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

				byte qType = q.getType();
				String qPath = q.getVariableName();
				String qName = q.getText();
				String[] tree = q.getVariableName().split("/\\s*");
				String qId = tree[tree.length - 1];

				if (qType == QuestionDef.QTN_TYPE_REPEAT) {
					buf.append(MessageFormat.format(
							"\t\t<xf:group id=\"{0}\">", qPath));
					buf.append('\n');
					buf.append(MessageFormat.format(
							"\t\t\t<xf:label>{0}</xf:label>", qName));
					buf.append('\n');
					buf.append("\t\t</xf:group>\n");
				} else if (qType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE) {
					buf.append(MessageFormat.format(
							"\t\t<xf:select1 bind=\"{0}\">", qId));
					buf.append('\n');
					buf.append(MessageFormat.format(
							"\t\t\t<xf:label>{0}</xf:label>", qName));
					buf.append('\n');
					for (OptionDef opt : (Vector<OptionDef>) q.getOptions()) {
						String optFormat = "<xf:item id=\"{0}\"><xf:label>{1}</xf:label><xf:value>{0}</xf:value></xf:item>";
						String optDef = MessageFormat.format(optFormat,
								opt.getVariableName(), opt.getText());
						buf.append("\t\t\t");
						buf.append(optDef);
						buf.append('\n');
					}
					buf.append("\t\t</xf:select1>");
					buf.append('\n');
				} else if (qType == QuestionDef.QTN_TYPE_LIST_MULTIPLE) {
					buf.append(MessageFormat.format(
							"\t\t<xf:select bind=\"{0}\">", qId));
					buf.append('\n');
					buf.append(MessageFormat.format(
							"\t\t\t<xf:label>{0}</xf:label>", qName));
					buf.append('\n');
					for (OptionDef opt : (Vector<OptionDef>) q.getOptions()) {
						String optFormat = "<xf:item id=\"{0}\"><xf:label>{1}</xf:label><xf:value>{0}</xf:value></xf:item>";
						String optDef = MessageFormat.format(optFormat,
								opt.getVariableName(), opt.getText());
						buf.append("\t\t\t");
						buf.append(optDef);
						buf.append('\n');
					}
					buf.append("\t\t</xf:select>");
					buf.append('\n');
				} else if (qType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC) {
					buf.append(MessageFormat.format(
							"\t\t<xf:select1 bind=\"{0}\">", qId));
					buf.append('\n');
					buf.append(MessageFormat.format(
							"\t\t\t<xf:label>{0}</xf:label>", qName));
					buf.append('\n');
					QuestionDef parentQuestion = dynOptDepMap.get(q.getId());
					String[] parentTree = parentQuestion.getVariableName()
							.split("/\\s*");
					String parentId = parentTree[parentTree.length - 1];
					String itemsetFormat = "\t\t\t<xf:itemset nodeset=\"instance(''{1}'')/item[@parent=instance(''{0}'')/{2}]\"><xf:label ref=\"label\"/><xf:value ref=\"value\"/></xf:itemset>\n";
					String itemsetDef = MessageFormat.format(itemsetFormat,
							tree[1], qId, parentId);
					buf.append(itemsetDef);
					buf.append("\t\t</xf:select1>");
					buf.append('\n');
				} else if (questionTypeGeneratesBoundInput(qType)) {
					buf.append(MessageFormat.format(
							"\t\t<xf:input bind=\"{0}\">", qId));
					buf.append('\n');
					buf.append(MessageFormat.format(
							"\t\t\t<xf:label>{0}</xf:label>", qName));
					buf.append('\n');
					buf.append("\t\t</xf:input>");
					buf.append('\n');
				} else if (questionTypeGeneratesBoundUpload(qType)) {
					String mediaType = questionTypeToMediaType(qType);
					buf.append(MessageFormat.format(
							"\t\t<xf:upload bind=\"{0}\" mediatype=\"{1}\">",
							qId, mediaType));
					buf.append('\n');
					buf.append(MessageFormat.format(
							"\t\t\t<xf:label>{0}</xf:label>", qName));
					buf.append('\n');
					buf.append("\t\t</xf:upload>");
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
}
