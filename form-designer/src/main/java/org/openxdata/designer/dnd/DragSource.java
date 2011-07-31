package org.openxdata.designer.dnd;

import org.apache.pivot.wtk.Component;
import org.apache.pivot.wtk.DropAction;
import org.apache.pivot.wtk.LocalManifest;
import org.apache.pivot.wtk.Point;
import org.apache.pivot.wtk.TreeView;
import org.apache.pivot.wtk.Visual;
import org.openxdata.designer.util.Option;
import org.openxdata.designer.util.Page;
import org.openxdata.designer.util.Question;

public class DragSource implements org.apache.pivot.wtk.DragSource {

	private TreeView tree;

	@Override
	public boolean beginDrag(Component component, int x, int y) {
		if (!(component instanceof TreeView))
			return false;
		tree = (TreeView) component;
		Object obj = tree.getSelectedNode();
		boolean acceptDrag = obj instanceof Page || obj instanceof Question
				|| obj instanceof Option;
		return acceptDrag;
	}

	@Override
	public void endDrag(Component component, DropAction dropAction) {
		tree = null;
	}

	@Override
	public boolean isNative() {
		return false;
	}

	@Override
	public LocalManifest getContent() {
		LocalManifest content = new LocalManifest();
		content.putValue("node", tree.getSelectedNode());
		content.putValue("path", tree.getSelectedPath());
		return content;
	}

	@Override
	public Visual getRepresentation() {
		return null;
	}

	@Override
	public Point getOffset() {
		return null;
	}

	@Override
	public int getSupportedDropActions() {
		return DropAction.MOVE.getMask();
	}
}
