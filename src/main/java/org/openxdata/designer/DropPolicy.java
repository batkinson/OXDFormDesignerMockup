package org.openxdata.designer;

import org.apache.pivot.collections.List;
import org.apache.pivot.collections.Sequence;
import org.openxdata.designer.util.Page;
import org.openxdata.designer.util.Question;

public class DropPolicy {

	public static boolean allowDrop(Object source, Object target) {

		if (source == null || target == null)
			return false;

		if (source instanceof Question)
			return (target instanceof Page && ((Page) target)
					.indexOf((Question) source) == -1)
					|| target instanceof Question;

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
