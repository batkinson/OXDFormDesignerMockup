importPackage(java.lang);
importPackage(org.apache.pivot.wtk);
importPackage(org.openxdata.designer);
importPackage(org.openxdata.designer.util);

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
    function(component) { },

    dragMove: 
    function(component, dragContent, supportedDropActions, x, y, userDropAction) {
    
        if (dragContent.containsValue('node')) {
            var treeData = designTree.getTreeData();
            var sourcePath = dragContent.getValue('path');
            var targetPath = designTree.getNodeAt(y);
            if (DropPolicy.allowDrop(treeData, sourcePath, targetPath))
                return DropAction.MOVE;
            }
        else if (dragContent.containsFileList())
            return DropAction.COPY;
    
        return null;
    },

    userDropActionChange: 
    function(component, dragContent, supportedDropActions, x, y, userDropAction) {
        if (dragContent.containsValue('node')) {
            var treeData = designTree.getTreeData();
            var sourcePath = dragContent.getValue('path');
            var targetPath = designTree.getNodeAt(y);
            if (DropPolicy.allowDrop(treeData, sourcePath, targetPath))
                return DropAction.MOVE;
        }
        else if (dragContent.containsFileList())
            return DropAction.COPY;

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
        else if (dragContent.containsValue('node')) {
            var treeData = designTree.getTreeData();
            var sourcePath = dragContent.getValue('path');
            var targetPath = designTree.getNodeAt(dropLocation.y);
            if (DropPolicy.allowDrop(treeData, sourcePath, targetPath)){
                dragContent.putValue('targetPath', targetPath);
                return application.drop(dragContent);
            }
        }

        this.dragExit(component);
        return dropAction;
    }
};

var designTreeDragSource = new DragSource() {

    beginDrag:
    function(component, x, y) {
        var obj = designTree.getSelectedNode();
        return obj instanceof Page || obj instanceof Question;
    },

    endDrag:
    function(component, dropAction) { },

    getContent: 
    function() {
        var content = new LocalManifest();
        content.putValue('node',designTree.getSelectedNode());
        content.putValue('path',designTree.getSelectedPath());
        return content;
    },

    getOffset:
    function() {
        return null; // Not used for native drags
    },

    getRepresentation: 
    function() {
        return null; // Not used for native drags
    },
 
    getSupportedDropActions:
    function() {
        return DropAction.MOVE.getMask();
    },
 
    isNative: 
    function() {
        return false;
    }
}
