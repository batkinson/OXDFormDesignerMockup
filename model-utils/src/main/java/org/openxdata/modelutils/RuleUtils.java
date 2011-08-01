package org.openxdata.modelutils;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import static org.apache.commons.lang.StringUtils.countMatches;
import static org.apache.commons.lang.StringUtils.repeat;
import org.fcitmuk.epihandy.Condition;
import org.fcitmuk.epihandy.EpihandyConstants;
import org.fcitmuk.epihandy.FormDef;
import org.fcitmuk.epihandy.QuestionDef;
import org.fcitmuk.epihandy.SkipRule;
import org.fcitmuk.epihandy.ValidationRule;

public class RuleUtils {

	private static Map<Byte, String> opMap = new HashMap<Byte, String>();
	private static Set<Byte> functionOps = new HashSet<Byte>();

	static {
		opMap.put(EpihandyConstants.OPERATOR_EQUAL, "=");
		opMap.put(EpihandyConstants.OPERATOR_NOT_EQUAL, "!=");
		opMap.put(EpihandyConstants.OPERATOR_LESS, "&lt;");
		opMap.put(EpihandyConstants.OPERATOR_LESS_EQUAL, "&lt;=");
		opMap.put(EpihandyConstants.OPERATOR_GREATER, "&gt;");
		opMap.put(EpihandyConstants.OPERATOR_GREATER_EQUAL, "&gt;=");
		opMap.put(EpihandyConstants.OPERATOR_STARTS_WITH, "starts-with");
		opMap.put(EpihandyConstants.OPERATOR_NOT_START_WITH, "not(starts-with");
		opMap.put(EpihandyConstants.OPERATOR_CONTAINS, "contains");
		opMap.put(EpihandyConstants.OPERATOR_NOT_CONTAIN, "not(contains");

		functionOps.add(EpihandyConstants.OPERATOR_STARTS_WITH);
		functionOps.add(EpihandyConstants.OPERATOR_NOT_START_WITH);
		functionOps.add(EpihandyConstants.OPERATOR_CONTAINS);
		functionOps.add(EpihandyConstants.OPERATOR_NOT_CONTAIN);
	}

	@SuppressWarnings("unchecked")
	public static Map<Short, Set<SkipRule>> getSkipRulesByTargetId(
			FormDef formDef) {
		Map<Short, Set<SkipRule>> skipRulesByTarget = new HashMap<Short, Set<SkipRule>>();
		for (SkipRule skipRule : (Vector<SkipRule>) formDef.getSkipRules()) {
			for (Short targetQuestionId : (Vector<Short>) skipRule
					.getActionTargets()) {
				Set<SkipRule> ruleSet = skipRulesByTarget.get(targetQuestionId);
				if (ruleSet == null) {
					ruleSet = new LinkedHashSet<SkipRule>();
					skipRulesByTarget.put(targetQuestionId, ruleSet);
				}
				ruleSet.add(skipRule);
			}
		}
		return skipRulesByTarget;
	}

	public static boolean questionGeneratesValidationRule(FormDef form,
			QuestionDef question) {
		return form.getValidationRule(question.getId()) != null;
	}

	public static String buildAction(byte action) {
		StringBuilder buf = new StringBuilder();

		if ((action & EpihandyConstants.ACTION_HIDE) != 0)
			buf.append("hide");
		else if ((action & EpihandyConstants.ACTION_SHOW) != 0)
			buf.append("show");
		else if ((action & EpihandyConstants.ACTION_DISABLE) != 0)
			buf.append("disable");
		else if ((action & EpihandyConstants.ACTION_ENABLE) != 0)
			buf.append("enable");

		if ((action & EpihandyConstants.ACTION_MAKE_MANDATORY) != 0)
			buf.append("|true()");

		return buf.toString();
	}

	@SuppressWarnings("unchecked")
	public static String buildSkipRuleLogic(FormDef form,
			Set<SkipRule> skipRules, QuestionDef target) {

		StringBuilder buf = new StringBuilder();
		for (SkipRule rule : skipRules) {
			String op = rule.getConditionsOperator() == EpihandyConstants.CONDITIONS_OPERATOR_AND ? "and"
					: "or";

			Vector<Condition> conditions = (Vector<Condition>) rule
					.getConditions();
			for (int i = 0; i < conditions.size(); i++) {

				Condition c = conditions.get(i);

				String qPath = null;
				if (target.getId() == c.getQuestionId())
					qPath = ".";
				else
					qPath = form.getQuestion(c.getQuestionId())
							.getVariableName();

				buf.append(qPath);
				buf.append(' ');
				buf.append(buildOpExpr(c.getOperator(), c.getValue(), true));

				if (i < conditions.size() - 1 && conditions.size() > 1) {
					buf.append(' ');
					buf.append(op);
					buf.append(' ');
				}
			}
		}
		return buf.toString();
	}

	@SuppressWarnings("unchecked")
	public static String buildConstraintFromRule(FormDef form,
			ValidationRule rule) {

		StringBuilder buf = new StringBuilder();
		String op = rule.getConditionsOperator() == EpihandyConstants.CONDITIONS_OPERATOR_AND ? "and"
				: "or";

		Vector<Condition> conditions = (Vector<Condition>) rule.getConditions();
		for (int i = 0; i < conditions.size(); i++) {

			Condition c = conditions.get(i);

			String qPath = null;
			if (rule.getQuestionId() == c.getQuestionId())
				qPath = ".";
			else
				qPath = form.getQuestion(c.getQuestionId()).getVariableName();

			if (c.getFunction() == EpihandyConstants.FUNCTION_LENGTH)
				buf.append(MessageFormat.format("length({0})", qPath));
			else
				buf.append(qPath);
			buf.append(' ');
			buf.append(buildOpExpr(c.getOperator(), c.getValue(), false));

			if (i < conditions.size() - 1 && conditions.size() > 1) {
				buf.append(' ');
				buf.append(op);
				buf.append(' ');
			}
		}
		return buf.toString();
	}

	public static String buildOpExpr(byte opType, String operand, boolean quote) {
		String opString = opMap.get(opType);
		String pattern;
		String closingParens = "";
		if (isOpTypeFunction(opType)) {
			pattern = "{0}({2}{1}{2}{3})";
			closingParens = repeat(")", countMatches(opString, "("));
		} else
			pattern = "{0} {2}{1}{2}";
		return MessageFormat.format(pattern, opString, operand, quote ? "'"
				: "", closingParens);
	}

	public static boolean isOpTypeFunction(byte opType) {
		return functionOps.contains(opType);
	}
}
