package edu.stanford.graphics.shapenet.gui;

import edu.stanford.graphics.shapenet.common.ModelInstance;
import edu.stanford.graphics.shapenet.jme3.viewer.Viewer;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Shows meshes for a scene
 *
 * @author Angel Chang
 */
public class SceneTreePanel extends TreePanel {

  public SceneTreePanel(DefaultMutableTreeNode top) {
    super(top);
  }

  public static void create(DefaultMutableTreeNode top) {
    TreePanel.create("SceneHierarchy", new SceneTreePanel(top));
  }

  /** Required by TreeSelectionListener interface. */
  public void valueChanged(TreeSelectionEvent e) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
        tree.getLastSelectedPathComponent();

    if (node == null) return;

    Object nodeInfo = node.getUserObject();
    TreeNodeInfo<ModelInstance> treeNodeInfo = (TreeNodeInfo)nodeInfo;
    ModelInstance modelInstance = treeNodeInfo.value;
    if (node.isLeaf()) {
    } else {
    }
    if (modelInstance != null) {
      // Display some info in the html pane
      String content = modelInstance.model().modelInfo().toDetailedString();
      htmlPane.setText(content);
      // Highlight modelInstance
      Viewer viewer = treeNodeInfo.get("viewer");
      if (viewer != null) {
        // TODO: Enqueue
        viewer.enqueue(() -> {
          viewer.setSelected(modelInstance.index());
          return 0;
        });
      }
    }
    if (DEBUG) {
      System.out.println(nodeInfo.toString());
    }
  }
}