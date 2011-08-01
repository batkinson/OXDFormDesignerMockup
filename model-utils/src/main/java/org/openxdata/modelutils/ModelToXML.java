package org.openxdata.modelutils;

import static org.apache.commons.lang.StringUtils.repeat;
import static org.openxdata.modelutils.OptionUtils.getDynOptDepMap;
import static org.openxdata.modelutils.OptionUtils.getPossibleValues;
import static org.openxdata.modelutils.QuestionUtils.getIdFromVarName;
import static org.openxdata.modelutils.QuestionUtils.getPathFromVariableName;
import static org.openxdata.modelutils.QuestionUtils.questionTypeGeneratesBind;
import static org.openxdata.modelutils.QuestionUtils.questionTypeGeneratesBindFormat;
import static org.openxdata.modelutils.QuestionUtils.questionTypeGeneratesBoundInput;
import static org.openxdata.modelutils.QuestionUtils.questionTypeGeneratesBoundUpload;
import static org.openxdata.modelutils.QuestionUtils.questionTypeToFormat;
import static org.openxdata.modelutils.QuestionUtils.questionTypeToMediaType;
import static org.openxdata.modelutils.QuestionUtils.questionTypeToSchemaType;
import static org.openxdata.modelutils.RuleUtils.buildAction;
import static org.openxdata.modelutils.RuleUtils.buildConstraintFromRule;
import static org.openxdata.modelutils.RuleUtils.buildSkipRuleLogic;
import static org.openxdata.modelutils.RuleUtils.getSkipRulesByTargetId;
import static org.openxdata.modelutils.RuleUtils.questionGeneratesValidationRule;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;
import org.fcitmuk.epihandy.DynamicOptionDef;
import org.fcitmuk.epihandy.FormDef;
import org.fcitmuk.epihandy.OptionDef;
import org.fcitmuk.epihandy.PageDef;
import org.fcitmuk.epihandy.QuestionDef;
import org.fcitmuk.epihandy.SkipRule;
import org.fcitmuk.epihandy.ValidationRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelToXML {

	private static Logger log = LoggerFactory.getLogger(ModelToXML.class);

	private static String PAD = "    ";

	private static void indent(StringBuilder b, int depth) {
		b.append(repeat(PAD, depth));
	}

	public static String convert(FormDef formDef, boolean useNamespaces) {
		String result = convert(formDef);
		if (!useNamespaces) {
			// Total hack, stripping rather than generating without
			result = result
					.replace(
							" xmlns:xf=\"http://www.w3.org/2002/xforms\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"",
							"");
			result = result.replaceAll("<xf:", "<");
			result = result.replaceAll("</xf:", "</");
		}
		return result;
	}

	public static String convert(FormDef formDef) {
		if (formDef == null)
			throw new IllegalArgumentException("form def can not be null");

		StringBuilder buf = new StringBuilder();

		QuestionTree qTree = QuestionTree.constructTreeFromFormDef(formDef);

		// Build a reverse map of targets to skip rules that affect them
		Map<Short, Set<SkipRule>> skipRulesByTarget = getSkipRulesByTargetId(formDef);

		// Build a map of dynamic lists to the dynopts that affect them
		Map<Short, QuestionDef> dynOptDepMap = getDynOptDepMap(formDef);

		// Output xform header and beginning of model declaration
		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n");
		buf.append("<xf:xforms xmlns:xf=\"http://www.w3.org/2002/xforms\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n");
		indent(buf, 1);
		buf.append("<xf:model>\n");
		generateMainInstance(qTree, buf);
		buf.append('\n');
		generateDynListInstances(qTree, dynOptDepMap, buf);
		generateBindings(qTree, skipRulesByTarget, buf);
		indent(buf, 1);
		buf.append("</xf:model>\n");
		generateControls(qTree, dynOptDepMap, buf);
		buf.append("</xf:xforms>\n");

		String result = buf.toString();

		if (log.isDebugEnabled())
			log.debug("converted form:\n" + result);

		return result;
	}

	@SuppressWarnings("unchecked")
	private static void generateControls(QuestionTree questionTree,
			Map<Short, QuestionDef> dynOptDepMap, StringBuilder buf) {

		FormDef formDef = questionTree.getFormDef();

		for (PageDef p : (Vector<PageDef>) formDef.getPages()) {
			indent(buf, 1);
			buf.append(MessageFormat.format("<xf:group id=\"{0}\">\n",
					p.getPageNo()));
			indent(buf, 2);
			buf.append(MessageFormat.format("<xf:label>{0}</xf:label>\n",
					StringEscapeUtils.escapeXml(p.getName())));
			for (QuestionDef q : (Vector<QuestionDef>) p.getQuestions()) {
				QuestionTree qTree = questionTree.getTreeForQuestion(q);
				generateQuestionControl(qTree, dynOptDepMap, buf);
			}
			indent(buf, 1);
			buf.append("</xf:group>\n");
		}
	}

	@SuppressWarnings("unchecked")
	private static void generateQuestionControl(QuestionTree questionTree,
			Map<Short, QuestionDef> dynOptDepMap, StringBuilder buf) {

		FormDef formDef = questionTree.getFormDef();
		String instancePath = "/" + formDef.getVariableName() + "/";

		QuestionDef question = questionTree.getQuestion();
		byte qType = question.getType();
		String qBindVar = question.getVariableName();
		if (qBindVar.startsWith(instancePath))
			qBindVar = qBindVar.substring(instancePath.length());
		String qName = StringEscapeUtils.escapeXml(question.getText());
		String[] qPath = getPathFromVariableName(question.getVariableName());
		String qId = getIdFromVarName(question.getVariableName());
		Short qIdNum = question.getId();
		int qDepth = questionTree.getDepth();
		int ilen = 1 + qDepth;

		boolean qNested = qDepth > 1;

		if (qType == QuestionDef.QTN_TYPE_REPEAT) {
			indent(buf, ilen);
			buf.append(MessageFormat.format("<xf:group id=\"{0}\">", qBindVar));
			buf.append('\n');
			indent(buf, ilen + 1);
			buf.append(MessageFormat
					.format("<xf:label>{0}</xf:label>\n", qName));
			indent(buf, ilen + 1);
			buf.append(MessageFormat.format("<xf:repeat bind=\"{0}\">\n", qId));
			for (QuestionTree childTree : questionTree.getChildren())
				generateQuestionControl(childTree, dynOptDepMap, buf);
			indent(buf, ilen + 1);
			buf.append("</xf:repeat>\n");
			indent(buf, ilen);
			buf.append("</xf:group>\n");
		} else if (qType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE) {
			indent(buf, ilen);
			if (!qNested) {
				buf.append(MessageFormat.format("<xf:select1 bind=\"{0}\">\n",
						qId));
			} else {
				buf.append(MessageFormat.format(
						"<xf:select1 ref=\"{0}\" type=\"{1}\">\n", qId,
						questionTypeToSchemaType(qType)));
			}
			indent(buf, ilen + 1);
			buf.append(MessageFormat
					.format("<xf:label>{0}</xf:label>\n", qName));
			for (OptionDef opt : (Vector<OptionDef>) question.getOptions()) {
				String optFormat = "<xf:item id=\"{0}\"><xf:label>{1}</xf:label><xf:value>{0}</xf:value></xf:item>\n";
				String optDef = MessageFormat.format(optFormat,
						opt.getVariableName(),
						StringEscapeUtils.escapeXml(opt.getText()));
				indent(buf, ilen + 1);
				buf.append(optDef);
			}
			indent(buf, ilen);
			buf.append("</xf:select1>\n");
		} else if (qType == QuestionDef.QTN_TYPE_LIST_MULTIPLE) {
			indent(buf, ilen);
			if (!qNested)
				buf.append(MessageFormat.format("<xf:select bind=\"{0}\">\n",
						qId));
			else
				buf.append(MessageFormat.format(
						"<xf:select ref=\"{0}\" type=\"{1}\">\n", qId,
						questionTypeToSchemaType(qType)));
			indent(buf, ilen + 1);
			buf.append(MessageFormat
					.format("<xf:label>{0}</xf:label>\n", qName));
			for (OptionDef opt : (Vector<OptionDef>) question.getOptions()) {
				String optFormat = "<xf:item id=\"{0}\"><xf:label>{1}</xf:label><xf:value>{0}</xf:value></xf:item>\n";
				String optDef = MessageFormat.format(optFormat,
						opt.getVariableName(),
						StringEscapeUtils.escapeXml(opt.getText()));
				indent(buf, ilen + 1);
				buf.append(optDef);
			}
			indent(buf, ilen);
			buf.append("</xf:select>\n");
		} else if (qType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC) {
			indent(buf, ilen);
			if (!qNested)
				buf.append(MessageFormat.format("<xf:select1 bind=\"{0}\">\n",
						qId));
			else
				buf.append(MessageFormat.format(
						"<xf:select1 ref=\"{0}\" type=\"{1}\">\n", qId,
						questionTypeToSchemaType(qType)));
			indent(buf, ilen + 1);
			buf.append(MessageFormat
					.format("<xf:label>{0}</xf:label>\n", qName));
			QuestionDef parentQuestion = dynOptDepMap.get(qIdNum);
			String parentId = getIdFromVarName(parentQuestion.getVariableName());
			String itemsetFormat = "<xf:itemset nodeset=\"instance(''{1}'')/item[@parent=instance(''{0}'')/{2}]\"><xf:label ref=\"label\"/><xf:value ref=\"value\"/></xf:itemset>\n";
			String instanceId = qPath[1];
			indent(buf, ilen + 1);
			String itemsetDef = MessageFormat.format(itemsetFormat, instanceId,
					qId, parentId);
			buf.append(itemsetDef);
			indent(buf, ilen);
			buf.append("</xf:select1>\n");
		} else if (questionTypeGeneratesBoundInput(qType)) {
			indent(buf, ilen);
			if (!qNested)
				buf.append(MessageFormat.format("<xf:input bind=\"{0}\">\n",
						qId));
			else
				buf.append(MessageFormat.format(
						"<xf:input ref=\"{0}\" type=\"{1}\">\n", qId,
						questionTypeToSchemaType(qType)));
			indent(buf, ilen + 1);
			buf.append(MessageFormat
					.format("<xf:label>{0}</xf:label>\n", qName));
			indent(buf, ilen);
			buf.append("</xf:input>\n");
		} else if (questionTypeGeneratesBoundUpload(qType)) {
			String mediaType = questionTypeToMediaType(qType);
			indent(buf, ilen);
			if (!qNested)
				buf.append(MessageFormat.format(
						"<xf:upload bind=\"{0}\" mediatype=\"{1}\">\n", qId,
						mediaType));
			else
				buf.append(MessageFormat
						.format("<xf:upload ref=\"{0}\" type=\"{1}\" mediatype=\"{2}\">\n",
								qId, questionTypeToSchemaType(qType), mediaType));
			indent(buf, ilen + 1);
			buf.append(MessageFormat
					.format("<xf:label>{0}</xf:label>\n", qName));
			indent(buf, ilen);
			buf.append("</xf:upload>\n");
		}
	}

	private static void generateBindings(QuestionTree questionTree,
			Map<Short, Set<SkipRule>> skipRulesByTarget, StringBuilder buf) {

		FormDef formDef = questionTree.getFormDef();

		if (!questionTree.isLeaf()) {
			for (QuestionTree childTree : questionTree.getChildren()) {

				QuestionDef question = childTree.getQuestion();
				Short qIdNum = question.getId();
				String qVar = question.getVariableName();
				String qId = getIdFromVarName(qVar);
				byte qType = question.getType();

				boolean generateBind = questionTypeGeneratesBind(qType);
				boolean generateType = qType != QuestionDef.QTN_TYPE_REPEAT;
				boolean generateFormat = questionTypeGeneratesBindFormat(qType);
				boolean generateValidation = questionGeneratesValidationRule(
						formDef, question);
				boolean generateRelevant = skipRulesByTarget
						.containsKey(qIdNum);
				boolean generateRequired = question.isMandatory()
						|| (generateRelevant && !question.isMandatory());
				boolean generateReadonly = !question.isEnabled();

				if (generateBind) {
					indent(buf, 2);
					StringBuilder bindBuf = new StringBuilder(
							"<xf:bind id=\"{0}\" nodeset=\"{1}\"");
					List<Object> bindArgs = new ArrayList<Object>();

					bindArgs.add(qId);
					bindArgs.add(qVar);

					if (generateType) {
						bindBuf.append(" type=\"{");
						bindBuf.append(bindArgs.size());
						bindBuf.append("}\"");
						bindArgs.add(questionTypeToSchemaType(qType));
					}

					if (generateFormat) {
						bindBuf.append(" format=\"{");
						bindBuf.append(bindArgs.size());
						bindBuf.append("}\"");
						bindArgs.add(questionTypeToFormat(qType));
					}

					if (generateValidation) {
						bindBuf.append(" constraint=\"{");
						bindBuf.append(bindArgs.size());
						bindBuf.append("}\" message=\"{");
						bindBuf.append(bindArgs.size() + 1);
						bindBuf.append("}\"");
						ValidationRule vRule = formDef
								.getValidationRule(qIdNum);
						String constraint = buildConstraintFromRule(formDef,
								vRule);
						bindArgs.add(constraint);
						bindArgs.add(vRule.getErrorMessage());
					}

					if (generateRelevant) {
						bindBuf.append(" relevant=\"{");
						bindBuf.append(bindArgs.size());
						bindBuf.append("}\" action=\"{");
						bindBuf.append(bindArgs.size() + 1);
						bindBuf.append("}\"");
						Set<SkipRule> skipRules = skipRulesByTarget.get(qIdNum);
						String constraint = buildSkipRuleLogic(formDef,
								skipRules, question);
						Object lastRule = skipRules.toArray()[skipRules.size() - 1];
						String action = buildAction(((SkipRule) lastRule)
								.getAction());
						bindArgs.add(constraint);
						bindArgs.add(action);
					}

					if (generateRequired) {
						bindBuf.append(" required=\"{");
						bindBuf.append(bindArgs.size());
						bindBuf.append("}()\"");
						bindArgs.add(question.isMandatory());
					}

					if (generateReadonly) {
						bindBuf.append(" readonly=\"{");
						bindBuf.append(bindArgs.size());
						bindBuf.append("}()\"");
						bindArgs.add(!question.isEnabled());
					}

					bindBuf.append("/>");
					buf.append(MessageFormat.format(bindBuf.toString(),
							bindArgs.toArray()));
					buf.append('\n');
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void generateDynListInstances(QuestionTree questionTree,
			Map<Short, QuestionDef> dynOptDepMap, StringBuilder buf) {

		FormDef formDef = questionTree.getFormDef();

		if (!questionTree.isLeaf()) {
			for (QuestionTree childTree : questionTree.getChildren()) {

				QuestionDef question = childTree.getQuestion();
				byte qType = question.getType();
				String qId = getIdFromVarName(question.getVariableName());

				if (qType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC) {
					indent(buf, 2);
					String instanceDef = MessageFormat.format(
							"<xf:instance id=''{0}''>\n", qId);
					buf.append(instanceDef);
					indent(buf, 3);
					buf.append("<dynamiclist>\n");
					QuestionDef parentQuestion = dynOptDepMap.get(question
							.getId());
					QuestionDef parentParentQuestion = dynOptDepMap
							.get(parentQuestion.getId());
					Map<Short, OptionDef> possibleParentValues = getPossibleValues(
							formDef, parentQuestion, parentParentQuestion);
					DynamicOptionDef dynOptDef = formDef
							.getDynamicOptions(parentQuestion.getId());
					for (Map.Entry<Short, Vector<OptionDef>> dynOptEntry : (Set<Map.Entry<Short, Vector<OptionDef>>>) dynOptDef
							.getParentToChildOptions().entrySet()) {
						for (OptionDef option : dynOptEntry.getValue()) {
							indent(buf, 4);
							String itemPattern = "<item id=\"{0}\" parent=\"{1}\"><label>{2}</label><value>{0}</value></item>\n";
							OptionDef parentOption = possibleParentValues
									.get(dynOptEntry.getKey());
							String itemDef = MessageFormat.format(itemPattern,
									option.getVariableName(), parentOption
											.getVariableName(),
									StringEscapeUtils.escapeXml(option
											.getText()));
							buf.append(itemDef);
						}
					}
					indent(buf, 3);
					buf.append("</dynamiclist>\n");
					indent(buf, 2);
					buf.append("</xf:instance>\n");
				}
			}
		}
	}

	private static void generateMainInstance(QuestionTree rootTree,
			StringBuilder buf) {

		FormDef formDef = rootTree.getFormDef();

		indent(buf, 2);
		buf.append(MessageFormat.format("<xf:instance id=\"{0}\">",
				formDef.getVariableName()));
		buf.append('\n');

		// Generate the main instance
		indent(buf, 3);
		buf.append(MessageFormat.format(
				"<{0} description-template=\"{1}\" id=\"{2}\" name=\"{3}\">",
				formDef.getVariableName(), formDef.getDescriptionTemplate(),
				formDef.getId(), formDef.getName()));
		buf.append('\n');
		if (!rootTree.isLeaf()) {
			for (QuestionTree childTree : rootTree.getChildren()) {
				generateInstanceElement(childTree, buf);
			}
		}
		indent(buf, 3);
		buf.append(MessageFormat.format("</{0}>", formDef.getVariableName()));
		buf.append('\n');
		indent(buf, 2);
		buf.append("</xf:instance>");
	}

	private static void generateInstanceElement(QuestionTree tree,
			StringBuilder buf) {

		FormDef form = tree.getFormDef();
		QuestionDef question = tree.getQuestion();
		String instanceBinding = "/" + form.getVariableName() + "/";
		String questionBinding = tree.getQuestion().getVariableName();
		int ilen = 3 + tree.getDepth();

		if (questionBinding.startsWith(instanceBinding))
			questionBinding = questionBinding.substring(instanceBinding
					.length());

		String[] elements = questionBinding.split("/");
		for (int elem = 0; elem < elements.length - 1; elem++) {
			indent(buf, ilen + elem);
			buf.append(MessageFormat.format("<{0}>\n", elements[elem]));
		}

		String lastElement = elements[elements.length - 1];
		if (tree.isLeaf()) {
			indent(buf, ilen);
			String defaultValue = question.getDefaultValue();
			if (defaultValue != null && !"".equals(defaultValue))
				buf.append(MessageFormat.format("<{0}>{1}</{0}>\n",
						lastElement, defaultValue));
			else
				buf.append(MessageFormat.format("<{0}/>\n", lastElement));
		} else {
			indent(buf, ilen + 1);
			buf.append(MessageFormat.format("<{0}>\n", lastElement));
			for (QuestionTree childTree : tree.getChildren())
				generateInstanceElement(childTree, buf);
			indent(buf, ilen + 1);
			buf.append(MessageFormat.format("</{0}>\n", lastElement));
		}

		for (int elem = elements.length - 2; elem >= 0; elem--) {
			indent(buf, ilen + elem);
			String elementName = elements[elem];
			buf.append(MessageFormat.format("</{0}>\n", elementName));
		}
	}
}
