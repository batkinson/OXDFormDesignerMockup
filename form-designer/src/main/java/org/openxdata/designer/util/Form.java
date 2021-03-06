package org.openxdata.designer.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.pivot.collections.ArrayList;
import org.apache.pivot.collections.List;
import org.apache.pivot.collections.ListListener;
import org.apache.pivot.collections.Sequence;
import org.apache.pivot.util.ListenerList;
import org.fcitmuk.epihandy.DynamicOptionDef;
import org.fcitmuk.epihandy.FormDef;
import org.fcitmuk.epihandy.OptionDef;
import org.fcitmuk.epihandy.PageDef;
import org.openxdata.designer.idgen.DefaultIdGenerator;
import org.openxdata.designer.idgen.ScarceIdGenerator;
import org.openxdata.modelutils.FormUtils;
import org.openxdata.modelutils.RuleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author brent
 * 
 */
public class Form extends org.fcitmuk.epihandy.FormDef implements List<Page> {

	private Logger log = LoggerFactory.getLogger(Form.class);

	private ScarceIdGenerator idGen = new DefaultIdGenerator(1, Short.MAX_VALUE);

	// Question are assumed unique to the form
	private ScarceIdGenerator questionIdGen = new DefaultIdGenerator(1,
			Short.MAX_VALUE);

	@SuppressWarnings("unchecked")
	public Form(FormDef formDef) {

		super(formDef);

		// Patch up pages into alternative model representation
		Vector<PageDef> pages = (Vector<PageDef>) getPages();
		if (pages != null) {
			if (log.isDebugEnabled())
				log.debug("constructing out of " + pages.size() + " pages");
			for (int i = 0; i < pages.size(); i++) {
				PageDef pageDef = pages.elementAt(i);
				Page page = new Page(questionIdGen, pageDef);
				pages.setElementAt(page, i);
				idGen.reserveId(page.getPageNo());
			}
		}

		Hashtable<Short, DynamicOptionDef> dynOptionMap = (Hashtable<Short, DynamicOptionDef>) getDynamicOptions();
		if (dynOptionMap != null) {
			if (log.isDebugEnabled())
				log.debug("patching up " + dynOptionMap.size()
						+ " dynamic options");
			for (Map.Entry<Short, DynamicOptionDef> entry : dynOptionMap
					.entrySet()) {
				DynamicOptionDef dOptDef = entry.getValue();
				Hashtable<Short, Vector<OptionDef>> optionMap = dOptDef
						.getParentToChildOptions();

				Short childId = dOptDef.getQuestionId();
				Short parentId = entry.getKey();

				if (log.isDebugEnabled())
					log.debug("question " + childId + " depends on " + parentId);

				for (Map.Entry<Short, Vector<OptionDef>> optionEntry : optionMap
						.entrySet()) {
					Vector<OptionDef> options = optionEntry.getValue();
					Short parentOptId = optionEntry.getKey();
					for (int i = 0; i < options.size(); i++)
						options.set(i, new Option(options.get(i)));
					if (log.isDebugEnabled())
						log.debug("option " + parentOptId + " yields "
								+ options);
				}
			}
		}

		Map<Short, Short> renamedIdMap = new HashMap<Short, Short>();
		for (Page p : this) {
			renamedIdMap.putAll(p.getModifiedQuestionIds());
		}
		if (log.isDebugEnabled())
			log.debug("qids changed (old=new): " + renamedIdMap);

		FormUtils.changeQuestionIds(this, renamedIdMap);

		// Consolidate skip rules into optimal set using multi-targets
		RuleUtils.consolidateSkipRules(this);
	}

	@Override
	public void setVariableName(String variableName) {
		String oldName = getVariableName();
		super.setVariableName(variableName);
		FormUtils.updateFormVarName(this, oldName, variableName);
	}

	@Override
	public void addPage() {
		@SuppressWarnings("unchecked")
		Vector<PageDef> pages = (Vector<PageDef>) getPages();
		synchronized (pages) {
			short pageId = (short) idGen.nextId();
			Page newPage = new Page("Page" + pageId, pageId,
					new Vector<Question>());
			newPage.setQuestionIdGen(questionIdGen);
			add(newPage); // Method sends notifications
		}
	}

