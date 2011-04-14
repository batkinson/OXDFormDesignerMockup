package org.openxdata.designer;

import org.apache.pivot.collections.List;
import org.apache.pivot.collections.Sequence;
import org.openxdata.designer.util.Option;
import org.openxdata.designer.util.Page;
import org.openxdata.designer.util.Question;

public class DropPolicy {

	public static boolean allowDrop(Object source, Object target) {

		if (source == null || target == null)
			return false;

		if (source == target)
			return false;

		if (source instanceof Question) {
			if (target instanceof Page) {
				Page targetPage = (Page) target;
				return targetPage.indexOf((Question) source) == -1;
			} else if (target instanceof Question) {
				Question targetQuestion = (Question) target;
				return targetQuestion.indexOf((Question) source) == -1;
			}
		} else if (source instanceof Option) {
			if (target instanceof Question) {
				Question targetQuestion = (Question) target;
				return targetQuestion.isStaticOptionList()
						&& targetQuestion.indexOf((Option) source) == -1;
			} else if (target instanceof Option) {
				return true;
			}
		}

		return false;
	}

	public static boolean allowDrop(List<Object> treeData,
			Sequence.Tree.Path sourcePath, Sequence.Tree.Path targetPath) {

		if (sourcePath == null || targetPath == null)
			return false;

		Object sourceObject = Sequence.Tree.get(treeData, sourcePath);
		Object targetObject = Sequence.Tree.get(treeData, targetPath);
		return allowDrop(sourceObject, targetObject);
	}
}
