package org.openxdata.modelutils;

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

	public static String convert(FormDef formDef) {
		if (formDef == null)
			throw new IllegalArgumentException("form def can not be null");

		StringBuilder buf = new StringBuilder();

		QuestionTree qTree = QuestionTree.constructTreeFromFormDef(formDef);

		if (log.isDebugEnabled())
			log.debug("parsed question tree: \n" + qTree);

		// Build a reverse map of targets to skip rules that affect them
		Map<Short, Set<SkipRule>> skipRulesByTarget = getSkipRulesByTargetId(formDef);

		// Build a map of dynamic lists to the dynopts that affect them
		Map<Short, QuestionDef> dynOptDepMap = getDynOptDepMap(formDef);

		// Output xform header and beginning of model declaration
		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n");
		buf.append("<xf:xforms xmlns:xf=\"http://www.w3.org/2002/xforms\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n");
		buf.append("\t<xf:model>\n");
		generateMainInstance(qTree, buf);
		buf.append('\n');
		generateDynListInstances(qTree, dynOptDepMap, buf);
		generateBindings(qTree, skipRulesByTarget, buf);
		buf.append("\t</xf:model>\n");
		generateControls(qTree, dynOptDepMap, buf);
		buf.append("</xf:xforms>\n");

		if (log.isDebugEnabled())
			log.debug("converted form:\n" + buf.toString());

		return buf.toString();
	}

	@SuppressWarnings("unchecked")
	private static void generateControls(QuestionTree questionTree,
			Map<Short, QuestionDef> dynOptDepMap, StringBuilder buf) {

		FormDef formDef = questionTree.getFormDef();

		for (PageDef p : (Vector<PageDef>) formDef.getPages()) {
			buf.append(MessageFormat.format("\t<xf:group id=\"{0}\">\n",
					p.getPageNo()));
			buf.append(MessageFormat.format("\t\t<xf:label>{0}</xf:label>\n",
					StringEscapeUtils.escapeXml(p.getName())));
			for (QuestionDef q : (Vector<QuestionDef>) p.getQuestions()) {
				QuestionTree qTree = questionTree.getTreeForQuestion(q);
				generateQuestionControl(qTree, dynOptDepMap, buf);
			}
			buf.append("\t</xf:group>\n");
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

		// Build pad based on question depth
		StringBuilder qPad = new StringBuilder("\t\t");
		for (int i = 1; i < qDepth; i++)
			qPad.append('\t');

		boolean qNested = qDepth > 1;

		if (qType == QuestionDef.QTN_TYPE_REPEAT) {
			buf.append(MessageFormat.format("{0}<xf:group id=\"{1}\">", qPad,
					qBindVar));
			buf.append('\n');
			buf.append(MessageFormat.format("{0}\t<xf:label>{1}</xf:label>\n",
					qPad, qName));
			buf.append(MessageFormat.format("{0}\t<xf:repeat bind=\"{1}\">\n",
					qPad, qId));
			for (QuestionTree childTree : questionTree.getChildren())
				generateQuestionControl(childTree, dynOptDepMap, buf);
			buf.append(MessageFormat.format(
					"{0}\t</xf:repeat>\n{0}</xf:group>\n", qPad));
		} else if (qType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE) {
			if (!qNested)
				buf.append(MessageFormat.format(
						"{0}<xf:select1 bind=\"{1}\">\n", qPad, qId));
			else
				buf.append(MessageFormat.format(
						"{0}<xf:select1 ref=\"{1}\" type=\"{2}\">\n", qPad,
						qId, questionTypeToSchemaType(qType)));
			buf.append(MessageFormat.format("{0}\t<xf:label>{1}</xf:label>\n",
					qPad, qName));
			for (OptionDef opt : (Vector<OptionDef>) question.getOptions()) {
				String optFormat = "{0}\t<xf:item id=\"{1}\"><xf:label>{2}</xf:label><xf:value>{1}</xf:value></xf:item>\n";
				String optDef = MessageFormat.format(optFormat, qPad,
						opt.getVariableName(),
						StringEscapeUtils.escapeXml(opt.getText()));
				buf.append(optDef);
			}
			buf.append(MessageFormat.format("{0}</xf:select1>\n", qPad));
		} else if (qType == QuestionDef.QTN_TYPE_LIST_MULTIPLE) {
			if (!qNested)
				buf.append(MessageFormat.format(
						"{0}<xf:select bind=\"{1}\">\n", qPad, qId));
			else
				buf.append(MessageFormat.format(
						"{0}<xf:select ref=\"{1}\" type=\"{2}\">\n", qPad, qId,
						questionTypeToSchemaType(qType)));
			buf.append(MessageFormat.format("{0}\t<xf:label>{1}</xf:label>\n",
					qPad, qName));
			for (OptionDef opt : (Vector<OptionDef>) question.getOptions()) {
				String optFormat = "{0}\t<xf:item id=\"{1}\"><xf:label>{2}</xf:label><xf:value>{1}</xf:value></xf:item>\n";
				String optDef = MessageFormat.format(optFormat, qPad,
						opt.getVariableName(),
						StringEscapeUtils.escapeXml(opt.getText()));
				buf.append(optDef);
			}
			buf.append(MessageFormat.format("{0}</xf:select>\n", qPad));
		} else if (qType == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC) {
			if (!qNested)
				buf.append(MessageFormat.format(
						"{0}<xf:select1 bind=\"{1}\">\n", qPad, qId));
			else
				buf.append(MessageFormat.format(
						"{0}<xf:select1 ref=\"{1}\" type=\"{2}\">\n", qPad,
						qId, questionTypeToSchemaType(qType)));
			buf.append(MessageFormat.format("{0}\t<xf:label>{1}</xf:label>\n",
					qPad, qName));
			QuestionDef parentQuestion = dynOptDepMap.get(qIdNum);
			String parentId = getIdFromVarName(parentQuestion.getVariableName());
			String itemsetFormat = "{0}\t<xf:itemset nodeset=\"instance(''{2}'')/item[@parent=instance(''{1}'')/{3}]\"><xf:label ref=\"label\"/><xf:value ref=\"value\"/></xf:itemset>\n";
			String instanceId = qPath[1];
			String itemsetDef = MessageFormat.format(itemsetFormat, qPad,
					instanceId, qId, parentId);
			buf.append(itemsetDef);
			buf.append(MessageFormat.format("{0}</xf:select1>\n", qPad));
		} else if (questionTypeGeneratesBoundInput(qType)) {
			if (!qNested)
				buf.append(MessageFormat.format("{0}<xf:input bind=\"{1}\">\n",
						qPad, qId));
			else
				buf.append(MessageFormat.format(
						"{0}<xf:input ref=\"{1}\" type=\"{2}\">\n", qPad, qId,
						questionTypeToSchemaType(qType)));
			buf.append(MessageFormat.format("{0}\t<xf:label>{1}</xf:label>\n",
					qPad, qName));
			buf.append(MessageFormat.format("{0}</xf:input>\n", qPad));
		} else if (questionTypeGeneratesBoundUpload(qType)) {
			String mediaType = questionTypeToMediaType(qType);
			if (!qNested)
				buf.append(MessageFormat.format(
						"{0}<xf:upload bind=\"{1}\" mediatype=\"{2}\">\n",
						qPad, qId, mediaType));
			else
				buf.append(MessageFormat
						.format("{0}<xf:upload ref=\"{1}\" type=\"{2}\" mediatype=\"{3}\">\n",
								qPad, qId, questionTypeToSchemaType(qType),
								mediaType));
			buf.append(MessageFormat.format("{0}\t<xf:label>{1}</xf:label>\n",
					qPad, qName));
			buf.append(MessageFormat.format("{0}</xf:upload>\n", qPad));
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
					buf.append("\t\t");
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
					String instanceDef = MessageFormat.format(
							"\t\t<xf:instance id=''{0}''>\n", qId);
					buf.append(instanceDef);
					buf.append("\t\t\t<dynamiclist>\n");
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
							String itemPattern = "\t\t\t\t<item id=\"{0}\" parent=\"{1}\"><label>{2}</label><value>{0}</value></item>\n";
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

					buf.append("\t\t\t</dynamiclist>\n");
					buf.append("\t\t</xf:instance>\n");
				}
			}
		}
	}

	private static void generateMainInstance(QuestionTree rootTree,
			StringBuilder buf) {

		FormDef formDef = rootTree.getFormDef();

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
		if (!rootTree.isLeaf()) {
			for (QuestionTree childTree : rootTree.getChildren()) {
				generateInstanceElement(childTree, buf);
			}
		}
		buf.append(MessageFormat.format("\t\t\t</{0}>",
				formDef.getVariableName()));
		buf.append('\n');
		buf.append("\t\t</xf:instance>");
	}

	private static void generateInstanceElement(QuestionTree tree,
			StringBuilder buf) {

		FormDef form = tree.getFormDef();
		QuestionDef question = tree.getQuestion();
		String instanceBinding = "/" + form.getVariableName() + "/";
		String questionBinding = tree.getQuestion().getVariableName();
		StringBuilder pad = new StringBuilder("\t\t\t\t");
		for (int i = 1; i < tree.getDepth(); i++)
			pad.append('\t');

		if (questionBinding.startsWith(instanceBinding))
			questionBinding = questionBinding.substring(instanceBinding
					.length());

		String[] elements = questionBinding.split("/");
		for (int elem = 0; elem < elements.length - 1; elem++) {
			buf.append(pad);
			for (int depth = 0; depth < elem; depth++)
				buf.append('\t');
			buf.append(MessageFormat.format("<{0}>\n", elements[elem]));
		}

		String lastElement = elements[elements.length - 1];
		if (tree.isLeaf()) {
			buf.append(pad);
			String defaultValue = question.getDefaultValue();
			if (defaultValue != null && !"".equals(defaultValue))
				buf.append(MessageFormat.format("<{0}>{1}</{0}>\n",
						lastElement, defaultValue));
			else
				buf.append(MessageFormat.format("<{0}/>\n", lastElement));
		} else {
			buf.append(MessageFormat.format("{0}\t<{1}>\n", pad, lastElement));
			for (QuestionTree childTree : tree.getChildren())
				generateInstanceElement(childTree, buf);
			buf.append(MessageFormat.format("{0}\t</{1}>\n", pad, lastElement));
		}

		for (int elem = elements.length - 2; elem >= 0; elem--) {
			buf.append(pad);
			for (int depth = 0; depth < elem; depth++)
				buf.append('\t');
			String elementName = elements[elem];
			buf.append(MessageFormat.format("</{0}>\n", elementName));
		}
	}
}
