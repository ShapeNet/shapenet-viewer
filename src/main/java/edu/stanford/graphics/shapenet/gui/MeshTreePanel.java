package edu.stanford.graphics.shapenet.gui;

import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import edu.stanford.graphics.shapenet.jme3.viewer.Viewer;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Shows meshes for a node
 *
 * @author Angel Chang
 */
public class MeshTreePanel extends TreePanel {

  public MeshTreePanel(DefaultMutableTreeNode top) {
    super(top);
  }

  public static void create(DefaultMutableTreeNode top) {
    TreePanel.create("MeshTree", new MeshTreePanel(top));
  }

  /** Required by TreeSelectionListener interface. */
  public void valueChanged(TreeSelectionEvent e) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
        tree.getLastSelectedPathComponent();

    if (node == null) return;

    Object nodeInfo = node.getUserObject();
    TreeNodeInfo<Spatial> treeNodeInfo = (TreeNodeInfo)nodeInfo;
    Spatial spatial = treeNodeInfo.value;
    // TODO: Highlight spatial
    // TODO: Display some info in the html pane
    if (node.isLeaf()) {
    } else {
    }

    String endl = "\n";
    StringBuilder sb = new StringBuilder();
    String name = spatial.getName();
    if (spatial instanceof Geometry) {
      Geometry geom = (Geometry) spatial;
      if (name == null) {
        name = "Mesh " + geom.getMesh().getId();
      }
      sb.append(name + endl);
      sb.append(" mesh: " + geom.getMesh().getId() + endl);
      sb.append(" nTriangles: " + geom.getMesh().getTriangleCount() + endl);
      sb.append(" material: " + geom.getMaterial().getName() + endl);
      sb.append(" worldMatrix: " + geom.getWorldMatrix() + endl);
    } else {
      sb.append("Node " + name + endl);
    }
    sb.append(" transform: " + spatial.getLocalTransform() + endl);
    sb.append(" bb: " + spatial.getWorldBound() + endl);
    sb.append(endl);
    String extraInfo = treeNodeInfo.get("info");
    if (extraInfo != null) {
      sb.append(extraInfo);
    }
    String content = sb.toString();
    htmlPane.setText(content);

    // Highlight modelInstance
    Viewer viewer = treeNodeInfo.get("viewer");
    if (viewer != null) {
      // TODO: Enqueue
      viewer.enqueue(() -> {
        viewer.debugVisualizer().showSpatial("TreeNodeSpatial", spatial);
        return 0;
      });
    }

    if (DEBUG) {
      System.out.println(nodeInfo.toString());
    }
  }
}