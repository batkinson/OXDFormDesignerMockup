importPackage(java.lang);
importPackage(org.apache.pivot.wtk);

var formTreeDropTarget = new DropTarget() {

	dragEnter: 
    function(component, dragContent, supportedDropActions, userDropAction) {
        var dropAction = null;
        if (dragContent.containsFileList()
            && DropAction.COPY.isSelected(supportedDropActions)) {
            dropAction = DropAction.COPY;
        }
        else if (dragContent.containsValue('node')) {
        	dropAction = DropAction.MOVE;
        }
        return dropAction;
    },

    dragExit:
    function(component) {
    },

    dragMove: 
    function(component, dragContent, supportedDropActions, x, y, userDropAction) {
    	if (dragContent.containsValue('node') && designTree.getNodeAt(y) != null) {
    		return DropAction.MOVE;
    	}
    	else if (dragContent.containsFileList())
    		return DropAction.COPY;
    	else
    		return null;
    },

    userDropActionChange: 
    function(component, dragContent, supportedDropActions, x, y, userDropAction) {
    	if (dragContent.containsValue('node') && designTree.getNodeAt(y) != null)
    		return DropAction.MOVE;
    	else if (dragContent.containsFileList())
    		return DropAction.COPY;
    	else
    		return null;
    },

    drop: 
    function(component, dragContent, supportedDropActions, x, y, userDropAction) {
    	// Oddly, in this method x,y are display relative
    	var display = component.getDisplay();
    	var dropLocation = component.mapPointFromAncestor(display, x, y);
        var dropAction = null;
        if (dragContent.containsFileList()) {
            return application.drop(dragContent);
        }
        else if (dragContent.containsValue('node') && designTree.getNodeAt(dropLocation.y) != null) {
        	dragContent.putValue('targetPath', designTree.getNodeAt(dropLocation.y));
        	return application.drop(dragContent);
        }
        this.dragExit(component);
        return dropAction;
    }
};

var designTreeDragSource = new DragSource() {
	
    beginDrag: function(component, x, y) {
    	return true;
    },
 
    endDrag: function(component, dropAction) {
    },
 
    getContent: function() {
        var content = new LocalManifest();
        content.putValue('node',designTree.getSelectedNode());
        content.putValue('path',designTree.getSelectedPath());
        return content;
    },
 
    getOffset: function() {
        return null; // Not used for native drags
    },
 
    getRepresentation: function() {
        return null; // Not used for native drags
    },
 
    getSupportedDropActions: function() {
        return DropAction.MOVE.getMask();
    },
 
    isNative: function() {
        return false;
    }
}
