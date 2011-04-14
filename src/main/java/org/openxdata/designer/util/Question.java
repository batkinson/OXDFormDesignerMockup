package org.openxdata.designer.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

import org.apache.pivot.collections.ArrayList;
import org.apache.pivot.collections.List;
import org.apache.pivot.collections.ListListener;
import org.apache.pivot.collections.Sequence;
import org.apache.pivot.util.ListenerList;
import org.fcitmuk.epihandy.OptionDef;
import org.fcitmuk.epihandy.QuestionDef;
import org.fcitmuk.epihandy.RepeatQtnsDef;

public class Question extends org.fcitmuk.epihandy.QuestionDef implements
		List<PageElement>, PageElement {

	public Question() {
		setText("Unnamed Question");
	}

	public Question(QuestionDef questionDef) {
		super(questionDef);

		if (isQuestionList()) {
			RepeatQtnsDef repeatDef = getRepeatQtnsDef();
			if (repeatDef != null) {
				@SuppressWarnings("unchecked")
				Vector<Question> nestedQuestions = (Vector<Question>) repeatDef
						.getQuestions();
				for (int i = 0; i < nestedQuestions.size(); i++) {
					QuestionDef nestedQuestionDef = nestedQuestions.get(i);
					Question question = new Question(nestedQuestionDef);
					nestedQuestions.set(i, question);
				}
			}
		} else if (isStaticOptionList()) {
			@SuppressWarnings("unchecked")
			Vector<OptionDef> options = (Vector<OptionDef>) getOptions();
			for (int i = 0; i < options.size(); i++) {
				OptionDef optionDef = options.get(i);
				Option option = new Option(optionDef);
				options.set(i, option);
			}
		}
	}

	public boolean isQuestionList() {
		byte questionType = getType();
		return questionType == QTN_TYPE_REPEAT;
	}

	public boolean isStaticOptionList() {
		byte questionType = getType();
		return questionType == QTN_TYPE_LIST_EXCLUSIVE
				|| questionType == QTN_TYPE_LIST_MULTIPLE;
	}

	public boolean isDynamicOptionList() {
		byte questionType = getType();
		return questionType == QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC;
	}

	@SuppressWarnings("unchecked")
	Vector<PageElement> getElements() {
		Vector<PageElement> elements = new Vector<PageElement>();
		if (isQuestionList()) {
			RepeatQtnsDef repeat = getRepeatQtnsDef();
			elements = (Vector<PageElement>) repeat.getQuestions();
		} else if (isStaticOptionList()) {
			elements = (Vector<PageElement>) getOptions();
		}
		return elements;
	}

	public boolean acceptsPageElement(PageElement element) {
		if (isQuestionList() && element instanceof Question)
			return true;
		if (isStaticOptionList() && element instanceof Option)
			return true;
		return false;
	}

	public int remove(PageElement item) {

		if (!acceptsPageElement(item))
			return -1;

		Vector<PageElement> elements = getElements();

		if (elements == null)
			return -1;

		for (int i = 0; i < elements.size(); i++) {
			if (item.equals(elements.get(i))) {
				PageElement removedElement = elements.remove(i);
				listenerList.itemsRemoved(this, i, new ArrayList<PageElement>(
						removedElement));
				return i;
			}
		}

		return -1;
	}

	public PageElement get(int index) {
		Vector<PageElement> pageElements = (Vector<PageElement>) getElements();
		return (PageElement) pageElements.get(index);
	}

	public int indexOf(PageElement item) {
		Vector<PageElement> pages = (Vector<PageElement>) getElements();
		return pages.indexOf(item);
	}

	public boolean isEmpty() {
		return getElements().isEmpty();
	}

	class PageElementComparator implements Comparator<PageElement> {

		public int compare(PageElement o1, PageElement o2) {
			if (o1 == o2 && o1 == null)
				return 0;

			if (o1 == null)
				return -1;

			if (o2 == null)
				return 1;

			return o1.toString().compareTo(o2.toString());
		}

	}

	private Comparator<PageElement> comparator = new PageElementComparator();

	public Comparator<PageElement> getComparator() {
		return comparator;
	}

	public Iterator<PageElement> iterator() {
		Vector<PageElement> elements = (Vector<PageElement>) getElements();
		return elements.iterator();
	}

	public int add(PageElement item) {
		Vector<PageElement> elements = (Vector<PageElement>) getElements();
		int index = -1;
		synchronized (elements) {
			index = elements.size();
			elements.add(item);
			listenerList.itemInserted(this, index);
			return index;
		}
	}

	public void insert(PageElement item, int index) {
		Vector<PageElement> elements = (Vector<PageElement>) getElements();
		elements.insertElementAt(item, index);
		listenerList.itemInserted(this, index);
	}

	public PageElement update(int index, PageElement item) {
		Vector<PageElement> elements = (Vector<PageElement>) getElements();
		PageElement exiled = null;
		synchronized (elements) {
			exiled = elements.get(index);
			elements.setElementAt(item, index);
		}
		listenerList.itemUpdated(this, index, exiled);
		return exiled;
	}

	public Sequence<PageElement> remove(int index, int count) {
		Vector<PageElement> elements = (Vector<PageElement>) getElements();
		Sequence<PageElement> removedPageElements = new ArrayList<PageElement>();
		for (int i = 0; i < count && index + i < elements.size(); i++) {
			PageElement removedPageElement = elements.remove(index);
			removedPageElements.add(removedPageElement);
		}
		listenerList.itemsRemoved(this, index, removedPageElements);
		return removedPageElements;
	}

	public void clear() {
		Vector<PageElement> elements = (Vector<PageElement>) getElements();
		elements.clear();
	}

	public int getLength() {
		Vector<PageElement> elements = (Vector<PageElement>) getElements();
		return elements.size();
	}

	public void setComparator(Comparator<PageElement> comparator) {
		this.comparator = comparator;
	}

	private ListListenerList<PageElement> listenerList = new ListListenerList<PageElement>();

	public ListenerList<ListListener<PageElement>> getListListeners() {
		return listenerList;
	}
}
