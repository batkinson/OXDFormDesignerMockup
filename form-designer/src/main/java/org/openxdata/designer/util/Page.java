package org.openxdata.designer.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.pivot.collections.ArrayList;
import org.apache.pivot.collections.List;
import org.apache.pivot.collections.ListListener;
import org.apache.pivot.collections.Sequence;
import org.apache.pivot.util.ListenerList;
import org.fcitmuk.epihandy.PageDef;
import org.fcitmuk.epihandy.QuestionDef;
import org.openxdata.designer.idgen.ScarceIdGenerator;

public class Page extends org.fcitmuk.epihandy.PageDef implements
		List<Question> {

	private ScarceIdGenerator questionIdGen;

	private Map<Short, Short> modifiedQuestionIds = new HashMap<Short, Short>();

	public Page() {
		this("Unnamed Question", (short) -1, new Vector<Question>());
	}

	protected Page(String name, short pageNo, Vector<?> questions) {
		super(name, pageNo, questions);
	}

	public Page(ScarceIdGenerator idGen, PageDef pageDef) {

		super(pageDef);

		this.questionIdGen = idGen;

		@SuppressWarnings("unchecked")
		Vector<QuestionDef> questions = (Vector<QuestionDef>) getQuestions();
		for (int i = 0; i < questions.size(); i++) {
			QuestionDef questionDef = questions.elementAt(i);
			Question question = new Question(questionIdGen, questionDef);

			short origQuestionId = questionDef.getId();
			short questionId = (short) questionIdGen.nextId();

			if (origQuestionId != questionId)
				modifiedQuestionIds.put(origQuestionId, questionId);

			question.setId(questionId);
			questions.setElementAt(question, i);
			idGen.reserveId(question.getId());
		}
	}

	void setQuestionIdGen(ScarceIdGenerator idGen) {
		this.questionIdGen = idGen;
	}

	public void newQuestion() {
		Question question = new Question();
		short nextId = (short) questionIdGen.nextId();
		question.setId(nextId);
		add(question);
	}

	public int remove(Question item) {
		@SuppressWarnings("unchecked")
		Vector<Question> questions = (Vector<Question>) getQuestions();
		for (int i = 0; i < questions.size(); i++) {
			if (item.equals(questions.get(i))) {
				Question removedQuestion = questions.remove(i);
				questionIdGen.makeIdAvailable(removedQuestion.getId());
				listenerList.itemsRemoved(this, i, new ArrayList<Question>(
						removedQuestion));
				return i;
			}
		}
		return -1;
	}

	public Question get(int index) {
		@SuppressWarnings("unchecked")
		Vector<Question> questions = (Vector<Question>) getQuestions();
		return (Question) questions.get(index);
	}

	public int indexOf(Question item) {
		@SuppressWarnings("unchecked")
		Vector<Question> questions = (Vector<Question>) getQuestions();
		return questions.indexOf(item);
	}

	public boolean isEmpty() {
		return getQuestions().isEmpty();
	}

	class QuestionComparator implements Comparator<Question> {

		public int compare(Question o1, Question o2) {
			if (o1 == o2 && o1 == null)
				return 0;

			if (o1 == null)
				return -1;

			if (o2 == null)
				return 1;

			return o1.toString().compareTo(o2.toString());
		}

	}

	private Comparator<Question> comparator = new QuestionComparator();

	public Comparator<Question> getComparator() {
		return comparator;
	}

	public Iterator<Question> iterator() {
		@SuppressWarnings("unchecked")
		Vector<Question> questions = (Vector<Question>) getQuestions();
		return questions.iterator();
	}

	public int add(Question item) {
		@SuppressWarnings("unchecked")
		Vector<Question> questions = (Vector<Question>) getQuestions();
		int index = -1;
		synchronized (questions) {
			index = questions.size();
			questions.add(item);
			questionIdGen.reserveId(item.getId());
			listenerList.itemInserted(this, index);
			return index;
		}
	}

	public void insert(Question item, int index) {
		@SuppressWarnings("unchecked")
		Vector<Question> questions = (Vector<Question>) getQuestions();
		questions.insertElementAt(item, index);
		questionIdGen.reserveId(item.getId());
		listenerList.itemInserted(this, index);
	}

	public Question update(int index, Question item) {
		@SuppressWarnings("unchecked")
		Vector<Question> questions = (Vector<Question>) getQuestions();
		Question exiled = null;
		synchronized (questions) {
			exiled = questions.get(index);
			questionIdGen.makeIdAvailable(exiled.getId());
			questions.setElementAt(item, index);
			questionIdGen.reserveId(item.getId());
			listenerList.itemUpdated(this, index, item);
		}
		return exiled;
	}

	public Sequence<Question> remove(int index, int count) {
		@SuppressWarnings("unchecked")
		Vector<Question> questions = (Vector<Question>) getQuestions();
		Sequence<Question> removedQuestions = new ArrayList<Question>();
		for (int i = 0; i < count && index + i < questions.size(); i++) {
			Question removedQuestion = questions.remove(index);
			questionIdGen.makeIdAvailable(removedQuestion.getId());
			removedQuestions.add(removedQuestion);
		}
		listenerList.itemsRemoved(this, index, removedQuestions);
		return removedQuestions;
	}

	public void clear() {
		@SuppressWarnings("unchecked")
		Vector<Question> questions = (Vector<Question>) getQuestions();
		for (Question question : this) {
			questionIdGen.makeIdAvailable(question.getId());
		}
		questions.clear();
	}

	public int getLength() {
		@SuppressWarnings("unchecked")
		Vector<Question> questions = (Vector<Question>) getQuestions();
		return questions.size();
	}

	public void setComparator(Comparator<Question> comparator) {
		this.comparator = comparator;
	}

	private ListListenerList<Question> listenerList = new ListListenerList<Question>();

	public ListenerList<ListListener<Question>> getListListeners() {
		return listenerList;
	}

	public Map<Short, Short> getModifiedQuestionIds() {
		return modifiedQuestionIds;
	}
}
