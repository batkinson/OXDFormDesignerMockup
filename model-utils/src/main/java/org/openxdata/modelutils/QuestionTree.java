package org.openxdata.modelutils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.fcitmuk.epihandy.FormDef;
import org.fcitmuk.epihandy.PageDef;
import org.fcitmuk.epihandy.QuestionDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class QuestionTree {

	private static Logger log = LoggerFactory.getLogger(QuestionTree.class);

	private int depth;
	private FormDef form;
	private QuestionDef question;
	private QuestionTree parent;
	private List<QuestionTree> children;

	QuestionTree(FormDef form) {
		this(null, null);
		this.form = form;
	}

	QuestionTree(QuestionTree parent, QuestionDef question) {
		if (parent == this)
			throw new IllegalArgumentException(
					"question can not be parent of itself");
		if (parent != null && question == null)
			throw new IllegalArgumentException(
					"non-root node must specify question");
		this.parent = parent;
		this.question = question;

		if (parent != null) {
			this.form = parent.form; // copy formDef from parent
			depth = parent.getDepth() + 1;
			if (parent.isLeaf()) {
				parent.children = new ArrayList<QuestionTree>();
			}
			parent.children.add(this); // getter is immutable
		}
	}

	public boolean isRoot() {
		return parent == null && question == null;
	}

	public boolean isLeaf() {
		List<QuestionTree> children = getChildren();
		return children == null || children.size() <= 0;
	}

	public int getDepth() {
		return depth;
	}

	public QuestionDef getQuestion() {
		if (question == null)
			throw new UnsupportedOperationException(
					"root tree node doesn't have question");
		return question;
	}

	public QuestionTree getTreeForQuestion(QuestionDef question) {
		QuestionTree result = null;
		if (!isRoot() && getQuestion().equals(question)) {
			result = this;
		} else if (!isLeaf()) {
			for (QuestionTree childTree : getChildren()) {
				QuestionTree childResult = childTree
						.getTreeForQuestion(question);
				if (childResult != null) {
					result = childResult;
					break;
				}
			}
		}
		return result;
	}

	public FormDef getFormDef() {
		return form;
	}

	public List<QuestionTree> getChildren() {
		return this.children == null ? null : Collections
				.unmodifiableList(this.children);
	}

	public QuestionTree getParent() {
		return this.parent;
	}

	public String toString() {
		StringBuilder buf = new StringBuilder();

		for (int i = 0; i < getDepth(); i++)
			buf.append("   ");

		if (isRoot())
			buf.append("form\n");
		else
			buf.append(MessageFormat.format(
					"question id: {0}, text: ''{1}'', binding: ''{2}''\n",
					question.getId(), question.getText(),
					question.getVariableName()));

		if (!isLeaf())
			for (QuestionTree child : getChildren())
				buf.append(child.toString());

		return buf.toString();
	}

	@SuppressWarnings("unchecked")
	public static QuestionTree constructTreeFromFormDef(FormDef formDef) {
		QuestionTree result = new QuestionTree(formDef);
		Vector<PageDef> pages = (Vector<PageDef>) formDef.getPages();
		for (PageDef p : pages) {
			for (QuestionDef q : (Vector<QuestionDef>) p.getQuestions()) {
				constructTreeFromQuestion(result, q);
			}
		}

		if (log.isDebugEnabled())
			log.debug("parsed question tree: \n" + result);

		return result;
	}

	@SuppressWarnings("unchecked")
	private static void constructTreeFromQuestion(QuestionTree parent,
			QuestionDef question) {

		if (parent == null)
			throw new IllegalArgumentException("parent can not be null");
		if (question == null)
			throw new IllegalArgumentException("question can not be null");

		QuestionTree questionTree = new QuestionTree(parent, question);

		if (question.getType() == QuestionDef.QTN_TYPE_REPEAT) {
			for (QuestionDef nestedQuestion : (Vector<QuestionDef>) question
					.getRepeatQtnsDef().getQuestions()) {
				constructTreeFromQuestion(questionTree, nestedQuestion);
			}
		}
	}
}
