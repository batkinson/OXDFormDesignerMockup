package org.openxdata.modelutils;

import java.text.MessageFormat;
import java.util.Stack;
import java.util.Vector;

import org.fcitmuk.epihandy.FormDef;
import org.fcitmuk.epihandy.PageDef;
import org.fcitmuk.epihandy.QuestionDef;

public class ModelToXML {

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
					buf.append(MessageFormat
							.format("<xf:bind id=\"{0}\" nodeset=\"{1}\" type=\"{2}\"/>",
									q.getText(), q.getVariableName(),
									"xsd:string"));
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
		System.err.println(buf.toString());
		return buf.toString();
	}
}