	public int remove(Page item) {

		@SuppressWarnings("unchecked")
		Vector<Page> pages = (Vector<Page>) getPages();
		for (int i = 0; i < pages.size(); i++) {
			if (item.equals(pages.get(i))) {
				Page removedPage = pages.remove(i);
				idGen.makeIdAvailable(removedPage.getPageNo());
				listenerList.itemsRemoved(this, i, new ArrayList<Page>(
						removedPage));
				return i;
			}
		}
		return -1;
	}

	public Page get(int index) {
		@SuppressWarnings("unchecked")
		Vector<Page> pages = (Vector<Page>) getPages();
		return (Page) pages.get(index);
	}

	public int indexOf(Page item) {
		@SuppressWarnings("unchecked")
		Vector<Page> pages = (Vector<Page>) getPages();
		return pages.indexOf(item);
	}

	public boolean isEmpty() {
		return getPages().isEmpty();
	}

	class PageComparator implements Comparator<Page> {

		public int compare(Page o1, Page o2) {
			if (o1 == o2 && o1 == null)
				return 0;

			if (o1 == null)
				return -1;

			if (o2 == null)
				return 1;

			return o1.toString().compareTo(o2.toString());
		}

	}

	private Comparator<Page> comparator = new PageComparator();

	public Comparator<Page> getComparator() {
		return comparator;
	}

	public Iterator<Page> iterator() {
		@SuppressWarnings("unchecked")
		Vector<Page> pages = (Vector<Page>) getPages();
		return pages.iterator();
	}

	public int add(Page item) {
		@SuppressWarnings("unchecked")
		Vector<Page> pages = (Vector<Page>) getPages();
		int index = -1;
		synchronized (pages) {
			index = pages.size();
			pages.add(item);
			idGen.reserveId(item.getPageNo());
			listenerList.itemInserted(this, index);
			return index;
		}
	}

	public void insert(Page item, int index) {
		@SuppressWarnings("unchecked")
		Vector<Page> pages = (Vector<Page>) getPages();
		pages.insertElementAt(item, index);
		idGen.reserveId(item.getPageNo());
		listenerList.itemInserted(this, index);
	}

	public Page update(int index, Page item) {
		@SuppressWarnings("unchecked")
		Vector<Page> pages = (Vector<Page>) getPages();
		Page exiled = null;
		synchronized (pages) {
			exiled = pages.get(index);
			idGen.makeIdAvailable(exiled.getPageNo());
			pages.setElementAt(item, index);
			idGen.reserveId(item.getPageNo());
		}
		listenerList.itemUpdated(this, index, exiled);
		return exiled;
	}

	public Sequence<Page> remove(int index, int count) {
		@SuppressWarnings("unchecked")
		Vector<Page> pages = (Vector<Page>) getPages();
		Sequence<Page> removedPages = new ArrayList<Page>();
		for (int i = 0; i < count && index + i < pages.size(); i++) {
			Page removedPage = pages.remove(index);
			removedPages.add(removedPage);
			idGen.makeIdAvailable(removedPage.getPageNo());
		}
		listenerList.itemsRemoved(this, index, removedPages);
		return removedPages;
	}

	public void clear() {
		@SuppressWarnings("unchecked")
		Vector<Page> pages = (Vector<Page>) getPages();
		for (Page page : this) {
			idGen.makeIdAvailable(page.getPageNo());
		}
		pages.clear();
	}

	public int getLength() {
		@SuppressWarnings("unchecked")
		Vector<Page> pages = (Vector<Page>) getPages();
		return pages.size();
	}

	public void setComparator(Comparator<Page> comparator) {
		this.comparator = comparator;
	}

	private ListListenerList<Page> listenerList = new ListListenerList<Page>();

	public ListenerList<ListListener<Page>> getListListeners() {
		return listenerList;
	}
}
